package com.anz.fastpayment.router.service;

import com.anz.fastpayment.router.model.InboundMessage;
import com.anz.fastpayment.router.repository.InboundMessageRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class RouterOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(RouterOrchestrator.class);

    private final PuidGenerator puidGenerator;
    private final InboundMessageRepository inboundRepo; // optional in local profile
    private final Iso20022MessageTypeDetector typeDetector;
    private final XmlSchemaValidator xmlSchemaValidator;
    private final Iso20022Transformer transformer;
    private final KafkaPublisher kafkaPublisher;

    public RouterOrchestrator(PuidGenerator puidGenerator,
                              ObjectProvider<InboundMessageRepository> inboundRepoProvider,
                              Iso20022MessageTypeDetector typeDetector,
                              XmlSchemaValidator xmlSchemaValidator,
                              Iso20022Transformer transformer,
                              KafkaPublisher kafkaPublisher) {
        this.puidGenerator = puidGenerator;
        this.inboundRepo = inboundRepoProvider.getIfAvailable();
        this.typeDetector = typeDetector;
        this.xmlSchemaValidator = xmlSchemaValidator;
        this.transformer = transformer;
        this.kafkaPublisher = kafkaPublisher;
    }

    public void processInboundXml(String xml) {
        log.info("[PROC] Starting processing for inbound message");
        String puid = puidGenerator.nextPuid();
        log.info("[PUID] Generated PUID={}", puid);
        String messageType = typeDetector.detectType(xml);
        log.info("[DETECT] Detected messageType={}", messageType);

        try {
            // persist RECEIVED
            if (inboundRepo != null) {
                InboundMessage m = new InboundMessage();
                m.setPuid(puid);
                m.setChannelId("G3I");
                m.setMessageType(messageType);
                m.setReceivedAt(java.time.Instant.now());
                m.setRawXml(xml);
                m.setStatus("RECEIVED");
                inboundRepo.save(m);
            }
            // XSD validation
            log.info("[XSD] Validating XML against XSD for type={}", messageType);
            xmlSchemaValidator.validate(xml, messageType);
            log.info("[XSD] Validation successful for PUID={}", puid);
            if (inboundRepo != null) {
                InboundMessage m = inboundRepo.findById(puid).orElse(null);
                if (m != null) { m.setStatus("VALIDATED"); inboundRepo.save(m); }
            }

            // Transform to unified JSON
            log.info("[TRANSFORM] Transforming XML to unified JSON for PUID={}", puid);
            String unifiedJson = transformer.toUnifiedJson(xml, messageType, puid);
            if (log.isDebugEnabled()) {
                log.debug("[TRANSFORM] Unified JSON (first 500 chars) PUID={} => {}", puid,
                        unifiedJson.substring(0, Math.min(500, unifiedJson.length())));
            }

            // Publish to Kafka
            log.info("[KAFKA] Publishing valid message to topic payment-messages with key={}", puid);
            kafkaPublisher.publishValid(puid, unifiedJson);
            log.info("[KAFKA] Publish complete for key={}", puid);
            if (inboundRepo != null) {
                InboundMessage m = inboundRepo.findById(puid).orElse(null);
                if (m != null) { m.setStatus("PUBLISHED"); inboundRepo.save(m); }
            }
        } catch (Exception ex) {
            log.error("[ERROR] Processing failed for PUID {}: {}", puid, ex.getMessage(), ex);
            log.warn("[KAFKA] Routing invalid message to exception topic, key={}", puid);
            kafkaPublisher.publishInvalid(puid, xml);
            if (inboundRepo != null) {
                InboundMessage m = inboundRepo.findById(puid).orElse(null);
                if (m != null) { m.setStatus("ERROR"); m.setError(ex.getMessage()); inboundRepo.save(m); }
            }
        }
    }
}


