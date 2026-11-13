package com.backend.finledger.attachment.service.impl;

import com.backend.finledger.attachment.service.FileStorageService;
import com.backend.finledger.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);
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

    private final Path rootLocation;

    public LocalFileStorageService(
            @Value("${finledger.attachments.base-path:${finledger.storage.local-dir:./uploads}}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory at " + this.rootLocation, e);
        }
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

        // Path structure: {rootLocation}/{companyId}/{entityType}/{entityId}/{uniqueFileName}
        String relativePath = Paths.get(String.valueOf(companyId), entityType.toUpperCase(Locale.ROOT),
                String.valueOf(entityId), uniqueFileName).toString().replace("\\", "/");

        Path targetPath = this.rootLocation.resolve(relativePath).normalize();

        try {
            if (!targetPath.startsWith(this.rootLocation)) {
                throw new BusinessException("SECURITY_ERROR", "Cannot store file outside target directory.");
            }
            Files.createDirectories(targetPath.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Stored file successfully for company {} entity {}:{} at {}", companyId, entityType, entityId, targetPath);
            return relativePath;
        } catch (IOException e) {
            log.error("Failed to store file: {}", e.getMessage(), e);
            throw new BusinessException("STORAGE_ERROR", "Failed to store uploaded file: " + e.getMessage());
        }
    }

    @Override
    public Resource loadAsResource(String storageKey) {
        try {
            Path filePath = this.rootLocation.resolve(storageKey).normalize();
            if (!filePath.startsWith(this.rootLocation)) {
                throw new BusinessException("SECURITY_ERROR", "Access to storage path outside root is forbidden.");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new BusinessException("FILE_NOT_FOUND", "Could not read file: " + storageKey);
            }
        } catch (MalformedURLException e) {
            throw new BusinessException("FILE_NOT_FOUND", "Malformed file path: " + storageKey);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Path filePath = this.rootLocation.resolve(storageKey).normalize();
            if (filePath.startsWith(this.rootLocation) && Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted physical file at {}", filePath);
            }
        } catch (IOException e) {
            log.warn("Failed to delete physical file {}: {}", storageKey, e.getMessage());
        }
    }

    private void validateFileType(String filename, String contentType) {
        String ext = getFileExtension(filename).toLowerCase(Locale.ROOT);
        boolean extAllowed = ALLOWED_EXTENSIONS.contains(ext);
        boolean typeAllowed = contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT));

        if (!extAllowed && !typeAllowed) {
            throw new BusinessException("INVALID_FILE_TYPE",
                    "Invalid file type. Only JPG, PNG, and PDF files up to 10MB are permitted.");
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return "";
    }
}

