package dev.truewinter.simofa.s3;

import dev.truewinter.simofa.config.Config;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.File;
import java.net.URI;
import java.time.Duration;

public class S3Service {
    private final String region;
    private final String bucket;
    private final String endpoint;
    private final AwsBasicCredentials credentials;

    public S3Service(Config config) {
        this.region = config.getS3Region();
        this.bucket = config.getS3Bucket();
        this.endpoint = config.getS3Endpoint();
        this.credentials = AwsBasicCredentials.create(config.getS3AccessKeyId(), config.getS3SecretAccessKey());
    }

    public String getPresignedUrl(String s3Path) {
        try (S3Presigner presigner = getS3Presigner()) {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Path)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(15))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(presignRequest);

            return presignedGetObjectRequest.url().toString();
        }
    }

    public void upload(File file, String s3Path) {
        try (S3Client s3Client = getS3Client()) {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Path)
                    .contentType("application/zip")
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromFile(file));
        }
    }

    public void delete(String s3Path) {
        try (S3Client s3Client = getS3Client()) {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Path)
                    .build();
            s3Client.deleteObject(deleteRequest);
        }
    }

    private S3Presigner getS3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    private S3Client getS3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}
