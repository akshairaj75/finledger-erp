package com.backend.finledger;

import com.backend.finledger.audit.AuditEntityListener;
import com.backend.finledger.banking.entity.BankAccount;
import com.backend.finledger.banking.repository.BankAccountRepository;
import com.backend.finledger.common.ApplicationContextProvider;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.DocType;
import com.backend.finledger.common.ForbiddenException;
import com.backend.finledger.common.NumberSeriesService;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.currency.repository.ExchangeRateRepository;
import com.backend.finledger.inventory.entity.Item;
import com.backend.finledger.inventory.entity.ItemType;
import com.backend.finledger.inventory.entity.MovementType;
import com.backend.finledger.inventory.entity.StockBalance;
import com.backend.finledger.inventory.entity.StockMovement;
import com.backend.finledger.inventory.entity.StockSourceType;
import com.backend.finledger.inventory.entity.Warehouse;
import com.backend.finledger.inventory.repository.ItemRepository;
import com.backend.finledger.inventory.repository.StockBalanceRepository;
import com.backend.finledger.inventory.repository.StockMovementRepository;
import com.backend.finledger.inventory.repository.WarehouseRepository;
import com.backend.finledger.inventory.service.CostingService;
import com.backend.finledger.inventory.service.StockService;
import com.backend.finledger.inventory.service.impl.StockServiceImpl;
import com.backend.finledger.ledger.dto.AccountRequest;
import com.backend.finledger.ledger.dto.JournalEntryDto;
import com.backend.finledger.ledger.dto.JournalLineRequest;
import com.backend.finledger.ledger.dto.SourceDocumentPostingRequest;
import com.backend.finledger.ledger.entity.*;
import com.backend.finledger.ledger.mapper.AccountMapper;
import com.backend.finledger.ledger.mapper.LedgerMapper;
import com.backend.finledger.ledger.repository.*;
import com.backend.finledger.ledger.service.PostingService;
import com.backend.finledger.ledger.service.impl.AccountServiceImpl;
import com.backend.finledger.ledger.service.impl.PostingServiceImpl;
import com.backend.finledger.purchases.dto.BillDto;
import com.backend.finledger.purchases.dto.PaymentAllocationRequest;
import com.backend.finledger.purchases.dto.PaymentRequest;
import com.backend.finledger.purchases.entity.Bill;
import com.backend.finledger.purchases.entity.BillDocType;
import com.backend.finledger.purchases.entity.BillLine;
import com.backend.finledger.purchases.entity.BillStatus;
import com.backend.finledger.purchases.entity.Payment;
import com.backend.finledger.purchases.entity.PaymentStatus;
import com.backend.finledger.purchases.entity.Vendor;
import com.backend.finledger.purchases.mapper.PurchasesMapper;
import com.backend.finledger.purchases.repository.BillRepository;
import com.backend.finledger.purchases.repository.PaymentRepository;
import com.backend.finledger.purchases.repository.VendorRepository;
import com.backend.finledger.purchases.service.impl.BillServiceImpl;
import com.backend.finledger.purchases.service.impl.PaymentServiceImpl;
import com.backend.finledger.sales.dto.InvoiceDto;
import com.backend.finledger.sales.dto.ReceiptAllocationRequest;
import com.backend.finledger.sales.dto.ReceiptDto;
import com.backend.finledger.sales.dto.ReceiptRequest;
import com.backend.finledger.sales.entity.Customer;
import com.backend.finledger.sales.entity.Invoice;
import com.backend.finledger.sales.entity.InvoiceDocType;
import com.backend.finledger.sales.entity.InvoiceLine;
import com.backend.finledger.sales.entity.InvoiceStatus;
import com.backend.finledger.sales.entity.PaymentMethod;
import com.backend.finledger.sales.entity.Receipt;
import com.backend.finledger.sales.entity.ReceiptStatus;
import com.backend.finledger.sales.mapper.SalesMapper;
import com.backend.finledger.sales.repository.CustomerRepository;
import com.backend.finledger.sales.repository.InvoiceRepository;
import com.backend.finledger.sales.repository.ReceiptRepository;
import com.backend.finledger.sales.service.impl.InvoiceServiceImpl;
import com.backend.finledger.sales.service.impl.ReceiptServiceImpl;
import com.backend.finledger.security.CurrentUser;
import com.backend.finledger.security.UserPrincipal;
import com.backend.finledger.tax.repository.TaxRateRepository;
import com.backend.finledger.tax.service.TaxCalculator;
import com.backend.finledger.common.mail.MailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Batch 1 Security & Core Accounting Unit Tests")
class Batch1UnitTest {

