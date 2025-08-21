package com.anz.fastpayment.router.config;

import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Instance;
import com.google.cloud.spanner.InstanceAdminClient;
import com.google.cloud.spanner.InstanceConfigId;
import com.google.cloud.spanner.InstanceId;
import com.google.cloud.spanner.InstanceInfo;
import com.google.cloud.spanner.Operation;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spring.data.spanner.core.admin.SpannerDatabaseAdminTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Configuration
@Profile("local")
public class SpannerLocalSchema {

    private static final Logger log = LoggerFactory.getLogger(SpannerLocalSchema.class);

    private final SpannerDatabaseAdminTemplate adminTemplate;

    @Value("${spring.cloud.gcp.spanner.instance-id}")
    private String instanceId;

    @Value("${spring.cloud.gcp.spanner.database}")
    private String databaseId;

    @Value("${spring.cloud.gcp.project-id:${SPRING_CLOUD_GCP_PROJECT_ID:local-project}}")
    private String projectId;

    public SpannerLocalSchema(SpannerDatabaseAdminTemplate adminTemplate) {
        this.adminTemplate = adminTemplate;
    }

    @PostConstruct
    public void ensureTables() {
        try {
            ensureInstanceAndDatabase();
            String ddl = "CREATE TABLE InboundMessages (" +
                    " puid STRING(16) NOT NULL,\n" +
                    " channel_id STRING(64),\n" +
                    " message_type STRING(64),\n" +
                    " received_at TIMESTAMP,\n" +
                    " raw_xml STRING(MAX),\n" +
                    " status STRING(32),\n" +
                    " error STRING(MAX)\n" +
                    ") PRIMARY KEY (puid)";
            try {
                adminTemplate.executeDdlStrings(Collections.singletonList(ddl), true);
                log.info("Created table InboundMessages in Spanner emulator");
            } catch (Exception ce) {
                String msg = ce.getMessage() == null ? "" : ce.getMessage();
                if (msg.contains("ALREADY_EXISTS") || msg.contains("AlreadyExists") || msg.contains("already exists")) {
                    log.info("InboundMessages table already exists; skipping create");
                } else {
                    throw ce;
                }
            }
        } catch (Exception e) {
            log.warn("Spanner emulator init warning: {}", e.getMessage());
        }
    }

    private void ensureInstanceAndDatabase() throws Exception {
        SpannerOptions options = SpannerOptions.newBuilder().setProjectId(projectId).build();
        try (Spanner spanner = options.getService()) {
            InstanceAdminClient instanceAdminClient = spanner.getInstanceAdminClient();
            DatabaseAdminClient databaseAdminClient = spanner.getDatabaseAdminClient();

            InstanceId iid = InstanceId.of(projectId, instanceId);
            boolean instanceExists;
            try {
                Instance i = instanceAdminClient.getInstance(iid.getInstance());
                instanceExists = i != null;
            } catch (Exception e) {
                instanceExists = false;
            }
            if (!instanceExists) {
                log.info("Creating Spanner emulator instance {} in project {}", instanceId, projectId);
                InstanceInfo info = InstanceInfo.newBuilder(iid)
                        .setDisplayName("Local Instance")
                        .setInstanceConfigId(InstanceConfigId.of(projectId, "emulator-config"))
                        .setNodeCount(1)
                        .build();
                instanceAdminClient.createInstance(info).get(30, TimeUnit.SECONDS);
            }

            DatabaseId db = DatabaseId.of(projectId, instanceId, databaseId);
            boolean dbExists;
            try {
                databaseAdminClient.getDatabase(instanceId, databaseId);
                dbExists = true;
            } catch (Exception e) {
                dbExists = false;
            }
            if (!dbExists) {
                log.info("Creating Spanner emulator database {} on instance {}", databaseId, instanceId);
                databaseAdminClient.createDatabase(instanceId, databaseId, Collections.emptyList()).get(30, TimeUnit.SECONDS);
            }
        }
    }
}


