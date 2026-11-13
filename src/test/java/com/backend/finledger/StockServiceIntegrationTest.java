package com.backend.finledger;

import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.inventory.dto.ItemDto;
import com.backend.finledger.inventory.dto.ItemRequest;
import com.backend.finledger.inventory.dto.WarehouseDto;
import com.backend.finledger.inventory.dto.WarehouseRequest;
import com.backend.finledger.inventory.entity.*;
import com.backend.finledger.inventory.repository.StockBalanceRepository;
import com.backend.finledger.inventory.service.ItemService;
import com.backend.finledger.inventory.service.StockService;
import com.backend.finledger.inventory.service.WarehouseService;
import com.backend.finledger.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StockService Integration Tests (Concurrent Locking & Weighted-Average Costing)")
class StockServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private StockBalanceRepository stockBalanceRepository;

    @Autowired
    private ItemService itemService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long companyId;
    private WarehouseDto testWarehouse;

    @BeforeEach
    void setUp() {
        Company company = companyRepository.findByCode("BSB").orElseGet(() -> {
            Company c = new Company();
            c.setCode("BSB");
            c.setLegalName("Blue Sky Business LLC");
            c.setCountryCode("AE");
            c.setBaseCurrency("AED");
            c.setFiscalYearStartMonth((byte) 1);
            return companyRepository.save(c);
        });
        companyId = company.getId();

        UserPrincipal principal = new UserPrincipal(
                1L,
                companyId,
                "admin@finledger.com",
                "Password123!",
                "System Admin",
                true,
                Set.of("ITEM_VIEW", "ITEM_EDIT", "STOCK_ADJUST", "STOCK_TRANSFER", "REPORT_VIEW")
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        String whCode = "WH-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        testWarehouse = warehouseService.createWarehouse(new WarehouseRequest(
                whCode, "Locking Test Warehouse", "Industrial Zone", null, false, true
        ));
    }

    private ItemDto createInventoryItem(String prefix, BigDecimal salesPrice, BigDecimal purchasePrice) {
        String sku = prefix + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return itemService.createItem(new ItemRequest(
                sku, null, "Item " + sku, null, ItemType.INVENTORY, "PCS",
                CostingMethod.WEIGHTED_AVERAGE, salesPrice, purchasePrice,
                BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, null, null, true
        ));
    }

    @Test
    @DisplayName("1. Weighted-Average Costing: Sequential Receipts and Issues recalculate AvgCost accurately")
    void testWeightedAverageCostingFormula() {
        ItemDto item = createInventoryItem("WAC", new BigDecimal("100.00"), new BigDecimal("10.00"));

        // Step 1: Initial receipt of 100 units @ 10.00
        stockService.receiveStock(
                companyId, item.id(), testWarehouse.id(),
                new BigDecimal("100"), new BigDecimal("10.00"),
                MovementType.PURCHASE, StockSourceType.BILL, 101L, 1L, null
        );

        StockBalance b1 = stockBalanceRepository.findByItemIdAndWarehouseId(item.id(), testWarehouse.id()).orElseThrow();
        assertEquals(0, new BigDecimal("100").compareTo(b1.getQtyOnHand()));
        assertEquals(0, new BigDecimal("10.0000").compareTo(b1.getAvgCost()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(b1.getStockValue()));

        // Step 2: Second receipt of 50 units @ 16.00
        // Expected total value = 1000.00 + (50 * 16.00) = 1800.00
        // Expected new avg cost = 1800.00 / 150 = 12.0000
        stockService.receiveStock(
                companyId, item.id(), testWarehouse.id(),
                new BigDecimal("50"), new BigDecimal("16.00"),
                MovementType.PURCHASE, StockSourceType.BILL, 102L, 1L, null
        );

        StockBalance b2 = stockBalanceRepository.findByItemIdAndWarehouseId(item.id(), testWarehouse.id()).orElseThrow();
        assertEquals(0, new BigDecimal("150").compareTo(b2.getQtyOnHand()));
        assertEquals(0, new BigDecimal("12.0000").compareTo(b2.getAvgCost()));
        assertEquals(0, new BigDecimal("1800.00").compareTo(b2.getStockValue()));

        // Step 3: Issue 30 units (Issue uses current weighted avg cost = 12.00)
        // Balance qty becomes 120, avgCost remains 12.0000, value = 120 * 12.00 = 1440.00
        stockService.issueStock(
                companyId, item.id(), testWarehouse.id(),
                new BigDecimal("30"),
                MovementType.SALE, StockSourceType.INVOICE, 201L, 1L, null
        );

        StockBalance b3 = stockBalanceRepository.findByItemIdAndWarehouseId(item.id(), testWarehouse.id()).orElseThrow();
        assertEquals(0, new BigDecimal("120").compareTo(b3.getQtyOnHand()));
        assertEquals(0, new BigDecimal("12.0000").compareTo(b3.getAvgCost()));
        assertEquals(0, new BigDecimal("1440.00").compareTo(b3.getStockValue()));

        // Step 4: Third receipt of 80 units @ 20.00
        // Total new qty = 120 + 80 = 200
        // Total value = 1440.00 + (80 * 20.00 = 1600.00) = 3040.00
        // New avg cost = 3040.00 / 200 = 15.2000
        stockService.receiveStock(
                companyId, item.id(), testWarehouse.id(),
                new BigDecimal("80"), new BigDecimal("20.00"),
                MovementType.PURCHASE, StockSourceType.BILL, 103L, 1L, null
        );

        StockBalance b4 = stockBalanceRepository.findByItemIdAndWarehouseId(item.id(), testWarehouse.id()).orElseThrow();
        assertEquals(0, new BigDecimal("200").compareTo(b4.getQtyOnHand()));
        assertEquals(0, new BigDecimal("15.2000").compareTo(b4.getAvgCost()));
        assertEquals(0, new BigDecimal("3040.00").compareTo(b4.getStockValue()));
    }

    @Test
    @DisplayName("2. Pessimistic Write Locking: Concurrent threads receiving stock synchronize via SELECT FOR UPDATE")
    void testConcurrentStockLockingPessimisticWrite() throws Exception {
        ItemDto item = createInventoryItem("CONC", new BigDecimal("50.00"), new BigDecimal("25.00"));

        // Initialize stock balance with 10 units @ 25.00
        stockService.receiveStock(
                companyId, item.id(), testWarehouse.id(),
                new BigDecimal("10"), new BigDecimal("25.00"),
                MovementType.OPENING, StockSourceType.OPENING, 1L, 1L, null
        );

        int threadCount = 5;
        BigDecimal qtyPerThread = new BigDecimal("5");
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        List<Future<Boolean>> futures = new ArrayList<>();
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                try {
                    UserPrincipal principal = new UserPrincipal(1L, companyId, "admin@finledger.com", "", "Admin", true, Set.of("ITEM_EDIT", "STOCK_ADJUST"));
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
                    );

                    startLatch.await(); // await synchronized blast

                    txTemplate.execute(status -> {
                        stockService.receiveStock(
                                companyId, item.id(), testWarehouse.id(),
                                qtyPerThread, new BigDecimal("25.00"),
                                MovementType.PURCHASE, StockSourceType.BILL, 500L + index, 1L, null
                        );
                        return true;
                    });
                    return true;
                } finally {
                    doneLatch.countDown();
                }
            }));
        }

        // Trigger all threads simultaneously
        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "Concurrent threads should complete within 30 seconds");

        for (Future<Boolean> f : futures) {
            assertTrue(f.get());
        }
        executor.shutdown();

        // Initial: 10 + (5 * 5) = 35 total on hand
        StockBalance finalBalance = stockBalanceRepository.findByItemIdAndWarehouseId(item.id(), testWarehouse.id()).orElseThrow();
        assertEquals(0, new BigDecimal("35").compareTo(finalBalance.getQtyOnHand()),
                "Pessimistic locking must guarantee exact total without lost updates");
    }

    @Test
    @DisplayName("Should allow or block negative stock according to Company allowNegativeStock flag")
    void testAllowNegativeStockPolicy() {
        ItemDto item = createInventoryItem("NEG", new BigDecimal("100.00"), new BigDecimal("40.00"));
        stockService.receiveStock(
                companyId, item.id(), testWarehouse.id(),
                new BigDecimal("2"), new BigDecimal("40.00"),
                MovementType.PURCHASE, StockSourceType.BILL, 701L, 1L, null
        );

        Company company = companyRepository.findById(companyId).orElseThrow();
        boolean originalFlag = Boolean.TRUE.equals(company.getAllowNegativeStock());
        try {
            company.setAllowNegativeStock(false);
            companyRepository.saveAndFlush(company);

            assertThrows(IllegalStateException.class, () -> stockService.issueStock(
                    companyId, item.id(), testWarehouse.id(),
                    new BigDecimal("5"), MovementType.SALE, StockSourceType.INVOICE, 702L, 1L, null
            ));

            company.setAllowNegativeStock(true);
            companyRepository.saveAndFlush(company);

            stockService.issueStock(
                    companyId, item.id(), testWarehouse.id(),
                    new BigDecimal("5"), MovementType.SALE, StockSourceType.INVOICE, 703L, 1L, null
            );

            StockBalance balance = stockBalanceRepository.findByItemIdAndWarehouseId(item.id(), testWarehouse.id()).orElseThrow();
            assertEquals(0, new BigDecimal("-3").compareTo(balance.getQtyOnHand()));
        } finally {
            company.setAllowNegativeStock(originalFlag);
            companyRepository.saveAndFlush(company);
        }
    }
}
