package com.backend.finledger.attachment.controller;

import com.backend.finledger.attachment.dto.AttachmentDto;
import com.backend.finledger.attachment.service.AttachmentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentDto> uploadAttachment(
            @RequestParam("file") MultipartFile file,
            @RequestParam("entityType") String entityType,
            @RequestParam("entityId") Long entityId) {
        AttachmentDto dto = attachmentService.uploadAttachment(entityType, entityId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id) {
        AttachmentDto metadata = attachmentService.getAttachmentById(id);
        Resource resource = attachmentService.downloadAttachment(id);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(metadata.contentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + metadata.fileName() + "\"")
                .body(resource);
    }

    @GetMapping("/{id}/metadata")
    public ResponseEntity<AttachmentDto> getAttachmentMetadata(@PathVariable Long id) {
        return ResponseEntity.ok(attachmentService.getAttachmentById(id));
    }

    @GetMapping
    public ResponseEntity<List<AttachmentDto>> getAttachments(
            @RequestParam("entityType") String entityType,
            @RequestParam("entityId") Long entityId) {
        return ResponseEntity.ok(attachmentService.getAttachmentsByEntity(entityType, entityId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long id) {
        attachmentService.deleteAttachment(id);
        return ResponseEntity.noContent().build();
    }
}

