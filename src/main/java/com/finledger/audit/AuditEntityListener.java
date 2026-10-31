package com.finledger.audit;

import com.backend.finledger.common.ApplicationContextProvider;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreRemove;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * JPA entity listener to automatically capture actions
 * (CREATE, UPDATE, DELETE, POST, VOID, APPROVE, REJECT, CONVERT)
 * with before/after JSON snapshots.
 */
public class AuditEntityListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEntityListener.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private static final Map<Integer, String> PRE_IMAGE_CACHE = new ConcurrentHashMap<>();
    private static final ThreadLocal<AuditAction> ACTION_OVERRIDE = new ThreadLocal<>();

    public static void setActionOverride(AuditAction action) {
        ACTION_OVERRIDE.set(action);
    }

    public static void clearActionOverride() {
        ACTION_OVERRIDE.remove();
    }

    @PostLoad
    public void onPostLoad(Object entity) {
        if (entity instanceof AuditLog) {
            return;
        }
        try {
            int key = System.identityHashCode(entity);
            PRE_IMAGE_CACHE.put(key, toJson(entity));
        } catch (Exception e) {
            log.trace("Could not capture pre-image for audit: {}", e.getMessage());
        }
    }

    @PostPersist
    public void onPostPersist(Object entity) {
        if (entity instanceof AuditLog) {
            return;
        }
        try {
            AuditAction action = ACTION_OVERRIDE.get() != null ? ACTION_OVERRIDE.get() : AuditAction.CREATE;
            recordAudit(entity, action, null, toJson(entity));
        } catch (Exception e) {
            log.error("Failed to capture audit log on CREATE for {}: {}", entity.getClass().getSimpleName(), e.getMessage());
        }
    }

    @PostUpdate
    public void onPostUpdate(Object entity) {
        if (entity instanceof AuditLog) {
            return;
        }
        try {
            int key = System.identityHashCode(entity);
            String oldJson = PRE_IMAGE_CACHE.remove(key);
            String newJson = toJson(entity);

            AuditAction action = ACTION_OVERRIDE.get();
            if (action == null) {
                action = detectActionFromState(entity, oldJson);
            }

            recordAudit(entity, action, oldJson, newJson);
        } catch (Exception e) {
            log.error("Failed to capture audit log on UPDATE for {}: {}", entity.getClass().getSimpleName(), e.getMessage());
        }
    }

    @PreRemove
    public void onPreRemove(Object entity) {
        if (entity instanceof AuditLog) {
            return;
        }
        try {
            int key = System.identityHashCode(entity);
            PRE_IMAGE_CACHE.put(key, toJson(entity));
        } catch (Exception e) {
            log.trace("Could not capture pre-remove image for audit: {}", e.getMessage());
        }
    }

    @PostRemove
    public void onPostRemove(Object entity) {
        if (entity instanceof AuditLog) {
            return;
        }
        try {
            int key = System.identityHashCode(entity);
            String oldJson = PRE_IMAGE_CACHE.remove(key);
            AuditAction action = ACTION_OVERRIDE.get() != null ? ACTION_OVERRIDE.get() : AuditAction.DELETE;
            recordAudit(entity, action, oldJson, null);
        } catch (Exception e) {
            log.error("Failed to capture audit log on DELETE for {}: {}", entity.getClass().getSimpleName(), e.getMessage());
        }
    }

    private AuditAction detectActionFromState(Object entity, String oldJson) {
        String currentStatus = extractStatus(entity);
        if (currentStatus != null) {
            return switch (currentStatus.toUpperCase()) {
                case "POSTED" -> AuditAction.POST;
                case "VOID" -> AuditAction.VOID;
                case "APPROVED" -> AuditAction.APPROVE;
                case "REJECTED" -> AuditAction.REJECT;
                case "CONVERTED" -> AuditAction.CONVERT;
                default -> AuditAction.UPDATE;
            };
        }
        return AuditAction.UPDATE;
    }

    private void recordAudit(Object entity, AuditAction action, String oldJson, String newJson) {
        Long entityId = extractId(entity);
        if (entityId == null) {
            entityId = 0L;
        }

        Long companyId = extractCompanyId(entity);
        if (companyId == null) {
            companyId = 1L; // fallback default tenant
        }

        Long userId = resolveCurrentUserId();
        String ipAddress = resolveClientIp();
        String entityName = entity.getClass().getSimpleName();

        saveAuditLog(companyId, userId, entityName, entityId, action, oldJson, newJson, ipAddress);
    }

    public static void logExplicitAction(Long companyId, Long userId, String entityName, Long entityId,
                                        AuditAction action, Object oldState, Object newState, String ipAddress) {
        String oldJson = oldState instanceof String ? (String) oldState : toJson(oldState);
        String newJson = newState instanceof String ? (String) newState : toJson(newState);
        saveAuditLog(companyId, userId, entityName, entityId, action, oldJson, newJson, ipAddress);
    }

    private static void saveAuditLog(Long companyId, Long userId, String entityName, Long entityId,
                                     AuditAction action, String oldJson, String newJson, String ipAddress) {
        JdbcTemplate jdbcTemplate = ApplicationContextProvider.getBean(JdbcTemplate.class);
        if (jdbcTemplate != null) {
            try {
                String sql = "INSERT INTO audit_logs (company_id, user_id, entity_name, entity_id, action, old_values, new_values, ip_address, created_at) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                jdbcTemplate.update(sql,
                        companyId,
                        userId,
                        entityName,
                        entityId,
                        action.name(),
                        oldJson,
                        newJson,
                        ipAddress,
                        new Timestamp(System.currentTimeMillis())
                );
                return;
            } catch (Exception e) {
                log.warn("JdbcTemplate insert for audit_log failed, attempting repository fallback: {}", e.getMessage());
            }
        }

        AuditLogRepository repository = ApplicationContextProvider.getBean(AuditLogRepository.class);
        if (repository != null) {
            try {
                AuditLog logEntry = new AuditLog(companyId, userId, entityName, entityId, action, oldJson, newJson, ipAddress);
                repository.save(logEntry);
            } catch (Exception e) {
                log.error("Failed to persist audit log: {}", e.getMessage());
            }
        }
    }

    private static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            log.trace("JSON serialization error: {}", e.getMessage());
            return "{\"error\":\"Serialization failed: " + e.getMessage() + "\"}";
        }
    }

    private Long extractId(Object entity) {
        return extractLongProperty(entity, "id", "getId");
    }

    private Long extractCompanyId(Object entity) {
        return extractLongProperty(entity, "companyId", "getCompanyId");
    }

    private Long extractLongProperty(Object entity, String fieldName, String methodName) {
        try {
            Method method = entity.getClass().getMethod(methodName);
            Object res = method.invoke(entity);
            if (res instanceof Number num) {
                return num.longValue();
            }
        } catch (Exception ignored) {
        }

        try {
            Field field = getFieldAnywhere(entity.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                Object val = field.get(entity);
                if (val instanceof Number num) {
                    return num.longValue();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String extractStatus(Object entity) {
        try {
            Method method = entity.getClass().getMethod("getStatus");
            Object res = method.invoke(entity);
            if (res != null) {
                return res.toString();
            }
        } catch (Exception ignored) {
        }

        try {
            Field field = getFieldAnywhere(entity.getClass(), "status");
            if (field != null) {
                field.setAccessible(true);
                Object val = field.get(entity);
                if (val != null) {
                    return val.toString();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Field getFieldAnywhere(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private Long resolveCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Number num) {
                return num.longValue();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String resolveClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xff = request.getHeader("X-Forwarded-For");
                if (xff != null && !xff.isBlank()) {
                    return xff.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}