    private MockedStatic<CurrentUser> mockedCurrentUser;

    @Mock private StockBalanceRepository stockBalanceRepository;
    @Mock private StockMovementRepository stockMovementRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private CostingService costingService;
    @Mock private CompanyRepository companyRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private AccountMapper accountMapper;
    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private JournalLineRepository journalLineRepository;
    @Mock private FiscalPeriodRepository fiscalPeriodRepository;
    @Mock private CostCenterRepository costCenterRepository;
    @Mock private NumberSeriesService numberSeriesService;
    @Mock private LedgerMapper ledgerMapper;

    @Mock private ReceiptRepository receiptRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private PostingService postingService;
    @Mock private SalesMapper salesMapper;
    @Mock private ExchangeRateRepository exchangeRateRepository;

    @Mock private PaymentRepository paymentRepository;
    @Mock private VendorRepository vendorRepository;
    @Mock private BillRepository billRepository;
    @Mock private PurchasesMapper purchasesMapper;

    @Mock private TaxRateRepository taxRateRepository;
    @Mock private TaxCalculator taxCalculator;
    @Mock private StockService stockService;
    @Mock private MailService mailService;

    @BeforeEach
    void setupAuth() {
        mockedCurrentUser = Mockito.mockStatic(CurrentUser.class);
        mockedCurrentUser.when(CurrentUser::getCompanyId).thenReturn(1L);
        mockedCurrentUser.when(CurrentUser::getUserId).thenReturn(10L);
        mockedCurrentUser.when(CurrentUser::getPrincipal).thenReturn(Optional.of(
                new UserPrincipal(10L, 1L, "user@test.com", "", "User", true, Set.of())
        ));
    }

    @AfterEach
    void tearDownAuth() {
        mockedCurrentUser.close();
    }

    @Test
    @DisplayName("StockServiceImpl blocks cross-tenant access to stock balances")
    void testStockBalanceTenantIsolation() {
        StockServiceImpl service = new StockServiceImpl(
                stockBalanceRepository, stockMovementRepository, itemRepository, warehouseRepository, costingService, companyRepository);

        when(itemRepository.findByCompanyIdAndId(1L, 99L)).thenReturn(Optional.empty());
        when(itemRepository.existsById(99L)).thenReturn(true);

        assertThrows(ForbiddenException.class, () -> service.getStockBalance(99L, 5L));
        assertThrows(ForbiddenException.class, () -> service.getStockBalancesByItem(99L));

        when(warehouseRepository.findByCompanyIdAndId(1L, 88L)).thenReturn(Optional.empty());
        when(warehouseRepository.existsById(88L)).thenReturn(true);
        assertThrows(ForbiddenException.class, () -> service.getStockBalancesByWarehouse(88L));
    }

    @Test
    @DisplayName("AuditEntityListener resolves child entity companyId from parent and skips (does not default to 1L) when unknown")
    void testAuditEntityListenerParentTenantResolutionAndNoDefaultOneFallback() throws Exception {
        AuditEntityListener listener = new AuditEntityListener();
        Method extractCompanyId = AuditEntityListener.class.getDeclaredMethod("extractCompanyId", Object.class);
        extractCompanyId.setAccessible(true);

        Invoice parentInv = new Invoice();
        parentInv.setCompanyId(42L);
        InvoiceLine invLine = new InvoiceLine();
        invLine.setInvoice(parentInv);
        assertEquals(42L, extractCompanyId.invoke(listener, invLine));

        Bill parentBill = new Bill();
        parentBill.setCompanyId(77L);
        BillLine billLine = new BillLine();
        billLine.setBill(parentBill);
        assertEquals(77L, extractCompanyId.invoke(listener, billLine));

        // Detached entity with no companyId and no active CurrentUser must return null, NEVER 1L
        mockedCurrentUser.when(CurrentUser::getPrincipal).thenReturn(Optional.empty());
        StockBalance orphan = new StockBalance(123L, 456L);
        try (MockedStatic<ApplicationContextProvider> ctxMock = Mockito.mockStatic(ApplicationContextProvider.class)) {
            ctxMock.when(() -> ApplicationContextProvider.getBean(JdbcTemplate.class)).thenReturn(null);
            assertNull(extractCompanyId.invoke(listener, orphan));
        }
    }

