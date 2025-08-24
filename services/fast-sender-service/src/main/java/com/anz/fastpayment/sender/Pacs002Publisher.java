package com.anz.fastpayment.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.jms.JmsException;

@Component
public class Pacs002Publisher {

    private static final Logger log = LoggerFactory.getLogger(Pacs002Publisher.class);

    private final JmsTemplate jmsTemplate;

    @Value("${app.activemq.pacs002-queue:pacs002.outbound}")
    private String pacs002Queue;

    public Pacs002Publisher(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void sendPacs002Xml(String pacs002Xml) {
        if (pacs002Xml == null) {
            log.error("[SENDER] Null pacs002Xml payload, not sending to queue {}", pacs002Queue);
            return;
        }
        try {
            jmsTemplate.convertAndSend(pacs002Queue, pacs002Xml);
            log.info("[SENDER] Sent pacs.002 to queue {} (size={} chars)", pacs002Queue, pacs002Xml.length());
        } catch (JmsException | RuntimeException ex) {
            log.error("[SENDER] Failed sending pacs.002 to queue {}: {}", pacs002Queue, ex.getMessage(), ex);
            // choose not to rethrow to keep flow resilient; service can decide policy here
        }
    }
}


