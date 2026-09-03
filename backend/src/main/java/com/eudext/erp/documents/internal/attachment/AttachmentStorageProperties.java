package com.eudext.erp.documents.internal.attachment;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** DOC-1. Off by default so no environment needs a reachable object store just to boot — see {@link #enabled}. */
@ConfigurationProperties(prefix = "eudext.documents.attachments")
public class AttachmentStorageProperties {

    /** Master switch — off by default; the {@code docker} profile turns it on against the local MinIO instance. */
    private boolean enabled = false;

    private String s3Endpoint;
    private String region = "us-east-1";
    private String bucket = "eudext-attachments";
    private String accessKey;
    private String secretKey;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getS3Endpoint() {
        return s3Endpoint;
    }

    public void setS3Endpoint(String s3Endpoint) {
        this.s3Endpoint = s3Endpoint;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}
