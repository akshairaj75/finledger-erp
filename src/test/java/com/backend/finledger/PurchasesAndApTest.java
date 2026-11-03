package com.backend.finledger;

import com.backend.finledger.banking.entity.AccountKind;
import com.backend.finledger.banking.entity.BankAccount;
import com.backend.finledger.banking.repository.BankAccountRepository;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.DocType;
import com.backend.finledger.common.NumberSeries;
import com.backend.finledger.common.NumberSeriesRepository;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.ledger.dto.CreateFiscalYearRequest;
import com.backend.finledger.ledger.dto.FiscalYearDto;
import com.backend.finledger.ledger.entity.*;
import com.backend.finledger.ledger.repository.*;
import com.backend.finledger.ledger.service.FiscalYearPeriodService;
import com.backend.finledger.purchases.dto.*;
import com.backend.finledger.purchases.entity.*;
import com.backend.finledger.purchases.repository.*;
import com.backend.finledger.purchases.service.*;
import com.backend.finledger.sales.entity.PaymentMethod;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PurchasesAndApTest {

    @Autowired
    private VendorService vendorService;

    @Autowired
    private BillService billService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ApAgeingService apAgeingService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

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

    private Long bsbCompanyId;
    private Long apAccountId;
    private Long expenseAccountId;
    private Long vatInputAccountId;
    private Long bankGlAccountId;
    private Long taxRate5PctId;
    private Long bankAccountId;

    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();

        Company bsb = companyRepository.findByCode("BSB").orElseThrow();
        bsbCompanyId = bsb.getId();

        authenticateAs(1L, bsbCompanyId, "admin@finledger.com", Set.of(
                "VENDOR_VIEW", "VENDOR_CREATE", "VENDOR_EDIT",
                "BILL_VIEW", "BILL_CREATE", "BILL_APPROVE", "BILL_VOID",
                "PAYMENT_VIEW", "PAYMENT_CREATE", "PAYMENT_VOID",
                "REPORT_VIEW", "JOURNAL_VIEW", "JOURNAL_CREATE", "JOURNAL_POST", "JOURNAL_VOID",
                "SETTINGS_EDIT", "ACCOUNT_VIEW", "ACCOUNT_EDIT"
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

        // 2. GL Accounts
        Account apAcc = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "2100").orElseGet(() -> {
            Account a = new Account(bsbCompanyId, "2100", "Accounts Payable", AccountType.LIABILITY, "AP", null, false, NormalBalance.CREDIT);
            return accountRepository.save(a);
        });
        apAccountId = apAcc.getId();

        Account expAcc = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "5100").orElseGet(() -> {
            Account a = new Account(bsbCompanyId, "5100", "Operating Expenses", AccountType.EXPENSE, "EXPENSE", null, false, NormalBalance.DEBIT);
            return accountRepository.save(a);
        });
        expenseAccountId = expAcc.getId();

        Account vatInAcc = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "1150").orElseGet(() -> {
            Account a = new Account(bsbCompanyId, "1150", "VAT Input", AccountType.ASSET, "TAX", null, false, NormalBalance.DEBIT);
            return accountRepository.save(a);
        });
        vatInputAccountId = vatInAcc.getId();

        Account bankGl = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "1110").orElseGet(() -> {
            Account a = new Account(bsbCompanyId, "1110", "Bank Account", AccountType.ASSET, "BANK", null, false, NormalBalance.DEBIT);
            return accountRepository.save(a);
        });
        bankGlAccountId = bankGl.getId();

        // 3. Tax Rate
        TaxRate taxRate = taxRateRepository.findByCompanyIdAndCode(bsbCompanyId, "VAT-5-PURCHASE").orElseGet(() -> {
            TaxRate tr = new TaxRate(bsbCompanyId, "VAT-5-PURCHASE", "5% Input VAT", new BigDecimal("5.0000"), TaxType.STANDARD);
            tr.setPurchaseAccountId(vatInputAccountId);
            return taxRateRepository.save(tr);
        });
        taxRate.setPurchaseAccountId(vatInputAccountId);
        taxRateRepository.save(taxRate);
        taxRate5PctId = taxRate.getId();

        // 4. Bank Account
        BankAccount bank = bankAccountRepository.findByCompanyIdAndIsActiveTrue(bsbCompanyId).stream()
                .filter(b -> b.getGlAccountId().equals(bankGlAccountId))
                .findFirst()
                .orElseGet(() -> {
                    BankAccount ba = new BankAccount();
                    ba.setCompanyId(bsbCompanyId);
                    ba.setName("Purchases Clearing Bank");
                    ba.setBankName("Emirates NBD");
                    ba.setAccountNumber("9988776655");
                    ba.setIban("AE998877665544332211001");
                    ba.setCurrencyCode("AED");
                    ba.setGlAccountId(bankGlAccountId);
                    ba.setAccountKind(AccountKind.BANK);
                    ba.setIsActive(true);
                    return bankAccountRepository.save(ba);
                });
        bankAccountId = bank.getId();

        // 5. Number series
        ensureNumberSeries(bsbCompanyId, DocType.BILL, "BILL-", 1);
        ensureNumberSeries(bsbCompanyId, DocType.PAYMENT, "PAY-", 1);
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

    @Test
    @Transactional
    @DisplayName("1. Vendor CRUD with unique code validation and IBAN tracking")
    void testVendorCrudAndValidation() {
        String code = "VEND-" + (System.currentTimeMillis() % 1000000);
        VendorRequest req = new VendorRequest(
                code,
                "Dell Technologies Middle East",
                "100987654300003",
                "billing@dell.ae",
                "+971 4 888 7766",
                "Dubai Internet City, Bldg 3",
                "AED",
                (short) 45,
                "AE020330001234567890123",
                apAccountId,
                true
        );

        VendorDto created = vendorService.createVendor(req);
        assertNotNull(created.id());
        assertEquals(code, created.code());
        assertEquals("AE020330001234567890123", created.bankIban());
        assertEquals((short) 45, created.paymentTermsDays());

        // Duplicate code check
        assertThrows(BusinessException.class, () -> vendorService.createVendor(req));

        // Update vendor
        VendorRequest updateReq = new VendorRequest(
                code,
                "Dell Technologies Global LLC",
                "100987654300003",
                "finance@dell.ae",
                "+971 4 888 7766",
                "Dubai Internet City, Bldg 3",
                "AED",
                (short) 60,
                "AE020330001234567890999",
                apAccountId,
                true
        );
        VendorDto updated = vendorService.updateVendor(created.id(), updateReq);
        assertEquals("Dell Technologies Global LLC", updated.name());
        assertEquals((short) 60, updated.paymentTermsDays());
        assertEquals("AE020330001234567890999", updated.bankIban());
    }

    @Test
    @Transactional
    @DisplayName("2. Unique (vendor_id, vendor_invoice_no) constraint enforcement across bills")
    void testUniqueVendorInvoiceNoEnforcement() {
        VendorDto vendor = vendorService.createVendor(new VendorRequest(
                "HP-" + (System.currentTimeMillis() % 1000000),
                "HP Middle East",
                "100333444500003",
                "hp@hp.com",
                null,
                null,
                "AED",
                (short) 30,
                null,
                apAccountId,
                true
        ));

        String vendorInvNo = "INV-DELL-9001";
        BillRequest b1 = new BillRequest(
                BillDocType.BILL,
                vendorInvNo,
                vendor.id(),
                today,
                today.plusDays(30),
                "AED",
                BigDecimal.ONE,
                "Servers",
                List.of(new BillLineRequest((short) 1, null, expenseAccountId, null, "Server Rack", new BigDecimal("1"), new BigDecimal("5000.00"), taxRate5PctId, null))
        );

        BillDto created1 = billService.createBill(b1);
        assertNotNull(created1.id());

        // Attempt second bill with identical vendor invoice number for the same vendor
        BillRequest b2 = new BillRequest(
                BillDocType.BILL,
                vendorInvNo,
                vendor.id(),
                today,
                today.plusDays(30),
                "AED",
                BigDecimal.ONE,
                "Duplicate",
                List.of(new BillLineRequest((short) 1, null, expenseAccountId, null, "Server Rack Duplicate", new BigDecimal("1"), new BigDecimal("5000.00"), taxRate5PctId, null))
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> billService.createBill(b2));
        assertEquals("DUPLICATE_VENDOR_INVOICE_NO", ex.getCode());
    }

    @Test
    @Transactional
    @DisplayName("3. Bill Approval: Generates gap-free number and balanced GL entry (Dr Expense, Dr Input VAT, Cr AP)")
    void testBillApprovalAndGlPosting() {
        VendorDto vendor = vendorService.createVendor(new VendorRequest(
                "CS-" + (System.currentTimeMillis() % 1000000),
                "Cisco Systems Gulf",
                "100111222300003",
                null,
                null,
                null,
                "AED",
                (short) 30,
                null,
                apAccountId,
                true
        ));

        // Bill: 2 routers @ 1000 AED = 2000 AED Net. 5% VAT = 100 AED. Total = 2100 AED.
        BillRequest billReq = new BillRequest(
                BillDocType.BILL,
                "CISCO-8891",
                vendor.id(),
                today,
                today.plusDays(30),
                "AED",
                BigDecimal.ONE,
                "Core Routers",
                List.of(new BillLineRequest((short) 1, null, expenseAccountId, null, "Cisco Catalyst 9200", new BigDecimal("2"), new BigDecimal("1000.00"), taxRate5PctId, null))
        );

        BillDto draft = billService.createBill(billReq);
        assertEquals(BillStatus.DRAFT, draft.status());
        assertEquals(new BigDecimal("2000.00"), draft.subtotal());
        assertEquals(new BigDecimal("100.00"), draft.taxTotal());
        assertEquals(new BigDecimal("2100.00"), draft.total());
        assertEquals(new BigDecimal("2100.00"), draft.balanceDue());

        // Approve bill
        BillDto approved = billService.approveBill(draft.id());
        assertEquals(BillStatus.APPROVED, approved.status());
        assertTrue(approved.billNo().startsWith("BILL-"));
        assertNotNull(approved.journalEntryId());

        // Inspect GL Journal Entry
        JournalEntry journal = journalEntryRepository.findById(approved.journalEntryId()).orElseThrow();
        assertEquals(JournalStatus.POSTED, journal.getStatus());
        assertEquals(new BigDecimal("2100.00"), journal.getTotalDebit());
        assertEquals(new BigDecimal("2100.00"), journal.getTotalCredit());

        // Verify Dr Expense = 2000, Dr Input VAT = 100, Cr AP = 2100
        BigDecimal debitExp = journal.getLines().stream()
                .filter(l -> l.getAccountId().equals(expenseAccountId))
                .map(JournalLine::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal debitVat = journal.getLines().stream()
                .filter(l -> l.getAccountId().equals(vatInputAccountId))
                .map(JournalLine::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal creditAp = journal.getLines().stream()
                .filter(l -> l.getAccountId().equals(apAccountId))
                .map(JournalLine::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("2000.00"), debitExp);
        assertEquals(new BigDecimal("100.00"), debitVat);
        assertEquals(new BigDecimal("2100.00"), creditAp);
    }

    @Test
    @Transactional
    @DisplayName("4. Vendor Payment Allocation: Updates bill balance and posts Dr AP, Cr Bank")
    void testVendorPaymentAllocationAndGlPosting() {
        VendorDto vendor = vendorService.createVendor(new VendorRequest(
                "LN-" + (System.currentTimeMillis() % 1000000),
                "Lenovo Middle East",
                "100444555600003",
                null,
                null,
                null,
                "AED",
                (short) 30,
                null,
                apAccountId,
                true
        ));

        // Create & approve bill of 1050 AED (1000 + 50 VAT)
        BillDto bill = billService.createBill(new BillRequest(
                BillDocType.BILL,
                "LEN-101",
                vendor.id(),
                today,
                today.plusDays(30),
                "AED",
                BigDecimal.ONE,
                "ThinkPad Laptops",
                List.of(new BillLineRequest((short) 1, null, expenseAccountId, null, "ThinkPad X1", new BigDecimal("1"), new BigDecimal("1000.00"), taxRate5PctId, null))
        ));
        bill = billService.approveBill(bill.id());

        // Record payment of 600 AED allocated to this bill
        PaymentRequest payReq = new PaymentRequest(
                vendor.id(),
                today,
                bankAccountId,
                PaymentMethod.BANK_TRANSFER,
                "FT-776655",
                "AED",
                new BigDecimal("600.00"),
                List.of(new PaymentAllocationRequest(bill.id(), new BigDecimal("600.00")))
        );

        PaymentDto payment = paymentService.createPayment(payReq);
        assertEquals(PaymentStatus.POSTED, payment.status());
        assertTrue(payment.paymentNo().startsWith("PAY-"));
        assertEquals(new BigDecimal("0.00"), payment.unappliedAmount());

        // Verify bill updated to PARTIAL with balance 450 AED
        Bill updatedBill = billRepository.findById(bill.id()).orElseThrow();
        assertEquals(BillStatus.PARTIAL, updatedBill.getStatus());
        assertEquals(new BigDecimal("600.00"), updatedBill.getAmountPaid());
        assertEquals(new BigDecimal("450.00"), updatedBill.getBalanceDue());

        // Check Payment Journal Entry: Dr AP 600, Cr Bank 600
        JournalEntry journal = journalEntryRepository.findById(payment.journalEntryId()).orElseThrow();
        assertEquals(new BigDecimal("600.00"), journal.getTotalDebit());
        assertEquals(new BigDecimal("600.00"), journal.getTotalCredit());

        BigDecimal debitAp = journal.getLines().stream()
                .filter(l -> l.getAccountId().equals(apAccountId))
                .map(JournalLine::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal creditBank = journal.getLines().stream()
                .filter(l -> l.getAccountId().equals(bankGlAccountId))
                .map(JournalLine::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("600.00"), debitAp);
        assertEquals(new BigDecimal("600.00"), creditBank);

        // Settle remaining 450 AED + 100 AED advance = 550 AED payment
        PaymentRequest finalPayReq = new PaymentRequest(
                vendor.id(),
                today,
                bankAccountId,
                PaymentMethod.CHEQUE,
                "CHQ-9901",
                "AED",
                new BigDecimal("550.00"),
                List.of(new PaymentAllocationRequest(bill.id(), new BigDecimal("450.00")))
        );

        PaymentDto secondPayment = paymentService.createPayment(finalPayReq);
        assertEquals(new BigDecimal("100.00"), secondPayment.unappliedAmount());

        Bill settledBill = billRepository.findById(bill.id()).orElseThrow();
        assertEquals(BillStatus.PAID, settledBill.getStatus());
        assertEquals(new BigDecimal("1050.00"), settledBill.getAmountPaid());
        assertEquals(new BigDecimal("0.00"), settledBill.getBalanceDue());
    }

    @Test
    @Transactional
    @DisplayName("5. AP Ageing Report correctly buckets open vendor balances")
    void testApAgeingReport() {
        VendorDto vendor = vendorService.createVendor(new VendorRequest(
                "MS-" + (System.currentTimeMillis() % 1000000),
                "Microsoft Gulf FZ",
                null,
                null,
                null,
                null,
                "AED",
                (short) 30,
                null,
                apAccountId,
                true
        ));

        // Bill 1: Due in 10 days (Current bucket)
        BillDto b1 = billService.createBill(new BillRequest(
                BillDocType.BILL,
                "MS-01",
                vendor.id(),
                today,
                today.plusDays(10),
                "AED",
                BigDecimal.ONE,
                "Azure cloud",
                List.of(new BillLineRequest((short) 1, null, expenseAccountId, null, "Azure", new BigDecimal("1"), new BigDecimal("1000.00"), null, null))
        ));
        billService.approveBill(b1.id());

        // Bill 2: Due 15 days ago (1-30 days overdue bucket)
        BillDto b2 = billService.createBill(new BillRequest(
                BillDocType.BILL,
                "MS-02",
                vendor.id(),
                today.minusDays(45),
                today.minusDays(15),
                "AED",
                BigDecimal.ONE,
                "M365 Licenses",
                List.of(new BillLineRequest((short) 1, null, expenseAccountId, null, "M365", new BigDecimal("1"), new BigDecimal("2000.00"), null, null))
        ));
        billService.approveBill(b2.id());

        ApAgeingReportDto report = apAgeingService.getApAgeingReport(today);
        assertNotNull(report);
        assertTrue(report.grandTotal().compareTo(BigDecimal.ZERO) > 0);

        ApAgeingVendorDto vendorRow = report.vendors().stream()
                .filter(v -> v.vendorId().equals(vendor.id()))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("1000.00"), vendorRow.current());
        assertEquals(new BigDecimal("2000.00"), vendorRow.days1To30());
        assertEquals(new BigDecimal("3000.00"), vendorRow.totalOutstanding());
    }

    @Test
    @Transactional
    @DisplayName("6. Voiding Bill creates reversing journal entry in GL")
    void testVoidBillReversal() {
        VendorDto vendor = vendorService.createVendor(new VendorRequest(
                "OR-" + (System.currentTimeMillis() % 1000000),
                "Oracle Systems ME",
                null,
                null,
                null,
                null,
                "AED",
                (short) 30,
                null,
                apAccountId,
                true
        ));

        BillDto bill = billService.createBill(new BillRequest(
                BillDocType.BILL,
                "ORA-5544",
                vendor.id(),
                today,
                today.plusDays(30),
                "AED",
                BigDecimal.ONE,
                "Database licenses",
                List.of(new BillLineRequest((short) 1, null, expenseAccountId, null, "Oracle DB", new BigDecimal("1"), new BigDecimal("4000.00"), null, null))
        ));
        bill = billService.approveBill(bill.id());
        assertNotNull(bill.journalEntryId());

        BillDto voided = billService.voidBill(bill.id(), "Incorrect billing currency");
        assertEquals(BillStatus.VOID, voided.status());
        assertEquals(0, BigDecimal.ZERO.compareTo(voided.balanceDue()));

        // Reversal voucher exists in journal entries
        assertTrue(journalEntryRepository.existsByCompanyIdAndReversalOfId(bsbCompanyId, bill.journalEntryId()));
    }
}
