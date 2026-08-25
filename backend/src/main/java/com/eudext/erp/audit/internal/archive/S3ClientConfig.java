package com.eudext.erp.audit.internal.archive;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * AUD-5. Only wired up when {@code eudext.audit.archive.enabled=true}
 * (off by default — see {@link AuditArchiveProperties}) so that no
 * environment needs a reachable S3-compatible endpoint just to start the
 * application; local dev/test/CI never sets this, only the {@code docker}
 * profile (MinIO) and staging/production (real S3) do.
 */
@Configuration
@EnableConfigurationProperties(AuditArchiveProperties.class)
class S3ClientConfig {

    @Bean
    @ConditionalOnProperty(prefix = "eudext.audit.archive", name = "enabled", havingValue = "true")
    S3Client auditArchiveS3Client(AuditArchiveProperties properties) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())))
                // MinIO is not virtual-hosted-style; path-style addressing is required for it
                // and harmless against real AWS S3, so it's set unconditionally here.
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        if (properties.getS3Endpoint() != null && !properties.getS3Endpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getS3Endpoint()));
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "eudext.audit.archive", name = "enabled", havingValue = "true")
    AuditArchiveStorage auditArchiveStorage(S3Client s3Client, AuditArchiveProperties properties) {
        return new S3AuditArchiveStorage(s3Client, properties.getBucket());
    }
}
