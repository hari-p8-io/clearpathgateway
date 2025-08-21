package com.anz.fastpayment.router.messaging;

import com.anz.fastpayment.router.service.RouterOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ActiveMqMessageListener {

    private static final Logger log = LoggerFactory.getLogger(ActiveMqMessageListener.class);

    private final RouterOrchestrator routerOrchestrator;

    @Value("${app.router.timeout-ms:4500}")
    private long processingTimeoutMs;

    @Value("${app.logging.payload-preview-enabled:false}")
    private boolean payloadPreviewEnabled;

    public ActiveMqMessageListener(RouterOrchestrator routerOrchestrator) {
        this.routerOrchestrator = routerOrchestrator;
    }

    @JmsListener(destination = "${app.activemq.input-queue:payment.inbound}")
    public void onMessage(@Payload String payload) {
        String messageKey = UUID.randomUUID().toString();
        int size = payload == null ? 0 : payload.length();
        log.info("[MQ] Received message key={}, size={} bytes", messageKey, size);
        if (payload == null || payload.trim().isEmpty()) {
            log.warn("[MQ] Ignoring empty/null payload for key={}", messageKey);
            return;
        }
        if (payloadPreviewEnabled && log.isDebugEnabled()) {
            log.debug("[MQ] Payload preview (first 500 chars) key={} => {}", messageKey,
                    safePreview(payload, 500));
        }
        try {
            routerOrchestrator.processInboundXml(payload);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            String preview = safePreview(payload, 200);
            log.error("[MQ] Error processing message key={}, size={}, preview='{}': {}", messageKey, size, preview, e.toString(), e);
            // Rethrow so broker redelivery/DLQ policies can apply
            throw e;
        }
    }

    private String safePreview(String s, int maxChars) {
        if (s == null || s.isEmpty()) return "<empty>";
        String cut = s.substring(0, Math.min(maxChars, s.length()));
        // Mask long digit runs (likely PAN/account numbers)
        cut = cut.replaceAll("\\d{6,}", "***masked***");
        // Mask common account-like tags
        cut = cut.replaceAll("(?i)(<IBAN>)(.*?)(</IBAN>)", "$1***masked***$3");
        cut = cut.replaceAll("(?i)(<AcctNbr>)(.*?)(</AcctNbr>)", "$1***masked***$3");
        cut = cut.replaceAll("(?i)(<CardNbr>)(.*?)(</CardNbr>)", "$1***masked***$3");
        return cut;
    }
}


