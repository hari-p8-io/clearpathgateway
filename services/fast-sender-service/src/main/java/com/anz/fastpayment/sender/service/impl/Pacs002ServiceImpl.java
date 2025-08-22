package com.anz.fastpayment.sender.service.impl;

import com.anz.fastpayment.sender.model.Pacs002Request;
import com.anz.fastpayment.sender.model.Pacs002Response;
import com.anz.fastpayment.sender.service.Pacs002Service;
import com.anz.fastpayment.sender.model.Pacs002Entity;
import com.anz.fastpayment.sender.repository.Pacs002Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import com.anz.fastpayment.sender.service.EventJsonPublisher;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class Pacs002ServiceImpl implements Pacs002Service {

    private static final Logger log = LoggerFactory.getLogger(Pacs002ServiceImpl.class);

    private final Pacs002Repository pacs002Repository;
    private final JmsTemplate jmsTemplate;
    private final EventJsonPublisher eventJsonPublisher;

    @Value("${app.activemq.pacs002-queue:pacs002.outbound}")
    private String pacs002OutboundQueue;

    @Value("${app.sender.max-retry-attempts:5}")
    private int maxRetryAttempts;

    @Value("${app.sender.retry-backoff-ms:1000}")
    private long retryBackoffMs;

    public Pacs002ServiceImpl(Pacs002Repository pacs002Repository, JmsTemplate jmsTemplate, EventJsonPublisher eventJsonPublisher) {
        this.pacs002Repository = pacs002Repository;
        this.jmsTemplate = jmsTemplate;
        this.eventJsonPublisher = eventJsonPublisher;
    }

    @Override
    public Pacs002Response handlePacs002Request(Pacs002Request request) {
        log.info("[PACS002] Received request for PUID={}, messageType={}, uniqueId={}, error={}",
                safe(request.getPuid()), request.getMessageType(), safe(request.getUniqueId()), safe(request.getError()));

        // Idempotency: if record already exists, skip re-send
        try {
            if (pacs002Repository.existsById(request.getPuid())) {
                log.info("[IDEMPOTENT] pacs.002 already processed for PUID={}, skipping re-send", safe(request.getPuid()));
                return new Pacs002Response(request.getPuid(), "ACCEPTED");
            }
        } catch (Exception e) {
            log.warn("[IDEMPOTENT] Failed to check existence for PUID={}, err={}", safe(request.getPuid()), e.getMessage());
        }

        String xml = buildPacs002RejectXml(request);
        log.debug("[PACS002] Built XML (first 500) => {}", preview(xml, 500));

        // Persist to Spanner
        Pacs002Entity entity = new Pacs002Entity();
        entity.setPuid(request.getPuid());
        entity.setUniqueId(request.getUniqueId());
        entity.setCreatedAt(java.time.Instant.now());
        entity.setXml(xml);
        entity.setEventJson(buildEventJson(request));
        try {
            pacs002Repository.save(entity);
        } catch (Exception e) {
            log.warn("[PERSIST] Failed to save pacs.002 entity for PUID={}, err={}", safe(request.getPuid()), e.getMessage());
        }

        // Publish to ActiveMQ
        boolean sent = false;
        for (int attempt = 1; attempt <= Math.max(1, maxRetryAttempts); attempt++) {
            try {
                jmsTemplate.convertAndSend(pacs002OutboundQueue, xml);
                log.info("[AMQ] Sent pacs.002 for PUID={} to queue {} (attempt {} of {})", safe(request.getPuid()), pacs002OutboundQueue, attempt, maxRetryAttempts);
                sent = true;
                break;
            } catch (Exception e) {
                log.warn("[AMQ] Send failed for PUID={}, attempt {}/{} err={}", safe(request.getPuid()), attempt, maxRetryAttempts, e.getMessage());
                if (attempt < maxRetryAttempts) {
                    try { Thread.sleep(Math.max(0, retryBackoffMs)); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }
        if (!sent) {
            log.warn("[AMQ] Exhausted retries; pacs.002 not sent for PUID={}", safe(request.getPuid()));
        }

        // Publish event JSON to Kafka
        try {
            eventJsonPublisher.publish(request.getPuid(), entity.getEventJson());
        } catch (Exception e) {
            log.warn("[KAFKA] Failed publishing event JSON for PUID={}, err={}", safe(request.getPuid()), e.getMessage());
        }

        return new Pacs002Response(request.getPuid(), "ACCEPTED");
    }

    private String buildPacs002RejectXml(Pacs002Request req) {
        String now = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String msgId = "P002-" + req.getPuid();
        String orgMsgId = extractOrMsgIdFallback(req);
        String orgEndToEnd = req.getUniqueId() != null ? escape(req.getUniqueId()) : "";
        String reason = req.getError() != null ? escape(req.getError()) : "Validation failure";
        return "" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.002.001.15\">" +
                "<FIToFIPmtStsRpt>" +
                "  <GrpHdr>" +
                "    <MsgId>" + msgId + "</MsgId>" +
                "    <CreDtTm>" + now + "</CreDtTm>" +
                "  </GrpHdr>" +
                "  <OrgnlGrpInfAndSts>" +
                (orgMsgId.isEmpty() ? "" : ("    <OrgnlMsgId>" + orgMsgId + "</OrgnlMsgId>")) +
                "    <GrpSts>RJCT</GrpSts>" +
                "    <StsRsnInf><Rsn><Prtry>VALD</Prtry></Rsn><AddtlInf>" + reason + "</AddtlInf></StsRsnInf>" +
                "  </OrgnlGrpInfAndSts>" +
                "  <TxInfAndSts>" +
                (orgEndToEnd.isEmpty() ? "" : ("    <OrgnlEndToEndId>" + orgEndToEnd + "</OrgnlEndToEndId>")) +
                "    <TxSts>RJCT</TxSts>" +
                "    <StsRsnInf><Rsn><Prtry>VALD</Prtry></Rsn><AddtlInf>" + reason + "</AddtlInf></StsRsnInf>" +
                "  </TxInfAndSts>" +
                "</FIToFIPmtStsRpt>" +
                "</Document>";
    }

    private String extractOrMsgIdFallback(Pacs002Request req) {
        // Best-effort: if message id can be derived from original XML, do it later; for now, use PUID
        return req.getPuid();
    }

    private String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private String buildEventJson(Pacs002Request req) {
        String now = java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now());
        String statusId = "SR-" + req.getPuid();
        String reason = req.getError() == null ? "Validation failure" : req.getError();
        return "{" +
                "\"messageType\":\"PACS_002\"," +
                "\"messageId\":\"" + escape(statusId) + "\"," +
                "\"creationDateTime\":\"" + now + "\"," +
                "\"statusReports\":[{" +
                    "\"statusId\":\"" + escape(statusId) + "\"," +
                    "\"originalEndToEndId\":\"" + (req.getUniqueId() == null ? "" : escape(req.getUniqueId())) + "\"," +
                    "\"transactionStatus\":\"RJCT\"," +
                    "\"statusReason\":\"" + escape(reason) + "\"}]}";
    }

    private String preview(String s, int maxChars) {
        if (s == null) return "<empty>";
        String cut = s.substring(0, Math.min(maxChars, s.length()));
        return cut.replaceAll("\\d{8,}", "***masked***");
    }

    private String safe(String s) {
        if (s == null) return "";
        if (s.matches("\\d{8,}")) return "***masked***";
        return s;
    }
}
