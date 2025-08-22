package com.anz.fastpayment.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

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
        jmsTemplate.convertAndSend(pacs002Queue, pacs002Xml);
        log.info("[SENDER] Sent pacs.002 to queue {} (size={} chars)", pacs002Queue, pacs002Xml == null ? 0 : pacs002Xml.length());
    }
}