    @Test
    @DisplayName("AccountServiceImpl rejects group opening balance and openingBalance edits when OPENING journal exists")
    void testOpeningBalancePolicyGuards() {
        AccountServiceImpl service = new AccountServiceImpl(accountRepository, accountMapper, journalEntryRepository);

        when(accountRepository.findByCompanyIdAndCode(1L, "1000")).thenReturn(Optional.empty());
        AccountRequest groupReq = new AccountRequest(
                "1000", "Assets", AccountType.ASSET, "CURRENT_ASSET", null, true,
                NormalBalance.DEBIT, "AED", new BigDecimal("500.00"), true);
        BusinessException ex1 = assertThrows(BusinessException.class, () -> service.createAccount(groupReq));
        assertEquals("GROUP_ACCOUNT_OPENING_BALANCE", ex1.getCode());

        when(accountRepository.findByCompanyIdAndCode(1L, "1110")).thenReturn(Optional.empty());
        when(journalEntryRepository.existsByCompanyIdAndSourceTypeAndStatus(1L, JournalSourceType.OPENING, JournalStatus.POSTED))
                .thenReturn(true);
        AccountRequest leafReq = new AccountRequest(
                "1110", "Cash", AccountType.ASSET, "CURRENT_ASSET", null, false,
                NormalBalance.DEBIT, "AED", new BigDecimal("500.00"), true);
        BusinessException ex2 = assertThrows(BusinessException.class, () -> service.createAccount(leafReq));
        assertEquals("OPENING_JOURNAL_EXISTS", ex2.getCode());
    }

