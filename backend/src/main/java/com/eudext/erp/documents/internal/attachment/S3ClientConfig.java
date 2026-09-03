package com.eudext.erp.documents.internal.attachment;

import java.net.URI;
import org.springframework.beans.factory.annotation.Qualifier;
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
 * DOC-1. Only wired up when {@code eudext.documents.attachments.enabled=true} (off by default — see {@link
 * AttachmentStorageProperties}), same pattern and reasoning as {@code
 * com.eudext.erp.audit.internal.archive.S3ClientConfig}: no environment needs a reachable S3-compatible endpoint
 * just to start the application. The bean is named explicitly and injected by {@link Qualifier} below so it can't be
 * confused with the audit module's own, separately-toggled {@code S3Client} bean when both happen to be enabled at
 * once (e.g. the {@code docker} profile).
 */
@Configuration
@EnableConfigurationProperties(AttachmentStorageProperties.class)
class S3ClientConfig {

    @Bean(name = "documentsAttachmentsS3Client")
    @ConditionalOnProperty(prefix = "eudext.documents.attachments", name = "enabled", havingValue = "true")
    S3Client documentsAttachmentsS3Client(AttachmentStorageProperties properties) {
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
    @ConditionalOnProperty(prefix = "eudext.documents.attachments", name = "enabled", havingValue = "true")
    AttachmentStorage attachmentStorage(
            @Qualifier("documentsAttachmentsS3Client") S3Client s3Client, AttachmentStorageProperties properties) {
        return new S3AttachmentStorage(s3Client, properties.getBucket());
    }
}
