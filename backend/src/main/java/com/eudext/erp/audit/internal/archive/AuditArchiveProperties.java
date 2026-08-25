package com.eudext.erp.audit.internal.archive;

import java.time.Period;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AUD-5. {@code tenantIds} is a stopgap: today there is no tenant registry
 * to enumerate tenants from (multi-tenancy is still an opaque,
 * caller-supplied id everywhere in Phase 0 — see the V4 migration comment
 * on {@code users} for the same limitation applied to login), so the
 * scheduled sweep can only archive tenants it's explicitly told about.
 * Epic 0.9's tenant registry replaces this with real enumeration.
 */
@ConfigurationProperties(prefix = "eudext.audit.archive")
public class AuditArchiveProperties {

    /** Master switch — off by default so no environment needs a reachable object store just to boot. */
    private boolean enabled = false;

    private String s3Endpoint;
    private String region = "us-east-1";
    private String bucket = "eudext-audit-archive";
    private String accessKey;
    private String secretKey;

    /** AUD-5: rows older than this are eligible for archival. */
    private Period retentionBeforeArchive = Period.ofYears(2);

    private List<UUID> tenantIds = List.of();

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

    public Period getRetentionBeforeArchive() {
        return retentionBeforeArchive;
    }

    public void setRetentionBeforeArchive(Period retentionBeforeArchive) {
        this.retentionBeforeArchive = retentionBeforeArchive;
    }

    public List<UUID> getTenantIds() {
        return tenantIds;
    }

    public void setTenantIds(List<UUID> tenantIds) {
        this.tenantIds = tenantIds;
    }
}
