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
        if (log.isDebugEnabled()) {
            log.debug("[MQ] Payload preview (first 500 chars) key={} => {}", messageKey,
                    payload.substring(0, Math.min(500, payload.length())));
        }
        try {
            routerOrchestrator.processInboundXml(payload);
        } catch (Throwable t) {
            String preview = payload.substring(0, Math.min(200, payload.length()));
            log.error("[MQ] Error processing message key={}, size={}, preview='{}': {}", messageKey, size, preview, t.toString(), t);
            // Do not rethrow to avoid poison-message redelivery loops
        }
    }
}


