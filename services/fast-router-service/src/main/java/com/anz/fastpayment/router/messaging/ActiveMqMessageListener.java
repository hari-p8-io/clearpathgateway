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
        log.info("[MQ] Received message key={}, size={} bytes", messageKey, payload != null ? payload.length() : 0);
        if (log.isDebugEnabled()) {
            log.debug("[MQ] Payload preview (first 500 chars) key={} => {}", messageKey,
                    payload == null ? "<null>" : payload.substring(0, Math.min(500, payload.length())));
        }

        routerOrchestrator.processInboundXml(payload);
    }
}