    @Test
    @DisplayName("PostingServiceImpl blocks reversing a journal entry with reconciled lines")
    void testReverseJournalEntryBlockedWhenReconciled() {
        PostingServiceImpl service = new PostingServiceImpl(
                journalEntryRepository, journalLineRepository, accountRepository, fiscalPeriodRepository,
                costCenterRepository, companyRepository, numberSeriesService, ledgerMapper);

        JournalLine reconciledLine = new JournalLine();
        reconciledLine.setId(1L);
        reconciledLine.setIsReconciled(true);

        JournalEntry je = new JournalEntry();
        je.setId(50L);
        je.setCompanyId(1L);
        je.setEntryNo("JE-0050");
        je.setStatus(JournalStatus.POSTED);
        je.addLine(reconciledLine);

        when(journalEntryRepository.findByCompanyIdAndId(1L, 50L)).thenReturn(Optional.of(je));
        when(journalEntryRepository.existsByCompanyIdAndReversalOfId(1L, 50L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.reverseJournalEntry(50L, "mistake"));
        assertEquals("JOURNAL_RECONCILED", ex.getCode());
    }

    @Test
    @DisplayName("ReceiptServiceImpl posts Realized FX Gain when USD invoice settles at higher rate and blocks voiding reconciled receipt")
    void testReceiptFxGainAndReconciledVoidGuard() {
        ReceiptServiceImpl service = new ReceiptServiceImpl(
                receiptRepository, customerRepository, invoiceRepository, bankAccountRepository,
                companyRepository, postingService, numberSeriesService, salesMapper,
                accountRepository, journalLineRepository, exchangeRateRepository);

        Customer cust = new Customer();
        cust.setId(10L);
        cust.setName("Acme US");
        cust.setCode("CUST-1");
        cust.setArAccountId(1200L);
        cust.setCurrencyCode("USD");

        BankAccount bank = new BankAccount();
        bank.setId(20L);
        bank.setName("USD Bank");
        bank.setGlAccountId(1110L);
        bank.setIsActive(true);

        Company company = new Company();
        company.setId(1L);
        company.setBaseCurrency("AED");

        Invoice inv = new Invoice();
        inv.setId(30L);
        inv.setCompanyId(1L);
        inv.setInvoiceNo("INV-30");
        inv.setCustomerId(10L);
        inv.setCurrencyCode("USD");
        inv.setExchangeRate(new BigDecimal("3.672500"));
        inv.setTotal(new BigDecimal("1000.00"));
        inv.setAmountPaid(BigDecimal.ZERO);
        inv.setBalanceDue(new BigDecimal("1000.00"));
        inv.setStatus(InvoiceStatus.SENT);

        Account fxGain = new Account();
        fxGain.setId(4900L);
        fxGain.setIsActive(true);
        fxGain.setIsGroup(false);

        when(customerRepository.findByCompanyIdAndId(1L, 10L)).thenReturn(Optional.of(cust));
        when(bankAccountRepository.findByCompanyIdAndId(1L, 20L)).thenReturn(Optional.of(bank));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(invoiceRepository.findByCompanyIdAndId(1L, 30L)).thenReturn(Optional.of(inv));
        when(accountRepository.findByCompanyIdAndCode(1L, "4900")).thenReturn(Optional.of(fxGain));
        when(numberSeriesService.generateNextNumber(eq(1L), eq(DocType.RECEIPT))).thenReturn("RCT-1");
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(i -> {
            Receipt r = i.getArgument(0);
            if (r.getId() == null) r.setId(99L);
            return r;
        });
        when(postingService.postDocument(any())).thenReturn(new JournalEntryDto(
                500L, 1L, "JE-500", LocalDate.now(), 1L, "RCT-1", "desc",
                JournalSourceType.RECEIPT, 99L, "USD", new BigDecimal("3.700000"),
                new BigDecimal("1000.00"), new BigDecimal("1000.00"), JournalStatus.POSTED,
                null, null, null, List.of()
        ));
        when(salesMapper.toReceiptDto(any())).thenAnswer(i -> {
            Receipt r = i.getArgument(0);
            return new ReceiptDto(r.getId(), 1L, r.getReceiptNo(), 10L, null, null, r.getReceiptDate(),
                    20L, null, r.getPaymentMethod(), r.getReference(), r.getCurrencyCode(),
                    r.getAmount(), r.getUnappliedAmount(), r.getStatus(), r.getJournalEntryId(), List.of(), r.getExchangeRate());
        });

        ReceiptDto dto = service.createReceipt(new ReceiptRequest(
                10L, LocalDate.of(2026, 5, 1), 20L, PaymentMethod.BANK_TRANSFER, "REF", "USD",
                new BigDecimal("1000.00"), List.of(new ReceiptAllocationRequest(30L, new BigDecimal("1000.00"))),
                new BigDecimal("3.700000")
        ));

        assertEquals(0, new BigDecimal("3.700000").compareTo(dto.exchangeRate()));

        ArgumentCaptor<SourceDocumentPostingRequest> postCaptor = ArgumentCaptor.forClass(SourceDocumentPostingRequest.class);
        verify(postingService).postDocument(postCaptor.capture());
        List<JournalLineRequest> lines = postCaptor.getValue().lines();
        assertEquals(3, lines.size(), "Bank Dr, AR Cr, and Realized FX Gain Cr expected");
        assertEquals(0, new BigDecimal("3700.00").compareTo(lines.get(0).baseDebit()));
        assertEquals(0, new BigDecimal("3672.50").compareTo(lines.get(1).baseCredit()));
        assertEquals(4900L, lines.get(2).accountId());
        assertEquals(0, new BigDecimal("27.50").compareTo(lines.get(2).baseCredit()));

        // Reconciled guard on voidReceipt
        Receipt rct = new Receipt();
        rct.setId(99L);
        rct.setReceiptNo("RCT-1");
        rct.setStatus(ReceiptStatus.POSTED);
        rct.setJournalEntryId(500L);
        when(receiptRepository.findByCompanyIdAndId(1L, 99L)).thenReturn(Optional.of(rct));
        when(journalLineRepository.countReconciledLinesByJournalEntryId(1L, 500L)).thenReturn(1L);

        BusinessException voidEx = assertThrows(BusinessException.class, () -> service.voidReceipt(99L, "err"));
        assertEquals("RECEIPT_RECONCILED", voidEx.getCode());
    }

    @Test
    @DisplayName("PaymentServiceImpl posts Realized FX Loss when settling USD bill at higher rate and blocks voiding reconciled payment")
    void testPaymentFxLossAndReconciledVoidGuard() {
        PaymentServiceImpl service = new PaymentServiceImpl(
                paymentRepository, vendorRepository, billRepository, bankAccountRepository,
                companyRepository, postingService, numberSeriesService, purchasesMapper,
                accountRepository, journalLineRepository, exchangeRateRepository);

        Vendor vendor = new Vendor();
        vendor.setId(11L);
        vendor.setName("Supplier US");
        vendor.setCode("VEND-1");
        vendor.setApAccountId(2100L);
        vendor.setCurrencyCode("USD");

        BankAccount bank = new BankAccount();
        bank.setId(21L);
        bank.setName("USD Bank");
        bank.setGlAccountId(1110L);
        bank.setIsActive(true);

        Company company = new Company();
        company.setId(1L);
        company.setBaseCurrency("AED");

        Bill bill = new Bill();
        bill.setId(31L);
        bill.setCompanyId(1L);
        bill.setBillNo("BILL-31");
        bill.setVendorId(11L);
        bill.setCurrencyCode("USD");
        bill.setExchangeRate(new BigDecimal("3.670000"));
        bill.setTotal(new BigDecimal("1000.00"));
        bill.setAmountPaid(BigDecimal.ZERO);
        bill.setBalanceDue(new BigDecimal("1000.00"));
        bill.setStatus(BillStatus.APPROVED);

        Account fxLoss = new Account();
        fxLoss.setId(6200L);
        fxLoss.setIsActive(true);
        fxLoss.setIsGroup(false);

        when(vendorRepository.findByCompanyIdAndId(1L, 11L)).thenReturn(Optional.of(vendor));
        when(bankAccountRepository.findByCompanyIdAndId(1L, 21L)).thenReturn(Optional.of(bank));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(billRepository.findByCompanyIdAndId(1L, 31L)).thenReturn(Optional.of(bill));
        when(accountRepository.findByCompanyIdAndCode(1L, "6200")).thenReturn(Optional.of(fxLoss));
        when(numberSeriesService.generateNextNumber(eq(1L), eq(DocType.PAYMENT))).thenReturn("PAY-1");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            if (p.getId() == null) p.setId(199L);
            return p;
        });
        when(postingService.postDocument(any())).thenReturn(new JournalEntryDto(
                501L, 1L, "JE-501", LocalDate.now(), 1L, "PAY-1", "desc",
                JournalSourceType.PAYMENT, 199L, "USD", new BigDecimal("3.700000"),
                new BigDecimal("1000.00"), new BigDecimal("1000.00"), JournalStatus.POSTED,
                null, null, null, List.of()
        ));

