package com.backend.finledger.audit.controller;

import com.backend.finledger.audit.entity.AuditAction;
import com.backend.finledger.audit.entity.AuditLog;
import com.backend.finledger.audit.repository.AuditLogRepository;
import com.backend.finledger.common.PageResponse;
import com.backend.finledger.security.CurrentUser;
import com.backend.finledger.user.entity.User;
import com.backend.finledger.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogController(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public record AuditLogDto(
            Long id,
            Long companyId,
            Long userId,
            String userName,
            String entityName,
            Long entityId,
            AuditAction action,
            String oldValues,
            String newValues,
            String ipAddress,
            Instant createdAt
    ) {}

    @GetMapping
    @PreAuthorize("hasAuthority('SETTINGS_EDIT') or hasAuthority('COMPANY_VIEW') or hasAuthority('REPORT_VIEW')")
    public ResponseEntity<PageResponse<AuditLogDto>> searchLogs(
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 25) Pageable pageable) {

        Long companyId = CurrentUser.getCompanyId();
        String entityFilter = (entity != null && !entity.isBlank()) ? entity.trim() : null;
        Instant fromInstant = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant toInstant = to != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;

        Page<AuditLog> page = auditLogRepository.searchLogs(companyId, entityFilter, userId, fromInstant, toInstant, pageable);
        Map<Long, String> userNames = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getFullName, (a, b) -> a));

        Page<AuditLogDto> dtoPage = page.map(a -> new AuditLogDto(
                a.getId(),
                a.getCompanyId(),
                a.getUserId(),
                a.getUserId() != null ? userNames.getOrDefault(a.getUserId(), "User #" + a.getUserId()) : "System",
                a.getEntityName(),
                a.getEntityId(),
                a.getAction(),
                a.getOldValues(),
                a.getNewValues(),
                a.getIpAddress(),
                a.getCreatedAt()
        ));

        return ResponseEntity.ok(PageResponse.from(dtoPage));
    }
}

