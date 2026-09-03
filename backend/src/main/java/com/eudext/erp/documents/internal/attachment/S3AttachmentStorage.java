package com.eudext.erp.documents.internal.attachment;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** DOC-1: writes/reads attachment bytes against an S3-compatible bucket (real S3, or MinIO in dev/docker). */
class S3AttachmentStorage implements AttachmentStorage {

    private final S3Client s3Client;
    private final String bucket;

    S3AttachmentStorage(S3Client s3Client, String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        ensureBucketExists();
    }

    @Override
    public void put(String objectKey, byte[] content, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(objectKey).contentType(contentType).build(),
                RequestBody.fromBytes(content));
    }

    @Override
    public byte[] get(String objectKey) {
        try (ResponseInputStream<GetObjectResponse> response =
                s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(objectKey).build())) {
            return response.readAllBytes();
        } catch (java.io.IOException e) {
            throw new AttachmentStorageException("Failed to read attachment " + objectKey, e);
        }
    }

    @Override
    public void delete(String objectKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
    }

    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(builder -> builder.bucket(bucket));
        }
    }
}
