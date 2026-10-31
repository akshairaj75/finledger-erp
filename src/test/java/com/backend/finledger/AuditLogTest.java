package com.backend.finledger;

import com.finledger.audit.AuditAction;
import com.finledger.audit.AuditEntityListener;
import com.finledger.audit.AuditLog;
import com.finledger.audit.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuditLogTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long testCompanyId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT IGNORE INTO currencies (code, name, symbol, decimal_places) VALUES ('AED', 'UAE Dirham', 'AED', 2)");
        jdbcTemplate.update("INSERT IGNORE INTO companies (id, code, legal_name, country_code, base_currency) " +
                "VALUES (1001, 'TEST_CO', 'Test Company LLC', 'AE', 'AED')");
        testCompanyId = 1001L;
    }

    @Test
    @DisplayName("Should persist and retrieve audit logs")
    void testAuditLogPersistence() {
        AuditLog logEntry = new AuditLog(
                testCompanyId,
                null,
                "Invoice",
                9414L,
                AuditAction.CREATE,
                null,
                "{\"invoiceNo\":\"INV-2026-0414\",\"total\":42525.00}",
                "127.0.0.1"
        );

        AuditLog saved = auditLogRepository.save(logEntry);
        assertNotNull(saved.getId());

        List<AuditLog> found = auditLogRepository.findByEntityNameAndEntityIdOrderByCreatedAtDesc("Invoice", 9414L);
        assertFalse(found.isEmpty());
        assertEquals(AuditAction.CREATE, found.get(0).getAction());
        assertEquals("Invoice", found.get(0).getEntityName());
    }

    @Test
    @DisplayName("Should log explicit actions like POST, VOID, APPROVE")
    void testExplicitAuditAction() {
        AuditEntityListener.logExplicitAction(
                testCompanyId,
                null,
                "Invoice",
                9414L,
                AuditAction.POST,
                "{\"status\":\"DRAFT\"}",
                "{\"status\":\"POSTED\"}",
                "127.0.0.1"
        );

        List<AuditLog> logs = auditLogRepository.findByEntityNameAndEntityIdOrderByCreatedAtDesc("Invoice", 9414L);
        boolean hasPost = logs.stream().anyMatch(l -> l.getAction() == AuditAction.POST);
        assertTrue(hasPost);
    }
}