        service.createPayment(new PaymentRequest(
                11L, LocalDate.of(2026, 5, 1), 21L, PaymentMethod.BANK_TRANSFER, "REF", "USD",
                new BigDecimal("1000.00"), List.of(new PaymentAllocationRequest(31L, new BigDecimal("1000.00"))),
                new BigDecimal("3.700000")
        ));

        ArgumentCaptor<SourceDocumentPostingRequest> postCaptor = ArgumentCaptor.forClass(SourceDocumentPostingRequest.class);
        verify(postingService).postDocument(postCaptor.capture());
        List<JournalLineRequest> lines = postCaptor.getValue().lines();
        assertEquals(3, lines.size(), "AP Dr, Bank Cr, and Realized FX Loss Dr expected");
        assertEquals(0, new BigDecimal("3670.00").compareTo(lines.get(0).baseDebit()));
        assertEquals(0, new BigDecimal("3700.00").compareTo(lines.get(1).baseCredit()));
        assertEquals(6200L, lines.get(2).accountId());
        assertEquals(0, new BigDecimal("30.00").compareTo(lines.get(2).baseDebit()));

        Payment pay = new Payment();
        pay.setId(199L);
        pay.setPaymentNo("PAY-1");
        pay.setStatus(PaymentStatus.POSTED);
        pay.setJournalEntryId(501L);
        when(paymentRepository.findByCompanyIdAndId(1L, 199L)).thenReturn(Optional.of(pay));
        when(journalLineRepository.countReconciledLinesByJournalEntryId(1L, 501L)).thenReturn(1L);

