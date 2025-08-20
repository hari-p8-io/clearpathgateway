package com.anz.fastpayment.router.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.topics.payment-messages:payment-messages}")
    private String paymentMessagesTopic;

    @Value("${app.kafka.topics.exception-queue:exception-queue}")
    private String exceptionTopic;

    public KafkaPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishValid(String key, String payload) {
        log.info("Publishing valid message to topic {} with key {}", paymentMessagesTopic, key);
        kafkaTemplate.send(paymentMessagesTopic, key, payload);
    }

    public void publishInvalid(String key, String payload) {
        log.warn("Publishing invalid message to exception topic {} with key {}", exceptionTopic, key);
        kafkaTemplate.send(exceptionTopic, key, payload);
    }
}


