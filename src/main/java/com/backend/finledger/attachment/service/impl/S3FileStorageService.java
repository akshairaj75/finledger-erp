package com.backend.finledger.attachment.service.impl;

import com.backend.finledger.attachment.service.FileStorageService;
import com.backend.finledger.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * S3-compatible FileStorageService adapter supporting AWS S3, MinIO, and LocalStack.
 */
@Service
@ConditionalOnProperty(name = "finledger.storage.type", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(S3FileStorageService.class);
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB per spec

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "pdf");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/pjpeg",
            "image/png",
            "image/x-png",
            "application/pdf"
    );

    private final String bucket;
    private final S3Client s3Client;

    public S3FileStorageService(
            @Value("${finledger.storage.s3.bucket:finledger-attachments}") String bucket,
            @Value("${finledger.storage.s3.region:us-east-1}") String region,
            @Value("${finledger.storage.s3.endpoint:}") String endpoint) {
        this.bucket = bucket;

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region));

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                   .forcePathStyle(true); // MinIO path-style access
        }

        this.s3Client = builder.build();
    }

    @Override
    public String store(Long companyId, String entityType, Long entityId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "Cannot store empty file.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("FILE_TOO_LARGE", "File exceeds maximum permitted size of 10MB.");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment"
        );
        validateFileType(originalFilename, file.getContentType());

        String extension = getFileExtension(originalFilename);
        String uniqueFileName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        String storageKey = String.format("%d/%s/%d/%s",
                companyId, entityType.toUpperCase(Locale.ROOT), entityId, uniqueFileName);

        try {
            PutObjectRequest putReq = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putReq, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("Stored file in S3 bucket '{}' at key '{}'", bucket, storageKey);
            return storageKey;
        } catch (IOException e) {
            log.error("Failed to read input stream for S3 upload: {}", e.getMessage(), e);
            throw new BusinessException("STORAGE_ERROR", "Failed to store file in S3: " + e.getMessage());
        } catch (S3Exception e) {
            log.error("S3 error while uploading key '{}': {}", storageKey, e.awsErrorDetails().errorMessage(), e);
            throw new BusinessException("STORAGE_ERROR", "S3 upload failed: " + e.awsErrorDetails().errorMessage());
        }
    }

    @Override
    public Resource loadAsResource(String storageKey) {
        try {
            GetObjectRequest getReq = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build();

            ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(getReq);
            String filename = storageKey.substring(storageKey.lastIndexOf('/') + 1);
            return new ByteArrayResource(bytes.asByteArray()) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
        } catch (NoSuchKeyException e) {
            throw new BusinessException("FILE_NOT_FOUND", "File not found in S3 storage at: " + storageKey);
        } catch (S3Exception e) {
            log.error("S3 error while downloading key '{}': {}", storageKey, e.awsErrorDetails().errorMessage(), e);
            throw new BusinessException("STORAGE_ERROR", "S3 download failed: " + e.awsErrorDetails().errorMessage());
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build();
            s3Client.deleteObject(delReq);
            log.info("Deleted S3 object at key '{}' in bucket '{}'", storageKey, bucket);
        } catch (S3Exception e) {
            log.error("Failed to delete S3 object at key '{}': {}", storageKey, e.awsErrorDetails().errorMessage(), e);
        }
    }

    private void validateFileType(String filename, String contentType) {
        String ext = getFileExtension(filename).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException("INVALID_FILE_TYPE", "File extension '." + ext + "' is not permitted.");
        }
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("INVALID_FILE_TYPE", "MIME type '" + contentType + "' is not permitted.");
        }
    }

    private String getFileExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot > 0 && dot < filename.length() - 1) ? filename.substring(dot + 1) : "";
    }
}