        BusinessException voidEx = assertThrows(BusinessException.class, () -> service.voidPayment(199L, "err"));
        assertEquals("PAYMENT_RECONCILED", voidEx.getCode());
    }

    @Test
    @DisplayName("BillServiceImpl converts foreign unitPrice to base currency for receiveStock and InvoiceServiceImpl avoids double-scaling COGS")
    void testForeignCurrencyInventoryAndCogsScaling() {
        BillServiceImpl billService = new BillServiceImpl(
                billRepository, vendorRepository, taxRateRepository, accountRepository, companyRepository,
                postingService, numberSeriesService, taxCalculator, purchasesMapper, itemRepository, warehouseRepository, stockService);

        Bill bill = new Bill();
        bill.setId(10L);
        bill.setCompanyId(1L);
        bill.setBillNo("BILL-10");
        bill.setDocType(BillDocType.BILL);
        bill.setVendorId(1L);
        bill.setBillDate(LocalDate.of(2026, 5, 1));
        bill.setCurrencyCode("USD");
        bill.setExchangeRate(new BigDecimal("3.672500"));
        bill.setSubtotal(new BigDecimal("1000.00"));
        bill.setTaxTotal(BigDecimal.ZERO);
        bill.setTotal(new BigDecimal("1000.00"));
        bill.setStatus(BillStatus.DRAFT);

        BillLine bl = new BillLine();
        bl.setId(101L);
        bl.setItemId(7L);
        bl.setWarehouseId(3L);
        bl.setAccountId(1300L);
        bl.setDescription("Widget");
        bl.setQuantity(new BigDecimal("10"));
        bl.setUnitPrice(new BigDecimal("100.00"));
        bl.setTaxAmount(BigDecimal.ZERO);
        bl.setLineTotal(new BigDecimal("1000.00"));
        bill.addLine(bl);

        Vendor v = new Vendor();
        v.setId(1L);
        v.setName("Vendor");
        v.setApAccountId(2100L);

        Item invItem = new Item();
        invItem.setId(7L);
        invItem.setSku("SKU-7");
        invItem.setItemType(ItemType.INVENTORY);
        invItem.setCogsAccountId(5100L);
        invItem.setInventoryAccountId(1300L);

        when(billRepository.findByCompanyIdAndId(1L, 10L)).thenReturn(Optional.of(bill));
        when(vendorRepository.findByCompanyIdAndId(1L, 1L)).thenReturn(Optional.of(v));
        when(itemRepository.findByCompanyIdAndId(1L, 7L)).thenReturn(Optional.of(invItem));
        when(billRepository.save(any(Bill.class))).thenAnswer(i -> i.getArgument(0));
        when(purchasesMapper.toBillDto(any())).thenAnswer(i -> {
            Bill b = i.getArgument(0);
            return new BillDto(b.getId(), 1L, b.getBillNo(), null, b.getDocType(), 1L, null, null,
                    b.getBillDate(), b.getDueDate(), b.getCurrencyCode(), b.getExchangeRate(),
                    b.getSubtotal(), b.getTaxTotal(), b.getTotal(), BigDecimal.ZERO, b.getTotal(),
                    b.getStatus(), b.getJournalEntryId(), null, List.of());
        });
        when(postingService.postDocument(any())).thenReturn(new JournalEntryDto(
                800L, 1L, "JE-800", LocalDate.now(), 1L, "BILL-10", "desc",
                JournalSourceType.BILL, 10L, "USD", new BigDecimal("3.672500"),
                new BigDecimal("1000.00"), new BigDecimal("1000.00"), JournalStatus.POSTED,
                null, null, null, List.of()
        ));

        billService.approveBill(10L);
        verify(stockService).receiveStock(
                eq(1L), eq(7L), eq(3L), eq(new BigDecimal("10")),
                eq(new BigDecimal("367.2500")),
                eq(MovementType.PURCHASE), eq(StockSourceType.BILL), eq(10L), eq(101L), isNull());

        // Foreign currency Invoice COGS scaling
        InvoiceServiceImpl invoiceService = new InvoiceServiceImpl(
                invoiceRepository, customerRepository, taxRateRepository, accountRepository, companyRepository,
                postingService, numberSeriesService, taxCalculator, salesMapper, itemRepository, warehouseRepository,
                stockService, mailService);

        Invoice inv = new Invoice();
        inv.setId(20L);
        inv.setCompanyId(1L);
        inv.setInvoiceNo("INV-20");
        inv.setDocType(InvoiceDocType.INVOICE);
        inv.setCustomerId(2L);
        inv.setInvoiceDate(LocalDate.of(2026, 5, 2));
        inv.setCurrencyCode("USD");
        inv.setExchangeRate(new BigDecimal("3.672500"));
        inv.setSubtotal(new BigDecimal("2000.00"));
        inv.setTaxTotal(BigDecimal.ZERO);
        inv.setTotal(new BigDecimal("2000.00"));
        inv.setStatus(InvoiceStatus.DRAFT);

        InvoiceLine il = new InvoiceLine();
        il.setId(201L);
        il.setItemId(7L);
        il.setWarehouseId(3L);
        il.setAccountId(4100L);
        il.setDescription("Widget Sale");
        il.setQuantity(new BigDecimal("10"));
        il.setUnitPrice(new BigDecimal("200.00"));
        il.setTaxAmount(BigDecimal.ZERO);
        il.setLineTotal(new BigDecimal("2000.00"));
        inv.addLine(il);

        Customer c = new Customer();
        c.setId(2L);
        c.setName("Buyer");
        c.setArAccountId(1200L);

        StockMovement sm = new StockMovement();
        sm.setUnitCost(new BigDecimal("367.2500"));

        when(invoiceRepository.findByCompanyIdAndId(1L, 20L)).thenReturn(Optional.of(inv));
        when(customerRepository.findByCompanyIdAndId(1L, 2L)).thenReturn(Optional.of(c));
        when(stockService.issueStock(eq(1L), eq(7L), eq(3L), eq(new BigDecimal("10")),
                eq(MovementType.SALE), eq(StockSourceType.INVOICE), eq(20L), eq(201L), isNull())).thenReturn(sm);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));
        when(salesMapper.toInvoiceDto(any())).thenAnswer(i -> {
            Invoice iv = i.getArgument(0);
            return new InvoiceDto(iv.getId(), 1L, iv.getInvoiceNo(), iv.getDocType(), 2L, null, null,
                    iv.getInvoiceDate(), iv.getDueDate(), iv.getCurrencyCode(), iv.getExchangeRate(),
                    iv.getSubtotal(), BigDecimal.ZERO, iv.getTaxTotal(), iv.getTotal(),
                    BigDecimal.ZERO, iv.getTotal(), iv.getStatus(), null, null, null, iv.getJournalEntryId(), List.of());
        });
        when(postingService.postDocument(any())).thenReturn(new JournalEntryDto(
                801L, 1L, "JE-801", LocalDate.now(), 1L, "INV-20", "desc",
                JournalSourceType.INVOICE, 20L, "USD", new BigDecimal("3.672500"),
                new BigDecimal("3000.00"), new BigDecimal("3000.00"), JournalStatus.POSTED,
                null, null, null, List.of()
        ));

        invoiceService.sendInvoice(20L);

        ArgumentCaptor<SourceDocumentPostingRequest> invPostCaptor = ArgumentCaptor.forClass(SourceDocumentPostingRequest.class);
        verify(postingService, times(2)).postDocument(invPostCaptor.capture());
        List<JournalLineRequest> invLines = invPostCaptor.getAllValues().get(1).lines();
        // Line 2 = COGS debit, Line 3 = Inventory credit: must carry baseDebit/baseCredit = 3672.50 (NOT 3672.50 * 3.6725)
        JournalLineRequest cogsLine = invLines.get(2);
        JournalLineRequest invReliefLine = invLines.get(3);
        assertEquals(0, new BigDecimal("1000.00").compareTo(cogsLine.debit()));
        assertEquals(0, new BigDecimal("3672.50").compareTo(cogsLine.baseDebit()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(invReliefLine.credit()));
        assertEquals(0, new BigDecimal("3672.50").compareTo(invReliefLine.baseCredit()));
    }
}
