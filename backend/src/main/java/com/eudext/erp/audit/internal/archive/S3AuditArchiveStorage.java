package com.eudext.erp.audit.internal.archive;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** AUD-5: writes archived audit batches to an S3-compatible bucket (real S3, or MinIO in dev/docker). */
class S3AuditArchiveStorage implements AuditArchiveStorage {

    private final S3Client s3Client;
    private final String bucket;

    S3AuditArchiveStorage(S3Client s3Client, String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        ensureBucketExists();
    }

    @Override
    public void put(String objectKey, byte[] content) {
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(objectKey).contentType("application/json").build(),
                RequestBody.fromBytes(content));
    }

    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(builder -> builder.bucket(bucket));
        }
    }
}
