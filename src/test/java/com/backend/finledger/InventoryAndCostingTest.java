package com.backend.finledger;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.DocType;
import com.backend.finledger.common.NumberSeries;
import com.backend.finledger.common.NumberSeriesRepository;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.inventory.dto.*;
import com.backend.finledger.inventory.entity.*;
import com.backend.finledger.inventory.repository.*;
import com.backend.finledger.inventory.service.*;
import com.backend.finledger.ledger.dto.CreateFiscalYearRequest;
import com.backend.finledger.ledger.dto.FiscalYearDto;
import com.backend.finledger.ledger.entity.Account;
import com.backend.finledger.ledger.entity.AccountType;
import com.backend.finledger.ledger.entity.JournalEntry;
import com.backend.finledger.ledger.entity.NormalBalance;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.ledger.repository.FiscalYearRepository;
import com.backend.finledger.ledger.repository.JournalEntryRepository;
import com.backend.finledger.ledger.service.FiscalYearPeriodService;
import com.backend.finledger.purchases.dto.BillDto;
import com.backend.finledger.purchases.dto.BillLineRequest;
import com.backend.finledger.purchases.dto.BillRequest;
import com.backend.finledger.purchases.dto.VendorDto;
import com.backend.finledger.purchases.dto.VendorRequest;
import com.backend.finledger.purchases.entity.BillDocType;
import com.backend.finledger.purchases.service.BillService;
import com.backend.finledger.purchases.service.VendorService;
import com.backend.finledger.sales.dto.CustomerDto;
import com.backend.finledger.sales.dto.CustomerRequest;
import com.backend.finledger.sales.dto.InvoiceDto;
import com.backend.finledger.sales.dto.InvoiceLineRequest;
import com.backend.finledger.sales.dto.InvoiceRequest;
import com.backend.finledger.sales.entity.InvoiceDocType;
import com.backend.finledger.sales.service.CustomerService;
import com.backend.finledger.sales.service.InvoiceService;
import com.backend.finledger.security.UserPrincipal;
import com.backend.finledger.tax.entity.TaxRate;
import com.backend.finledger.tax.entity.TaxType;
import com.backend.finledger.tax.repository.TaxRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class InventoryAndCostingTest {

    @Autowired
    private ItemCategoryService categoryService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private CostingService costingService;

    @Autowired
    private StockService stockService;

    @Autowired
    private StockAdjustmentService stockAdjustmentService;

    @Autowired
    private StockTransferService stockTransferService;

    @Autowired
    private InventoryReportService reportService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TaxRateRepository taxRateRepository;

    @Autowired
    private FiscalYearRepository fiscalYearRepository;

    @Autowired
    private FiscalYearPeriodService fiscalYearPeriodService;

    @Autowired
    private NumberSeriesRepository numberSeriesRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private StockBalanceRepository stockBalanceRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private VendorService vendorService;

    @Autowired
    private BillService billService;

    private Long bsbCompanyId;
    private Long inventoryAssetAccountId;
    private Long cogsAccountId;
    private Long salesRevenueAccountId;
    private Long inventoryAdjustmentAccountId;
    private Long apAccountId;
    private Long arAccountId;
    private Long vatInputAccountId;
    private Long vatOutputAccountId;
    private Long taxRateZeroId;

    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();

        Company bsb = companyRepository.findByCode("BSB").orElseThrow();
        bsbCompanyId = bsb.getId();

        authenticateAs(1L, bsbCompanyId, "admin@finledger.com", Set.of(
                "ITEM_VIEW", "ITEM_EDIT", "STOCK_ADJUST", "STOCK_TRANSFER",
                "CUSTOMER_VIEW", "CUSTOMER_CREATE", "CUSTOMER_EDIT",
                "INVOICE_VIEW", "INVOICE_CREATE", "INVOICE_SEND", "INVOICE_VOID",
                "VENDOR_VIEW", "VENDOR_CREATE", "VENDOR_EDIT",
                "BILL_VIEW", "BILL_CREATE", "BILL_APPROVE", "BILL_VOID",
                "JOURNAL_VIEW", "JOURNAL_CREATE", "JOURNAL_POST", "JOURNAL_VOID",
                "REPORT_VIEW", "SETTINGS_EDIT", "ACCOUNT_VIEW", "ACCOUNT_EDIT"
        ));

        // 1. Fiscal Year & Periods
        int currentYear = today.getYear();
        String fyName = "FY" + currentYear;
        fiscalYearRepository.findByCompanyIdAndName(bsbCompanyId, fyName).orElseGet(() -> {
            CreateFiscalYearRequest fyReq = new CreateFiscalYearRequest(
                    fyName,
                    LocalDate.of(currentYear, 1, 1),
                    LocalDate.of(currentYear, 12, 31),
                    true
            );
            FiscalYearDto dto = fiscalYearPeriodService.createFiscalYear(fyReq);
            return fiscalYearRepository.findById(dto.id()).orElseThrow();
        });

        // 2. Setup GL Accounts for Inventory, COGS, Adjustment, Sales, AP, AR, VAT
        Account invAsset = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "1300").orElseGet(() -> {
            Account a = new Account(bsbCompanyId, "1300", "Merchandise Inventory", AccountType.ASSET, "INVENTORY", null, false, NormalBalance.DEBIT);
            return accountRepository.save(a);
        });
        inventoryAssetAccountId = invAsset.getId();

        Account cogsAcc = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "5100").orElseGet(() -> {
            Account a = new Account(bsbCompanyId, "5100", "Cost of Goods Sold", AccountType.EXPENSE, "COGS", null, false, NormalBalance.DEBIT);
            return accountRepository.save(a);
        });
        cogsAccountId = cogsAcc.getId();

        Account revAcc = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "4100").orElseGet(() -> {
            Account a = new Account(bsbCompanyId, "4100", "Sales Revenue", AccountType.INCOME, "REVENUE", null, false, NormalBalance.CREDIT);
            return accountRepository.save(a);
        });
        salesRevenueAccountId = revAcc.getId();

        Account adjAcc = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "5150").orElseGet(() -> {
            Account a = new Account(bsbCompanyId, "5150", "Inventory Adjustments", AccountType.EXPENSE, "INVENTORY_ADJ", null, false, NormalBalance.DEBIT);
            return accountRepository.save(a);
        });
        inventoryAdjustmentAccountId = adjAcc.getId();

        Account apAcc = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "2100").orElseGet(() -> {
            Account a = new Account(bsbCompanyId, "2100", "Accounts Payable", AccountType.LIABILITY, "AP", null, false, NormalBalance.CREDIT);
            return accountRepository.save(a);
        });
        apAccountId = apAcc.getId();

        Account arAcc = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "1200").orElseGet(() -> {
            Account a = new Account(bsbCompanyId, "1200", "Accounts Receivable", AccountType.ASSET, "AR", null, false, NormalBalance.DEBIT);
            return accountRepository.save(a);
        });
        arAccountId = arAcc.getId();

        Account vatIn = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "1150").orElseGet(() -> {
            Account a = new Account(bsbCompanyId, "1150", "VAT Input", AccountType.ASSET, "TAX", null, false, NormalBalance.DEBIT);
            return accountRepository.save(a);
        });
        vatInputAccountId = vatIn.getId();

        Account vatOut = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "2150").orElseGet(() -> {
            Account a = new Account(bsbCompanyId, "2150", "VAT Output", AccountType.LIABILITY, "TAX", null, false, NormalBalance.CREDIT);
            return accountRepository.save(a);
        });
        vatOutputAccountId = vatOut.getId();

        // 3. Tax Rates
        TaxRate zeroTax = taxRateRepository.findByCompanyIdAndCode(bsbCompanyId, "VAT-0").orElseGet(() -> {
            TaxRate tr = new TaxRate(bsbCompanyId, "VAT-0", "Zero Rate VAT", BigDecimal.ZERO, TaxType.ZERO);
            tr.setSalesAccountId(vatOutputAccountId);
            tr.setPurchaseAccountId(vatInputAccountId);
            return taxRateRepository.save(tr);
        });
        taxRateZeroId = zeroTax.getId();

        // 4. Number series for adjustments, transfers, bills, invoices, journals
        ensureNumberSeries(bsbCompanyId, DocType.ADJUSTMENT, "ADJ-", 1);
        ensureNumberSeries(bsbCompanyId, DocType.TRANSFER, "TRF-", 1);
        ensureNumberSeries(bsbCompanyId, DocType.INVOICE, "INV-", 1);
        ensureNumberSeries(bsbCompanyId, DocType.BILL, "BILL-", 1);
        ensureNumberSeries(bsbCompanyId, DocType.JOURNAL, "JV-{YYYY}-", 1);
    }

    private void ensureNumberSeries(Long companyId, DocType docType, String prefix, Integer nextVal) {
        var opt = numberSeriesRepository.findByCompanyIdAndDocType(companyId, docType);
        if (opt.isEmpty()) {
            NumberSeries ns = new NumberSeries();
            ns.setCompanyId(companyId);
            ns.setDocType(docType);
            ns.setPrefix(prefix);
            ns.setNextNumber(nextVal);
            ns.setPadding((byte) 4);
            numberSeriesRepository.save(ns);
        } else {
            NumberSeries ns = opt.get();
            if (!prefix.equals(ns.getPrefix())) {
                ns.setPrefix(prefix);
                numberSeriesRepository.save(ns);
            }
        }
    }

    private void authenticateAs(Long userId, Long companyId, String email, Set<String> permissions) {
        UserPrincipal principal = new UserPrincipal(userId, companyId, email, "", "Test User", true, permissions);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private WarehouseDto createTestWarehouse(String code, String name, boolean isDefault) {
        return warehouseService.createWarehouse(new WarehouseRequest(
                code, name, "Dubai Industrial Area", null, isDefault, true
        ));
    }

    private ItemDto createTestItem(String sku, String name, BigDecimal salesPrice, BigDecimal purchasePrice, BigDecimal reorderLevel, BigDecimal reorderQty) {
        return itemService.createItem(new ItemRequest(
                sku, null, name, null, ItemType.INVENTORY, "PCS",
                CostingMethod.WEIGHTED_AVERAGE, salesPrice, purchasePrice,
                reorderLevel, reorderQty, salesRevenueAccountId, cogsAccountId,
                inventoryAssetAccountId, taxRateZeroId, null, true
        ));
    }

    @Test
    @Transactional
    @DisplayName("1. Master Data: ItemCategory and Warehouse CRUD operations")
    void testItemCategoryAndWarehouseCrud() {
        // Create category
        ItemCategoryRequest catReq = new ItemCategoryRequest(
                "Electronics & Gadgets",
                null,
                salesRevenueAccountId,
                inventoryAssetAccountId,
                cogsAccountId
        );
        ItemCategoryDto cat = categoryService.createCategory(catReq);
        assertNotNull(cat.id());
        assertEquals("Electronics & Gadgets", cat.name());
        assertEquals(inventoryAssetAccountId, cat.inventoryAccountId());

        // Create warehouse
        WarehouseDto wh = createTestWarehouse("WH-DXB-01", "Dubai Central Warehouse", true);
        assertNotNull(wh.id());
        assertEquals("WH-DXB-01", wh.code());
        assertTrue(wh.isDefault());

        // Fetch warehouse by id
        WarehouseDto fetchedWh = warehouseService.getWarehouseById(wh.id());
        assertEquals("Dubai Central Warehouse", fetchedWh.name());
    }

    @Test
    @Transactional
    @DisplayName("2. Master Data: Item CRUD with default costing method and account inheritance")
    void testItemCrudWithDefaults() {
        ItemRequest itemReq = new ItemRequest(
                "SKU-TEST-LAPTOP",
                "890123456789",
                "Gaming Laptop Pro 15",
                null,
                ItemType.INVENTORY,
                "PCS",
                CostingMethod.WEIGHTED_AVERAGE,
                new BigDecimal("4500.00"),
                new BigDecimal("3500.00"),
                new BigDecimal("5.00"),
                new BigDecimal("10.00"),
                salesRevenueAccountId,
                cogsAccountId,
                inventoryAssetAccountId,
                taxRateZeroId,
                null,
                true
        );

        ItemDto created = itemService.createItem(itemReq);
        assertNotNull(created.id());
        assertEquals("SKU-TEST-LAPTOP", created.sku());
        assertEquals(CostingMethod.WEIGHTED_AVERAGE, created.costingMethod());
        assertEquals(ItemType.INVENTORY, created.itemType());

        // Update item price
        ItemRequest updateReq = new ItemRequest(
                "SKU-TEST-LAPTOP",
                "890123456789",
                "Gaming Laptop Pro 15 - Updated",
                null,
                ItemType.INVENTORY,
                "PCS",
                CostingMethod.WEIGHTED_AVERAGE,
                new BigDecimal("4800.00"),
                new BigDecimal("3600.00"),
                new BigDecimal("5.00"),
                new BigDecimal("10.00"),
                salesRevenueAccountId,
                cogsAccountId,
                inventoryAssetAccountId,
                taxRateZeroId,
                null,
                true
        );
        ItemDto updated = itemService.updateItem(created.id(), updateReq);
        assertEquals(new BigDecimal("4800.00"), updated.salesPrice());
    }

    @Test
    @DisplayName("3. Costing Engine: Weighted-average unit cost formula verification")
    void testCostingEngineWeightedAverage() {
        // Initial receipt: 0 stock, receive 100 @ 10.00 -> avg = 10.0000
        BigDecimal firstAvg = costingService.calculateWeightedAverageCost(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("100"),
                new BigDecimal("10.00")
        );
        assertEquals(new BigDecimal("10.0000"), firstAvg);

        // Next receipt: 100 @ 10.00 (val 1000) + 50 @ 13.00 (val 650) -> total 1650 / 150 = 11.0000
        BigDecimal secondAvg = costingService.calculateWeightedAverageCost(
                new BigDecimal("100"),
                firstAvg,
                new BigDecimal("50"),
                new BigDecimal("13.00")
        );
        assertEquals(new BigDecimal("11.0000"), secondAvg);

        // Next receipt: 150 @ 11.00 (val 1650) + 50 @ 15.00 (val 750) -> total 2400 / 200 = 12.0000
        BigDecimal thirdAvg = costingService.calculateWeightedAverageCost(
                new BigDecimal("150"),
                secondAvg,
                new BigDecimal("50"),
                new BigDecimal("15.00")
        );
        assertEquals(new BigDecimal("12.0000"), thirdAvg);
    }

    @Test
    @Transactional
    @DisplayName("4. StockService: Stock receive, issue, and negative stock blocking")
    void testStockServiceReceiveIssueAndNegativeStockBlocking() {
        WarehouseDto wh = createTestWarehouse("WH-MAIN", "Main WH", true);
        ItemDto item = createTestItem("SKU-STOCK-01", "Stock Widget A", new BigDecimal("50.00"), new BigDecimal("30.00"), BigDecimal.ZERO, BigDecimal.ZERO);

        // 1. Receive stock: 100 units @ 30.00
        StockMovement rcv = stockService.receiveStock(
                bsbCompanyId, item.id(), wh.id(), new BigDecimal("100"), new BigDecimal("30.00"),
                MovementType.PURCHASE, StockSourceType.BILL, 1L, 1L, null
        );
        assertNotNull(rcv.getId());
        assertEquals(0, new BigDecimal("100").compareTo(rcv.getQuantity()));

        StockBalance balance = stockBalanceRepository.findByItemIdAndWarehouseId(item.id(), wh.id()).orElseThrow();
        assertEquals(0, new BigDecimal("100").compareTo(balance.getQtyOnHand()));
        assertEquals(0, new BigDecimal("30.0000").compareTo(balance.getAvgCost()));
        assertEquals(0, new BigDecimal("3000.00").compareTo(balance.getStockValue()));

        // 2. Issue stock: 40 units
        StockMovement issue = stockService.issueStock(
                bsbCompanyId, item.id(), wh.id(), new BigDecimal("40"),
                MovementType.SALE, StockSourceType.INVOICE, 2L, 1L, null
        );
        assertEquals(0, new BigDecimal("-40").compareTo(issue.getQuantity()));

        balance = stockBalanceRepository.findByItemIdAndWarehouseId(item.id(), wh.id()).orElseThrow();
        assertEquals(0, new BigDecimal("60").compareTo(balance.getQtyOnHand()));
        assertEquals(0, new BigDecimal("1800.00").compareTo(balance.getStockValue()));

        // 3. Issue stock exceeding balance (70 > 60) -> must throw INSUFFICIENT_STOCK
        BusinessException ex = assertThrows(BusinessException.class, () ->
                stockService.issueStock(
                        bsbCompanyId, item.id(), wh.id(), new BigDecimal("70"),
                        MovementType.SALE, StockSourceType.INVOICE, 3L, 1L, null
                ));
        assertEquals("INSUFFICIENT_STOCK", ex.getCode());
    }

    @Test
    @Transactional
    @DisplayName("5. Vendor Bill Approval: receives stock and updates weighted-average cost")
    void testVendorBillApprovalReceivesStockAndUpdateCost() {
        WarehouseDto wh = createTestWarehouse("WH-BILL", "Purchases WH", true);
        ItemDto item = createTestItem("SKU-BILL-ITEM", "Purchase Item X", new BigDecimal("100.00"), new BigDecimal("60.00"), BigDecimal.ZERO, BigDecimal.ZERO);

        VendorDto vendor = vendorService.createVendor(new VendorRequest(
                "VEND-INV-01", "Supplier Inventory LLC", "100200300400003",
                "vendor@supplier.ae", "+97141234567", "Dubai", "AED",
                (short) 30, "AE123456789012345678901", apAccountId, true
        ));

        // Bill 1: 50 units @ 40.00 AED
        BillDto bill1 = billService.createBill(new BillRequest(
                BillDocType.BILL, "INV-VEND-001", vendor.id(), today, today.plusDays(30), "AED", BigDecimal.ONE, "First purchase",
                List.of(new BillLineRequest((short) 1, item.id(), inventoryAssetAccountId, wh.id(), "First batch",
                        new BigDecimal("50"), new BigDecimal("40.00"), taxRateZeroId, null))
        ));
        billService.approveBill(bill1.id());

        StockBalance b1 = stockBalanceRepository.findByItemIdAndWarehouseId(item.id(), wh.id()).orElseThrow();
        assertEquals(0, new BigDecimal("50").compareTo(b1.getQtyOnHand()));
        assertEquals(0, new BigDecimal("40.0000").compareTo(b1.getAvgCost()));

        // Bill 2: 50 units @ 60.00 AED
        BillDto bill2 = billService.createBill(new BillRequest(
                BillDocType.BILL, "INV-VEND-002", vendor.id(), today, today.plusDays(30), "AED", BigDecimal.ONE, "Second purchase",
                List.of(new BillLineRequest((short) 1, item.id(), inventoryAssetAccountId, wh.id(), "Second batch",
                        new BigDecimal("50"), new BigDecimal("60.00"), taxRateZeroId, null))
        ));
        billService.approveBill(bill2.id());

        // Balance: 100 units, avg cost = (50*40 + 50*60)/100 = 50.0000, value = 5000.00
        StockBalance b2 = stockBalanceRepository.findByItemIdAndWarehouseId(item.id(), wh.id()).orElseThrow();
        assertEquals(0, new BigDecimal("100").compareTo(b2.getQtyOnHand()));
        assertEquals(0, new BigDecimal("50.0000").compareTo(b2.getAvgCost()));
        assertEquals(0, new BigDecimal("5000.00").compareTo(b2.getStockValue()));
    }

    @Test
    @Transactional
    @DisplayName("6. Sales Invoice: sending deducts stock and generates balanced COGS lines in GL")
    void testSalesInvoiceDeductsStockAndPostsCogs() {
        WarehouseDto wh = createTestWarehouse("WH-SALE", "Sales WH", true);
        ItemDto item = createTestItem("SKU-SALE-ITEM", "Sale Inventory Y", new BigDecimal("150.00"), new BigDecimal("80.00"), BigDecimal.ZERO, BigDecimal.ZERO);

        // Initial stock: 100 units @ 80.00 AED
        stockService.receiveStock(
                bsbCompanyId, item.id(), wh.id(), new BigDecimal("100"), new BigDecimal("80.00"),
                MovementType.OPENING, StockSourceType.ADJUSTMENT, 1L, null, null
        );

        CustomerDto customer = customerService.createCustomer(new CustomerRequest(
                "CUST-INV-01", "Acme Retailers", "100200300400003", "retail@acme.ae", "+97149998888",
                "Business Bay, Dubai", "AE", "AED", (short) 30, new BigDecimal("100000.00"),
                arAccountId, null, null, true
        ));

        // Invoice: 20 units @ 150.00 AED
        InvoiceDto draft = invoiceService.createInvoice(new InvoiceRequest(
                InvoiceDocType.INVOICE, customer.id(), today, today.plusDays(30), "AED", BigDecimal.ONE, "Retail Sale",
                null, null,
                List.of(new InvoiceLineRequest((short) 1, item.id(), "Inventory Sale",
                        new BigDecimal("20"), new BigDecimal("150.00"), BigDecimal.ZERO, taxRateZeroId,
                        salesRevenueAccountId, wh.id(), null))
        ));

        InvoiceDto sent = invoiceService.sendInvoice(draft.id());
        assertNotNull(sent.journalEntryId());

        // Verify stock deducted to 80 units
        StockBalance balance = stockBalanceRepository.findByItemIdAndWarehouseId(item.id(), wh.id()).orElseThrow();
        assertEquals(0, new BigDecimal("80").compareTo(balance.getQtyOnHand()));

        // Verify Journal Entry contains COGS Dr and Inventory Cr lines for 20 * 80 = 1600.00 AED
        JournalEntry journal = journalEntryRepository.findById(sent.journalEntryId()).orElseThrow();
        assertEquals(0, journal.getTotalDebit().compareTo(journal.getTotalCredit()), "Journal must be balanced");

        boolean hasCogsDebit = journal.getLines().stream().anyMatch(line ->
                line.getAccountId().equals(cogsAccountId) &&
                line.getDebit().compareTo(new BigDecimal("1600.00")) == 0
        );
        boolean hasInventoryCredit = journal.getLines().stream().anyMatch(line ->
                line.getAccountId().equals(inventoryAssetAccountId) &&
                line.getCredit().compareTo(new BigDecimal("1600.00")) == 0
        );

        assertTrue(hasCogsDebit, "Invoice GL entry must include Dr COGS for 1600.00");
        assertTrue(hasInventoryCredit, "Invoice GL entry must include Cr Inventory for 1600.00");
    }

    @Test
    @Transactional
    @DisplayName("7. Stock Adjustment: shortage and surplus with balanced GL entries")
    void testStockAdjustmentShortageAndSurplusWithGl() {
        WarehouseDto wh = createTestWarehouse("WH-ADJ", "Adjustment WH", false);
        ItemDto item = createTestItem("SKU-ADJ-ITEM", "Adjustment Item Z", new BigDecimal("100.00"), new BigDecimal("50.00"), BigDecimal.ZERO, BigDecimal.ZERO);

        // Initial stock: 100 units @ 50.00
        stockService.receiveStock(
                bsbCompanyId, item.id(), wh.id(), new BigDecimal("100"), new BigDecimal("50.00"),
                MovementType.PURCHASE, StockSourceType.BILL, 1L, 1L, null
        );

        // Adjustment 1: Physical count found 90 units (Shortage of 10 units @ 50.00 = 500.00 AED)
        StockAdjustmentDto shortageAdj = stockAdjustmentService.createDraftAdjustment(new StockAdjustmentRequest(
                today, wh.id(), AdjustmentReason.DAMAGED, inventoryAdjustmentAccountId, "Damaged stock found",
                List.of(new StockAdjustmentLineRequest(item.id(), new BigDecimal("90"), new BigDecimal("50.00")))
        ));
        StockAdjustmentDto postedShortage = stockAdjustmentService.postAdjustment(shortageAdj.id());
        assertNotNull(postedShortage.journalEntryId());

        StockBalance afterShortage = stockBalanceRepository.findByItemIdAndWarehouseId(item.id(), wh.id()).orElseThrow();
        assertEquals(0, new BigDecimal("90").compareTo(afterShortage.getQtyOnHand()));

        JournalEntry jShortage = journalEntryRepository.findById(postedShortage.journalEntryId()).orElseThrow();
        assertEquals(0, jShortage.getTotalDebit().compareTo(jShortage.getTotalCredit()));
        assertEquals(0, new BigDecimal("500.00").compareTo(jShortage.getTotalDebit()));

        // Adjustment 2: Physical count found 95 units (Surplus of 5 units @ 50.00 = 250.00 AED)
        StockAdjustmentDto surplusAdj = stockAdjustmentService.createDraftAdjustment(new StockAdjustmentRequest(
                today, wh.id(), AdjustmentReason.STOCK_COUNT, inventoryAdjustmentAccountId, "Stock recount found surplus",
                List.of(new StockAdjustmentLineRequest(item.id(), new BigDecimal("95"), new BigDecimal("50.00")))
        ));
        StockAdjustmentDto postedSurplus = stockAdjustmentService.postAdjustment(surplusAdj.id());
        assertNotNull(postedSurplus.journalEntryId());

        StockBalance afterSurplus = stockBalanceRepository.findByItemIdAndWarehouseId(item.id(), wh.id()).orElseThrow();
        assertEquals(0, new BigDecimal("95").compareTo(afterSurplus.getQtyOnHand()));

        JournalEntry jSurplus = journalEntryRepository.findById(postedSurplus.journalEntryId()).orElseThrow();
        assertEquals(0, jSurplus.getTotalDebit().compareTo(jSurplus.getTotalCredit()));
        assertEquals(0, new BigDecimal("250.00").compareTo(jSurplus.getTotalDebit()));
    }

    @Test
    @Transactional
    @DisplayName("8. Stock Transfer: dispatch from source and receive into destination warehouse")
    void testStockTransferDispatchAndReceive() {
        WarehouseDto whSrc = createTestWarehouse("WH-SRC", "Source Warehouse", false);
        WarehouseDto whDst = createTestWarehouse("WH-DST", "Destination Warehouse", false);

        ItemDto item = createTestItem("SKU-TRF-01", "Transfer Item T", new BigDecimal("100.00"), new BigDecimal("25.00"), BigDecimal.ZERO, BigDecimal.ZERO);

        // Initial stock in source warehouse: 50 units @ 25.00
        stockService.receiveStock(
                bsbCompanyId, item.id(), whSrc.id(), new BigDecimal("50"), new BigDecimal("25.00"),
                MovementType.PURCHASE, StockSourceType.BILL, 1L, 1L, null
        );

        // 1. Create draft transfer of 20 units
        StockTransferDto draft = stockTransferService.createDraftTransfer(new StockTransferRequest(
                today, whSrc.id(), whDst.id(), "Inter-warehouse transfer",
                List.of(new StockTransferLineRequest(item.id(), new BigDecimal("20")))
        ));
        assertEquals(TransferStatus.DRAFT, draft.status());

        // 2. Dispatch transfer: issues 20 from WH-SRC
        StockTransferDto dispatched = stockTransferService.dispatchTransfer(draft.id());
        assertEquals(TransferStatus.IN_TRANSIT, dispatched.status());

        StockBalance srcBal = stockBalanceRepository.findByItemIdAndWarehouseId(item.id(), whSrc.id()).orElseThrow();
        assertEquals(0, new BigDecimal("30").compareTo(srcBal.getQtyOnHand()));

        // 3. Receive transfer: receives 20 into WH-DST
        StockTransferDto received = stockTransferService.receiveTransfer(draft.id());
        assertEquals(TransferStatus.RECEIVED, received.status());

        StockBalance dstBal = stockBalanceRepository.findByItemIdAndWarehouseId(item.id(), whDst.id()).orElseThrow();
        assertEquals(0, new BigDecimal("20").compareTo(dstBal.getQtyOnHand()));
        assertEquals(0, new BigDecimal("25.0000").compareTo(dstBal.getAvgCost()));
    }

    @Test
    @Transactional
    @DisplayName("9. Inventory Reports: stock summary, movements ledger, and reorder suggestions")
    void testInventoryReports() {
        WarehouseDto wh = createTestWarehouse("WH-RPT", "Report Warehouse", false);
        ItemDto item = createTestItem("SKU-RPT-LOW", "Low Stock Hardware", new BigDecimal("200.00"), new BigDecimal("120.00"), new BigDecimal("15.00"), new BigDecimal("20.00"));

        // Receive 10 units (reorder level is 15, so 10 <= 15 -> LOW_STOCK)
        stockService.receiveStock(
                bsbCompanyId, item.id(), wh.id(), new BigDecimal("10"), new BigDecimal("120.00"),
                MovementType.PURCHASE, StockSourceType.BILL, 1L, 1L, null
        );

        // Stock Summary report
        List<StockSummaryDto> summary = reportService.getStockSummary(null, wh.id(), null);
        assertFalse(summary.isEmpty());
        StockSummaryDto itemSummary = summary.stream()
                .filter(s -> s.itemId().equals(item.id()))
                .findFirst()
                .orElseThrow();
        assertEquals("LOW_STOCK", itemSummary.status());
        assertEquals(0, new BigDecimal("10").compareTo(itemSummary.totalOnHand()));

        // Movements ledger
        List<StockMovementDto> movements = reportService.getItemMovements(item.id(), wh.id(), null, null);
        assertEquals(1, movements.size());
        assertEquals(MovementType.PURCHASE, movements.get(0).movementType());
        assertEquals(0, new BigDecimal("10.0000").compareTo(movements.get(0).quantity()));

        // Reorder suggestions
        List<ReorderSuggestionDto> suggestions = reportService.getReorderSuggestions();
        assertFalse(suggestions.isEmpty());
        ReorderSuggestionDto suggestion = suggestions.stream()
                .filter(s -> s.itemId().equals(item.id()))
                .findFirst()
                .orElseThrow();
        assertEquals(0, new BigDecimal("20.00").compareTo(suggestion.suggestedOrderQty()));
        assertEquals(0, new BigDecimal("2400.00").compareTo(suggestion.estimatedCost())); // 20 * 120.00 = 2400.00
    }
}
