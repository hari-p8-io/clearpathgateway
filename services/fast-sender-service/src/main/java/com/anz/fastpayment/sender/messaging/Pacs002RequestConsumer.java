package com.anz.fastpayment.sender.messaging;

import com.anz.fastpayment.sender.model.Pacs002Request;
import com.anz.fastpayment.sender.model.Pacs002Response;
import com.anz.fastpayment.sender.service.Pacs002Service;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class Pacs002RequestConsumer {

    private static final Logger log = LoggerFactory.getLogger(Pacs002RequestConsumer.class);

    private final Pacs002Service pacs002Service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.kafka.topics.pacs002-requests:pacs002-requests}")
    private String pacs002RequestsTopic;

    public Pacs002RequestConsumer(Pacs002Service pacs002Service) {
        this.pacs002Service = pacs002Service;
    }

    @KafkaListener(topics = "#{'${app.kafka.topics.pacs002-requests:pacs002-requests}'}", groupId = "${spring.application.name}")
    public void onMessage(ConsumerRecord<String, String> record) {
        try {
            String key = record.key();
            String value = record.value();
            log.info("[KAFKA] Received pacs002 request key={}, topic={}", key, record.topic());
            Pacs002Request req = objectMapper.readValue(value, Pacs002Request.class);
            Pacs002Response resp = pacs002Service.handlePacs002Request(req);
            log.info("[PACS002] Accepted request for PUID={}, status={}", resp.getPuid(), resp.getStatus());
        } catch (Exception ex) {
            log.warn("[KAFKA] Failed processing pacs002 request: {}", ex.getMessage(), ex);
        }
    }
}
