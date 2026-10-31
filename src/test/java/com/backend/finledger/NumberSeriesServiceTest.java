package com.backend.finledger;

import com.backend.finledger.common.DocType;
import com.backend.finledger.common.NumberSeriesRepository;
import com.finledger.backend.common.NumberSeriesService;
import java.time.Year;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class NumberSeriesServiceTest {

    @Autowired
    private NumberSeriesService numberSeriesService;

    @Autowired
    private NumberSeriesRepository numberSeriesRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long testCompanyId;

    @BeforeEach
    void setUp() {
        // Ensure test currency exists
        jdbcTemplate.update("INSERT IGNORE INTO currencies (code, name, symbol, decimal_places) VALUES ('AED', 'UAE Dirham', 'AED', 2)");

        // Ensure test company exists
        jdbcTemplate.update("INSERT IGNORE INTO companies (id, code, legal_name, country_code, base_currency) " +
                "VALUES (1001, 'TEST_CO', 'Test Company LLC', 'AE', 'AED')");

        testCompanyId = 1001L;
    }

    @Test
    @Transactional
    @DisplayName("Should generate gap-free document numbers scoped per company and doc type")
    void shouldGenerateGapFreeDocumentNumbers() {
        String currentYear = String.valueOf(Year.now().getValue());

        String inv1 = numberSeriesService.generateNextNumber(testCompanyId, DocType.INVOICE);
        assertEquals("INV-" + currentYear + "-0001", inv1);

        String inv2 = numberSeriesService.generateNextNumber(testCompanyId, DocType.INVOICE);
        assertEquals("INV-" + currentYear + "-0002", inv2);

        String jv1 = numberSeriesService.generateNextNumber(testCompanyId, DocType.JOURNAL);
        assertEquals("JV-" + currentYear + "-0001", jv1);
    }
}
