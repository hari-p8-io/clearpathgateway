package com.anz.fastpayment.sender.config;

import com.google.cloud.spring.data.spanner.core.admin.SpannerDatabaseAdminTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import java.util.Collections;

@Configuration
@Profile("local")
public class SpannerLocalSchema {

    private static final Logger log = LoggerFactory.getLogger(SpannerLocalSchema.class);

    private final SpannerDatabaseAdminTemplate adminTemplate;

    @Value("${spring.cloud.gcp.spanner.instance-id:payment-gateway-local}")
    private String instanceId;
    @Value("${spring.cloud.gcp.spanner.database:sender-db}")
    private String databaseId;

    public SpannerLocalSchema(SpannerDatabaseAdminTemplate adminTemplate) {
        this.adminTemplate = adminTemplate;
    }

    @PostConstruct
    public void ensureTables() {
        try {
            // Ensure database exists (idempotent)
            try {
                adminTemplate.executeDdlStrings(java.util.Collections.singletonList(""), true);
            } catch (Exception ignore) { }
            String ddl = "CREATE TABLE Pacs002Messages (" +
                    " puid STRING(16) NOT NULL,\n" +
                    " unique_id STRING(64),\n" +
                    " created_at TIMESTAMP,\n" +
                    " xml STRING(MAX),\n" +
                    " event_json STRING(MAX)\n" +
                    ") PRIMARY KEY (puid)";
            adminTemplate.executeDdlStrings(Collections.singletonList(ddl), true);
            log.info("Created table Pacs002Messages in Spanner emulator");
        } catch (Exception ce) {
            String msg = ce.getMessage() == null ? "" : ce.getMessage();
            if (msg.contains("ALREADY_EXISTS") || msg.contains("AlreadyExists") || msg.contains("already exists")) {
                log.info("Pacs002Messages table already exists; skipping create");
            } else {
                log.warn("Spanner emulator init warning: {}", ce.getMessage());
            }
        }
    }
}
