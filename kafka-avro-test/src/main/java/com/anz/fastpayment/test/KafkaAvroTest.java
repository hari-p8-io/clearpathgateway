package com.anz.fastpayment.test;

import com.anz.fastpayment.test.TestPaymentMessage;
import com.anz.fastpayment.test.PaymentStatus;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

public class KafkaAvroTest {
    
    private static final String TOPIC = "test-payment-messages";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    
    public static void main(String[] args) {
        System.out.println("🚀 Starting Kafka Avro Test Application");
        System.out.println("========================================");
        
        try {
            // Start Kafka producer and consumer
            KafkaProducer<String, TestPaymentMessage> producer = createProducer();
            KafkaConsumer<String, TestPaymentMessage> consumer = createConsumer();
            
            // Subscribe to topic
            consumer.subscribe(Collections.singletonList(TOPIC));
            
            // Start consumer thread
            Thread consumerThread = new Thread(() -> consumeMessages(consumer));
            consumerThread.setDaemon(true);
            consumerThread.start();
            
            // Wait a bit for consumer to be ready
            Thread.sleep(2000);
            
            // Produce test messages
            produceTestMessages(producer);
            
            // Wait for messages to be consumed
            Thread.sleep(5000);
            
            // Close resources
            producer.close();
            consumer.close();
            
            System.out.println("✅ Test completed successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed with error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static KafkaProducer<String, TestPaymentMessage> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put("schema.registry.url", "http://localhost:8081");
        
        return new KafkaProducer<>(props);
    }
    
    private static KafkaConsumer<String, TestPaymentMessage> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put("schema.registry.url", "http://localhost:8081");
        props.put("specific.avro.reader", true);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        return new KafkaConsumer<>(props);
    }
    
    private static void produceTestMessages(KafkaProducer<String, TestPaymentMessage> producer) {
        System.out.println("\n📤 Producing test messages...");
        
        // Create test payment messages
        TestPaymentMessage message1 = TestPaymentMessage.newBuilder()
                .setPaymentId("PAY-" + UUID.randomUUID().toString().substring(0, 8))
                .setAmount(1000.50)
                .setCurrency("USD")
                .setSenderAccount("ACC-001-123456")
                .setReceiverAccount("ACC-002-789012")
                .setTimestamp(Instant.now().toString())
                .setStatus(PaymentStatus.PENDING)
                .setDescription("Test payment 1")
                .build();
        
        TestPaymentMessage message2 = TestPaymentMessage.newBuilder()
                .setPaymentId("PAY-" + UUID.randomUUID().toString().substring(0, 8))
                .setAmount(2500.75)
                .setCurrency("EUR")
                .setSenderAccount("ACC-003-345678")
                .setReceiverAccount("ACC-004-901234")
                .setTimestamp(Instant.now().toString())
                .setStatus(PaymentStatus.PROCESSING)
                .setDescription("Test payment 2")
                .build();
        
        TestPaymentMessage message3 = TestPaymentMessage.newBuilder()
                .setPaymentId("PAY-" + UUID.randomUUID().toString().substring(0, 8))
                .setAmount(500.25)
                .setCurrency("GBP")
                .setSenderAccount("ACC-005-567890")
                .setReceiverAccount("ACC-006-123456")
                .setTimestamp(Instant.now().toString())
                .setStatus(PaymentStatus.COMPLETED)
                .setDescription(null) // Testing nullable field
                .build();
        
        // Send messages
        producer.send(new ProducerRecord<>(TOPIC, "key1", message1));
        producer.send(new ProducerRecord<>(TOPIC, "key2", message2));
        producer.send(new ProducerRecord<>(TOPIC, "key3", message3));
        
        producer.flush();
        System.out.println("✅ 3 test messages sent to topic: " + TOPIC);
    }
    
    private static void consumeMessages(KafkaConsumer<String, TestPaymentMessage> consumer) {
        System.out.println("\n📥 Starting message consumption...");
        
        try {
            while (true) {
                ConsumerRecords<String, TestPaymentMessage> records = consumer.poll(Duration.ofMillis(100));
                
                for (ConsumerRecord<String, TestPaymentMessage> record : records) {
                    TestPaymentMessage message = record.value();
                    
                    System.out.println("\n📨 Received message:");
                    System.out.println("   Key: " + record.key());
                    System.out.println("   Partition: " + record.partition());
                    System.out.println("   Offset: " + record.offset());
                    System.out.println("   Payment ID: " + message.getPaymentId());
                    System.out.println("   Amount: " + message.getAmount() + " " + message.getCurrency());
                    System.out.println("   From: " + message.getSenderAccount());
                    System.out.println("   To: " + message.getReceiverAccount());
                    System.out.println("   Status: " + message.getStatus());
                    System.out.println("   Description: " + (message.getDescription() != null ? message.getDescription() : "N/A"));
                    System.out.println("   Timestamp: " + message.getTimestamp());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Consumer error: " + e.getMessage());
        }
    }
}
