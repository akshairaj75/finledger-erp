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
import com.backend.finledger.ledger.service.*;
import com.backend.finledger.sales.dto.*;
import com.backend.finledger.sales.entity.*;
import com.backend.finledger.sales.repository.*;
import com.backend.finledger.sales.service.*;
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
class SalesAndArTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private ReceiptService receiptService;

    @Autowired
    private ArAgeingService arAgeingService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ReceiptRepository receiptRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TaxRateRepository taxRateRepository;

    @Autowired
    private FiscalYearRepository fiscalYearRepository;

    @Autowired
    private FiscalPeriodRepository fiscalPeriodRepository;

    @Autowired
    private FiscalYearPeriodService fiscalYearPeriodService;

    @Autowired
    private NumberSeriesRepository numberSeriesRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    private Long bsbCompanyId;
    private Long acmeCompanyId;

    private Long arAccountId;
    private Long revenueAccountId;
    private Long vatAccountId;
    private Long bankGlAccountId;
    private Long taxRate5PctId;
    private Long bankAccountId;

    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();

        Company bsb = companyRepository.findByCode("BSB").orElseThrow();
        bsbCompanyId = bsb.getId();

        Company acme = companyRepository.findByCode("ACME").orElseGet(() -> {
            Company c = new Company();
            c.setCode("ACME");
            c.setLegalName("ACME Corporation FZE");
            c.setCountryCode("AE");
            c.setBaseCurrency("AED");
            c.setFiscalYearStartMonth((byte) 1);
            return companyRepository.save(c);
        });
        acmeCompanyId = acme.getId();

        authenticateAs(1L, bsbCompanyId, "admin@finledger.com", Set.of(
                "CUSTOMER_VIEW", "CUSTOMER_CREATE", "CUSTOMER_EDIT",
                "INVOICE_VIEW", "INVOICE_CREATE", "INVOICE_SEND", "INVOICE_VOID",
                "RECEIPT_VIEW", "RECEIPT_CREATE", "RECEIPT_VOID",
                "REPORT_VIEW", "JOURNAL_VIEW", "JOURNAL_CREATE", "JOURNAL_POST", "JOURNAL_VOID",
                "SETTINGS_EDIT", "ACCOUNT_VIEW", "ACCOUNT_EDIT"
        ));

        // 1. Ensure Fiscal Year & Periods exist for current year
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

        // 2. Fetch or create GL accounts
        Account arAcc = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "1120").orElseGet(() -> {
            Account a = new Account(bsbCompanyId, "1120", "Accounts Receivable", AccountType.ASSET, "AR", null, false, NormalBalance.DEBIT);
            return accountRepository.save(a);
        });
        arAccountId = arAcc.getId();

        Account revAcc = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "4100").orElseGet(() -> {
            Account a = new Account(bsbCompanyId, "4100", "Sales Revenue", AccountType.INCOME, "REVENUE", null, false, NormalBalance.CREDIT);
            return accountRepository.save(a);
        });
        revenueAccountId = revAcc.getId();

        Account vatAcc = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "2130").orElseGet(() -> {
            Account a = new Account(bsbCompanyId, "2130", "VAT Payable", AccountType.LIABILITY, "TAX", null, false, NormalBalance.CREDIT);
            return accountRepository.save(a);
        });
        vatAccountId = vatAcc.getId();

        Account bankAcc = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "1110").orElseGet(() -> {
            Account a = new Account(bsbCompanyId, "1110", "Bank Account", AccountType.ASSET, "BANK", null, false, NormalBalance.DEBIT);
            return accountRepository.save(a);
        });
        bankGlAccountId = bankAcc.getId();

        // 3. Tax Rate (5% VAT)
        TaxRate taxRate = taxRateRepository.findByCompanyIdAndCode(bsbCompanyId, "VAT-5").orElseGet(() -> {
            TaxRate tr = new TaxRate(bsbCompanyId, "VAT-5", "5% Standard VAT", new BigDecimal("5.0000"), TaxType.STANDARD);
            tr.setSalesAccountId(vatAccountId);
            return taxRateRepository.save(tr);
        });
        taxRate.setSalesAccountId(vatAccountId);
        taxRateRepository.save(taxRate);
        taxRate5PctId = taxRate.getId();

        // 4. Bank Account
        BankAccount bank = bankAccountRepository.findByCompanyIdAndIsActiveTrue(bsbCompanyId).stream()
                .filter(b -> b.getGlAccountId().equals(bankGlAccountId))
                .findFirst()
                .orElseGet(() -> {
                    BankAccount ba = new BankAccount();
                    ba.setCompanyId(bsbCompanyId);
                    ba.setName("Emirates NBD Main");
                    ba.setBankName("Emirates NBD");
                    ba.setAccountNumber("1234567890");
                    ba.setIban("AE123456789012345678901");
                    ba.setCurrencyCode("AED");
                    ba.setGlAccountId(bankGlAccountId);
                    ba.setAccountKind(AccountKind.BANK);
                    ba.setIsActive(true);
                    return bankAccountRepository.save(ba);
                });
        bankAccountId = bank.getId();

        // 5. Number series for INVOICE, CREDIT_NOTE, RECEIPT, JOURNAL
        ensureNumberSeries(bsbCompanyId, DocType.INVOICE, "INV-", 1);
        ensureNumberSeries(bsbCompanyId, DocType.CREDIT_NOTE, "CRN-", 1);
        ensureNumberSeries(bsbCompanyId, DocType.RECEIPT, "RCT-", 1);
        ensureNumberSeries(bsbCompanyId, DocType.JOURNAL, "JRN-", 1);
    }

    private void ensureNumberSeries(Long companyId, DocType docType, String prefix, Integer nextVal) {
        if (numberSeriesRepository.findByCompanyIdAndDocType(companyId, docType).isEmpty()) {
            NumberSeries ns = new NumberSeries();
            ns.setCompanyId(companyId);
            ns.setDocType(docType);
            ns.setPrefix(prefix);
            ns.setNextNumber(nextVal);
            ns.setPadding((byte) 5);
            numberSeriesRepository.save(ns);
        }
    }

    private void authenticateAs(Long userId, Long companyId, String email, Set<String> perms) {
        UserPrincipal principal = new UserPrincipal(
                userId,
                companyId,
                email,
                "",
                "Test User",
                true,
                perms
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @Transactional
    @DisplayName("1. Customer CRUD and duplicate code validation")
    void testCustomerCrud() {
        CustomerRequest req = new CustomerRequest(
                "CUST-001",
                "Al Futtaim Motors",
                "100200300400003",
                "info@alfuttaim.ae",
                "+97141234567",
                "Dubai Festival City, Dubai, UAE",
                "AE",
                "AED",
                (short) 30,
                new BigDecimal("50000.00"),
                arAccountId,
                null,
                null,
                true
        );

        CustomerDto created = customerService.createCustomer(req);
        assertNotNull(created.id());
        assertEquals("CUST-001", created.code());
        assertEquals("Al Futtaim Motors", created.name());

        // Duplicate code within same company should throw DUPLICATE_CUSTOMER_CODE
        BusinessException ex = assertThrows(BusinessException.class, () -> customerService.createCustomer(req));
        assertEquals("DUPLICATE_CUSTOMER_CODE", ex.getCode());

        // Update customer
        CustomerRequest updateReq = new CustomerRequest(
                "CUST-001",
                "Al Futtaim Motors LLC",
                "100200300400003",
                "billing@alfuttaim.ae",
                "+97141234567",
                "Dubai Festival City, Dubai, UAE",
                "AE",
                "AED",
                (short) 45,
                new BigDecimal("100000.00"),
                arAccountId,
                null,
                null,
                true
        );
        CustomerDto updated = customerService.updateCustomer(created.id(), updateReq);
        assertEquals("Al Futtaim Motors LLC", updated.name());
        assertEquals((short) 45, updated.paymentTermsDays());
    }

    @Test
    @Transactional
    @DisplayName("2. Invoice Creation: Server-side tax & discount recalculation ignores client manipulation")
    void testServerSideRecalculation() {
        CustomerDto cust = createTestCustomer("CUST-TAX-1");

        // Request contains 2 items:
        // Line 1: Qty 2 @ 100.00, discount 10% -> Net 180.00, 5% Tax = 9.00 -> Line Total 189.00
        // Line 2: Qty 1 @ 50.00, discount 0% -> Net 50.00, 5% Tax = 2.50 -> Line Total 52.50
        // Document Totals: Subtotal 250.00, Discount 20.00, Tax 11.50, Grand Total 241.50
        InvoiceRequest invReq = new InvoiceRequest(
                InvoiceDocType.INVOICE,
                cust.id(),
                today,
                today.plusDays(30),
                "AED",
                BigDecimal.ONE,
                "Test Invoice with discount and tax",
                null,
                null,
                List.of(
                        new InvoiceLineRequest((short) 1, null, "Item 1", new BigDecimal("2"), new BigDecimal("100.00"), new BigDecimal("10.00"), taxRate5PctId, revenueAccountId, null, null),
                        new InvoiceLineRequest((short) 2, null, "Item 2", new BigDecimal("1"), new BigDecimal("50.00"), BigDecimal.ZERO, taxRate5PctId, revenueAccountId, null, null)
                )
        );

        InvoiceDto invoice = invoiceService.createInvoice(invReq);
        assertNotNull(invoice.id());
        assertEquals(InvoiceStatus.DRAFT, invoice.status());
        assertEquals(new BigDecimal("250.00"), invoice.subtotal());
        assertEquals(new BigDecimal("20.00"), invoice.discountTotal());
        assertEquals(new BigDecimal("11.50"), invoice.taxTotal());
        assertEquals(new BigDecimal("241.50"), invoice.total());
        assertEquals(new BigDecimal("241.50"), invoice.balanceDue());
        assertEquals(new BigDecimal("0.00"), invoice.amountPaid());
        assertEquals(2, invoice.lines().size());
        assertEquals(new BigDecimal("189.00"), invoice.lines().get(0).lineTotal());
        assertEquals(new BigDecimal("9.00"), invoice.lines().get(0).taxAmount());
    }

    @Test
    @Transactional
    @DisplayName("3. Invoice Send / Post: Gap-free number, status SENT, double-entry GL posting")
    void testSendInvoiceAndGlPosting() {
        CustomerDto cust = createTestCustomer("CUST-POST-1");

        InvoiceDto draft = invoiceService.createInvoice(new InvoiceRequest(
                InvoiceDocType.INVOICE,
                cust.id(),
                today,
                today.plusDays(30),
                "AED",
                BigDecimal.ONE,
                "Consulting Services",
                null,
                null,
                List.of(
                        new InvoiceLineRequest((short) 1, null, "Consulting Hours", new BigDecimal("10"), new BigDecimal("100.00"), BigDecimal.ZERO, taxRate5PctId, revenueAccountId, null, null)
                )
        ));

        // Subtotal = 1000.00, Tax = 50.00, Total = 1050.00
        assertEquals(new BigDecimal("1050.00"), draft.total());

        // Send & Post invoice
        InvoiceDto sent = invoiceService.sendInvoice(draft.id());
        assertEquals(InvoiceStatus.SENT, sent.status());
        assertFalse(sent.invoiceNo().startsWith("DRAFT-"));
        assertTrue(sent.invoiceNo().startsWith("INV-"));
        assertNotNull(sent.journalEntryId());

        // Verify Journal Entry was created and posted
        JournalEntry journal = journalEntryRepository.findById(sent.journalEntryId()).orElseThrow();
        assertEquals(JournalStatus.POSTED, journal.getStatus());
        assertEquals(new BigDecimal("1050.00"), journal.getTotalDebit());
        assertEquals(new BigDecimal("1050.00"), journal.getTotalCredit());
        assertEquals(3, journal.getLines().size());

        // Line 1: Debit AR for 1050.00
        JournalLine arLine = journal.getLines().stream()
                .filter(l -> l.getAccountId().equals(arAccountId))
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("1050.00"), arLine.getDebit());
        assertEquals(BigDecimal.ZERO.setScale(2), arLine.getCredit());
        assertEquals(PartyType.CUSTOMER, arLine.getPartyType());
        assertEquals(cust.id(), arLine.getPartyId());

        // Line 2: Credit Revenue for 1000.00
        JournalLine revLine = journal.getLines().stream()
                .filter(l -> l.getAccountId().equals(revenueAccountId))
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("1000.00"), revLine.getCredit());
        assertEquals(BigDecimal.ZERO.setScale(2), revLine.getDebit());

        // Line 3: Credit Output VAT for 50.00
        JournalLine vatLine = journal.getLines().stream()
                .filter(l -> l.getAccountId().equals(vatAccountId))
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("50.00"), vatLine.getCredit());
        assertEquals(BigDecimal.ZERO.setScale(2), vatLine.getDebit());
    }

    @Test
    @Transactional
    @DisplayName("4. Credit Note Posting: Reverse double-entry (Dr Revenue, Dr VAT, Cr AR)")
    void testCreditNotePosting() {
        CustomerDto cust = createTestCustomer("CUST-CRN-1");

        InvoiceDto creditNoteDraft = invoiceService.createInvoice(new InvoiceRequest(
                InvoiceDocType.CREDIT_NOTE,
                cust.id(),
                today,
                today,
                "AED",
                BigDecimal.ONE,
                "Sales Return Allowance",
                null,
                null,
                List.of(
                        new InvoiceLineRequest((short) 1, null, "Defective Goods Return", new BigDecimal("1"), new BigDecimal("200.00"), BigDecimal.ZERO, taxRate5PctId, revenueAccountId, null, null)
                )
        ));

        // Subtotal = 200.00, Tax = 10.00, Total = 210.00
        assertEquals(new BigDecimal("210.00"), creditNoteDraft.total());

        InvoiceDto postedCrn = invoiceService.sendInvoice(creditNoteDraft.id());
        assertEquals(InvoiceStatus.SENT, postedCrn.status());
        assertTrue(postedCrn.invoiceNo().startsWith("CRN-"));
        assertNotNull(postedCrn.journalEntryId());

        JournalEntry journal = journalEntryRepository.findById(postedCrn.journalEntryId()).orElseThrow();
        assertEquals(new BigDecimal("210.00"), journal.getTotalDebit());
        assertEquals(new BigDecimal("210.00"), journal.getTotalCredit());

        // Verify Credit Note debited Revenue & VAT, credited AR
        JournalLine revLine = journal.getLines().stream().filter(l -> l.getAccountId().equals(revenueAccountId)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("200.00"), revLine.getDebit());

        JournalLine vatLine = journal.getLines().stream().filter(l -> l.getAccountId().equals(vatAccountId)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("10.00"), vatLine.getDebit());

        JournalLine arLine = journal.getLines().stream().filter(l -> l.getAccountId().equals(arAccountId)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("210.00"), arLine.getCredit());
    }

    @Test
    @Transactional
    @DisplayName("5. Receipt Allocation: Validation, status transition (PARTIAL -> PAID), and GL posting")
    void testReceiptAllocationAndInvoiceStatusTransition() {
        CustomerDto cust = createTestCustomer("CUST-RCT-1");

        // Create and send invoice for 1000.00 (tax inclusive 1050.00)
        InvoiceDto inv = invoiceService.createInvoice(new InvoiceRequest(
                InvoiceDocType.INVOICE,
                cust.id(),
                today,
                today.plusDays(30),
                "AED",
                BigDecimal.ONE,
                "Services",
                null,
                null,
                List.of(
                        new InvoiceLineRequest((short) 1, null, "Line 1", new BigDecimal("1"), new BigDecimal("1000.00"), BigDecimal.ZERO, taxRate5PctId, revenueAccountId, null, null)
                )
        ));
        inv = invoiceService.sendInvoice(inv.id());
        final Long invId = inv.id();
        assertEquals(new BigDecimal("1050.00"), inv.balanceDue());

        // Part 1: Partial payment of 400.00
        ReceiptDto receipt1 = receiptService.createReceipt(new ReceiptRequest(
                cust.id(),
                today,
                bankAccountId,
                PaymentMethod.BANK_TRANSFER,
                "WIRE-001",
                "AED",
                new BigDecimal("400.00"),
                List.of(new ReceiptAllocationRequest(invId, new BigDecimal("400.00")))
        ));
        assertEquals(ReceiptStatus.POSTED, receipt1.status());
        assertEquals(new BigDecimal("0.00"), receipt1.unappliedAmount());

        // Check invoice status became PARTIAL
        Invoice updatedInv = invoiceRepository.findById(invId).orElseThrow();
        assertEquals(InvoiceStatus.PARTIAL, updatedInv.getStatus());
        assertEquals(new BigDecimal("400.00"), updatedInv.getAmountPaid());
        assertEquals(new BigDecimal("650.00"), updatedInv.getBalanceDue());

        // Part 2: Prevent Over-allocation
        BusinessException overAllocEx = assertThrows(BusinessException.class, () ->
                receiptService.createReceipt(new ReceiptRequest(
                        cust.id(),
                        today,
                        bankAccountId,
                        PaymentMethod.BANK_TRANSFER,
                        "WIRE-002",
                        "AED",
                        new BigDecimal("700.00"),
                        List.of(new ReceiptAllocationRequest(invId, new BigDecimal("700.00"))) // Exceeds 650.00 balance due!
                )));
        assertEquals("OVER_ALLOCATION", overAllocEx.getCode());

        // Part 3: Final payment of 650.00 + 50.00 advance unapplied (Receipt amount 700.00)
        ReceiptDto receipt2 = receiptService.createReceipt(new ReceiptRequest(
                cust.id(),
                today,
                bankAccountId,
                PaymentMethod.CARD,
                "AUTH-999",
                "AED",
                new BigDecimal("700.00"),
                List.of(new ReceiptAllocationRequest(invId, new BigDecimal("650.00")))
        ));
        assertEquals(new BigDecimal("50.00"), receipt2.unappliedAmount());

        // Check invoice status became PAID
        Invoice fullyPaidInv = invoiceRepository.findById(invId).orElseThrow();
        assertEquals(InvoiceStatus.PAID, fullyPaidInv.getStatus());
        assertEquals(new BigDecimal("1050.00"), fullyPaidInv.getAmountPaid());
        assertEquals(new BigDecimal("0.00"), fullyPaidInv.getBalanceDue());

        // Check GL posting for receipt2 (Dr Bank 700.00, Cr AR 700.00)
        JournalEntry rctJournal = journalEntryRepository.findById(receipt2.journalEntryId()).orElseThrow();
        assertEquals(new BigDecimal("700.00"), rctJournal.getTotalDebit());
        assertEquals(new BigDecimal("700.00"), rctJournal.getTotalCredit());

        JournalLine bankLine = rctJournal.getLines().stream().filter(l -> l.getAccountId().equals(bankGlAccountId)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("700.00"), bankLine.getDebit());

        JournalLine arLine = rctJournal.getLines().stream().filter(l -> l.getAccountId().equals(arAccountId)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("700.00"), arLine.getCredit());
    }

    @Test
    @Transactional
    @DisplayName("6. Receipt Voiding: Restores invoice balance and status, reverses GL journal")
    void testReceiptVoiding() {
        CustomerDto cust = createTestCustomer("CUST-VOID-RCT");

        InvoiceDto inv = invoiceService.createInvoice(new InvoiceRequest(
                InvoiceDocType.INVOICE,
                cust.id(),
                today,
                today.plusDays(30),
                "AED",
                BigDecimal.ONE,
                "Consulting",
                null,
                null,
                List.of(new InvoiceLineRequest((short) 1, null, "Item", new BigDecimal("1"), new BigDecimal("500.00"), BigDecimal.ZERO, null, revenueAccountId, null, null))
        ));
        inv = invoiceService.sendInvoice(inv.id());
        assertEquals(new BigDecimal("500.00"), inv.balanceDue());

        ReceiptDto receipt = receiptService.createReceipt(new ReceiptRequest(
                cust.id(),
                today,
                bankAccountId,
                PaymentMethod.CASH,
                null,
                "AED",
                new BigDecimal("500.00"),
                List.of(new ReceiptAllocationRequest(inv.id(), new BigDecimal("500.00")))
        ));

        // Invoice should be PAID
        Invoice paidInv = invoiceRepository.findById(inv.id()).orElseThrow();
        assertEquals(InvoiceStatus.PAID, paidInv.getStatus());

        // Void the receipt
        ReceiptDto voided = receiptService.voidReceipt(receipt.id(), "Customer cheque bounced");
        assertEquals(ReceiptStatus.VOID, voided.status());

        // Invoice balance and status should be restored back to SENT and 500.00 due
        Invoice restoredInv = invoiceRepository.findById(inv.id()).orElseThrow();
        assertEquals(InvoiceStatus.SENT, restoredInv.getStatus());
        assertEquals(new BigDecimal("500.00"), restoredInv.getBalanceDue());
        assertEquals(new BigDecimal("0.00"), restoredInv.getAmountPaid());

        // Original receipt journal should be marked VOID
        JournalEntry origJournal = journalEntryRepository.findById(receipt.journalEntryId()).orElseThrow();
        assertEquals(JournalStatus.VOID, origJournal.getStatus());
    }

    @Test
    @Transactional
    @DisplayName("7. Invoice Voiding: Blocks voiding paid invoices, reverses GL for unpaid invoices")
    void testInvoiceVoiding() {
        CustomerDto cust = createTestCustomer("CUST-VOID-INV");

        InvoiceDto inv = invoiceService.createInvoice(new InvoiceRequest(
                InvoiceDocType.INVOICE,
                cust.id(),
                today,
                today.plusDays(30),
                "AED",
                BigDecimal.ONE,
                "Services",
                null,
                null,
                List.of(new InvoiceLineRequest((short) 1, null, "Item", new BigDecimal("1"), new BigDecimal("300.00"), BigDecimal.ZERO, null, revenueAccountId, null, null))
        ));
        inv = invoiceService.sendInvoice(inv.id());

        // Pay 100.00 towards the invoice
        ReceiptDto receipt = receiptService.createReceipt(new ReceiptRequest(
                cust.id(),
                today,
                bankAccountId,
                PaymentMethod.CASH,
                null,
                "AED",
                new BigDecimal("100.00"),
                List.of(new ReceiptAllocationRequest(inv.id(), new BigDecimal("100.00")))
        ));

        // Attempting to void the partially paid invoice should be rejected
        final Long invId = inv.id();
        BusinessException ex = assertThrows(BusinessException.class, () -> invoiceService.voidInvoice(invId, "Cancelled contract"));
        assertEquals("CANNOT_VOID_PAID_INVOICE", ex.getCode());

        // Void the receipt first
        receiptService.voidReceipt(receipt.id(), "Mistake");

        // Now voiding the unpaid invoice should succeed
        InvoiceDto voided = invoiceService.voidInvoice(invId, "Cancelled contract");
        assertEquals(InvoiceStatus.VOID, voided.status());
        assertEquals(new BigDecimal("0.00"), voided.balanceDue());

        // The invoice journal entry should be marked VOID
        JournalEntry journal = journalEntryRepository.findById(inv.journalEntryId()).orElseThrow();
        assertEquals(JournalStatus.VOID, journal.getStatus());
    }

    @Test
    @Transactional
    @DisplayName("8. Open Invoices Lookup: Only returns unpaid/partially paid sent invoices")
    void testOpenInvoicesLookup() {
        CustomerDto cust = createTestCustomer("CUST-OPEN-1");

        // Create 2 invoices: 1 sent, 1 paid
        InvoiceDto inv1 = invoiceService.createInvoice(new InvoiceRequest(
                InvoiceDocType.INVOICE, cust.id(), today, today.plusDays(30), "AED", BigDecimal.ONE, "Inv 1", null, null,
                List.of(new InvoiceLineRequest((short) 1, null, "Item", BigDecimal.ONE, new BigDecimal("100.00"), BigDecimal.ZERO, null, revenueAccountId, null, null))
        ));
        inv1 = invoiceService.sendInvoice(inv1.id());

        InvoiceDto inv2 = invoiceService.createInvoice(new InvoiceRequest(
                InvoiceDocType.INVOICE, cust.id(), today, today.plusDays(30), "AED", BigDecimal.ONE, "Inv 2", null, null,
                List.of(new InvoiceLineRequest((short) 1, null, "Item", BigDecimal.ONE, new BigDecimal("200.00"), BigDecimal.ZERO, null, revenueAccountId, null, null))
        ));
        inv2 = invoiceService.sendInvoice(inv2.id());

        // Pay inv2 in full
        receiptService.createReceipt(new ReceiptRequest(
                cust.id(), today, bankAccountId, PaymentMethod.CASH, null, "AED", new BigDecimal("200.00"),
                List.of(new ReceiptAllocationRequest(inv2.id(), new BigDecimal("200.00")))
        ));

        List<OpenInvoiceDto> openInvoices = customerService.getOpenInvoices(cust.id());
        assertEquals(1, openInvoices.size());
        assertEquals(inv1.id(), openInvoices.get(0).id());
        assertEquals(new BigDecimal("100.00"), openInvoices.get(0).balanceDue());
    }

    @Test
    @Transactional
    @DisplayName("9. Customer Statement: Chronological transactions and running balance")
    void testCustomerStatement() {
        CustomerDto cust = createTestCustomer("CUST-STMT-1");

        // Transaction 1: Invoice for 500.00 on today - 10
        InvoiceDto inv1 = invoiceService.createInvoice(new InvoiceRequest(
                InvoiceDocType.INVOICE, cust.id(), today.minusDays(10), today.plusDays(20), "AED", BigDecimal.ONE, "Inv 1", null, null,
                List.of(new InvoiceLineRequest((short) 1, null, "Item", BigDecimal.ONE, new BigDecimal("500.00"), BigDecimal.ZERO, null, revenueAccountId, null, null))
        ));
        invoiceService.sendInvoice(inv1.id());

        // Transaction 2: Receipt for 200.00 on today - 5
        receiptService.createReceipt(new ReceiptRequest(
                cust.id(), today.minusDays(5), bankAccountId, PaymentMethod.BANK_TRANSFER, "REF-1", "AED", new BigDecimal("200.00"),
                List.of(new ReceiptAllocationRequest(inv1.id(), new BigDecimal("200.00")))
        ));

        // Transaction 3: Invoice for 300.00 on today
        InvoiceDto inv2 = invoiceService.createInvoice(new InvoiceRequest(
                InvoiceDocType.INVOICE, cust.id(), today, today.plusDays(30), "AED", BigDecimal.ONE, "Inv 2", null, null,
                List.of(new InvoiceLineRequest((short) 1, null, "Item", BigDecimal.ONE, new BigDecimal("300.00"), BigDecimal.ZERO, null, revenueAccountId, null, null))
        ));
        invoiceService.sendInvoice(inv2.id());

        CustomerStatementDto stmt = customerService.getCustomerStatement(cust.id(), today.minusDays(15), today);
        assertEquals(cust.id(), stmt.customerId());
        assertEquals(new BigDecimal("0.00"), stmt.openingBalance());
        assertEquals(new BigDecimal("600.00"), stmt.closingBalance()); // 500 - 200 + 300 = 600
        assertEquals(3, stmt.lines().size());

        // Line 1: Debit 500, Running 500
        assertEquals(new BigDecimal("500.00"), stmt.lines().get(0).debit());
        assertEquals(new BigDecimal("500.00"), stmt.lines().get(0).runningBalance());

        // Line 2: Credit 200, Running 300
        assertEquals(new BigDecimal("200.00"), stmt.lines().get(1).credit());
        assertEquals(new BigDecimal("300.00"), stmt.lines().get(1).runningBalance());

        // Line 3: Debit 300, Running 600
        assertEquals(new BigDecimal("300.00"), stmt.lines().get(2).debit());
        assertEquals(new BigDecimal("600.00"), stmt.lines().get(2).runningBalance());
    }

    @Test
    @Transactional
    @DisplayName("10. AR Ageing Report: Accurate aging buckets (Current, 1-30, 31-60, 61-90, 90+)")
    void testArAgeingReport() {
        CustomerDto cust = createTestCustomer("CUST-AGE-1");

        // Inv 1: Due in 10 days (Current) -> 100.00
        createAndSendInvoice(cust.id(), today.minusDays(10), today.plusDays(10), new BigDecimal("100.00"));

        // Inv 2: Due 15 days ago (1-30 days) -> 200.00
        createAndSendInvoice(cust.id(), today.minusDays(45), today.minusDays(15), new BigDecimal("200.00"));

        // Inv 3: Due 45 days ago (31-60 days) -> 300.00
        createAndSendInvoice(cust.id(), today.minusDays(75), today.minusDays(45), new BigDecimal("300.00"));

        // Inv 4: Due 100 days ago (90+ days) -> 400.00
        createAndSendInvoice(cust.id(), today.minusDays(130), today.minusDays(100), new BigDecimal("400.00"));

        ArAgeingReportDto report = arAgeingService.getArAgeingReport(today);
        assertNotNull(report);

        ArAgeingCustomerDto custRow = report.customers().stream()
                .filter(c -> c.customerId().equals(cust.id()))
                .findFirst().orElseThrow();

        assertEquals(new BigDecimal("100.00"), custRow.current());
        assertEquals(new BigDecimal("200.00"), custRow.days1To30());
        assertEquals(new BigDecimal("300.00"), custRow.days31To60());
        assertEquals(new BigDecimal("0.00"), custRow.days61To90());
        assertEquals(new BigDecimal("400.00"), custRow.daysOver90());
        assertEquals(new BigDecimal("1000.00"), custRow.totalOutstanding());
    }

    @Test
    @Transactional
    @DisplayName("11. Invoice PDF Generation: Produces valid PDF document bytes")
    void testInvoicePdfGeneration() {
        CustomerDto cust = createTestCustomer("CUST-PDF-1");
        InvoiceDto inv = invoiceService.createInvoice(new InvoiceRequest(
                InvoiceDocType.INVOICE, cust.id(), today, today.plusDays(30), "AED", BigDecimal.ONE, "Tax invoice for consulting", null, null,
                List.of(new InvoiceLineRequest((short) 1, null, "Consulting", BigDecimal.ONE, new BigDecimal("1000.00"), BigDecimal.ZERO, taxRate5PctId, revenueAccountId, null, null))
        ));
        inv = invoiceService.sendInvoice(inv.id());

        byte[] pdfBytes = invoiceService.generateInvoicePdf(inv.id());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF bytes should be non-trivial");

        // Verify PDF Magic Bytes (%PDF-)
        String header = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", header);
    }

    @Test
    @Transactional
    @DisplayName("12. Tenant Isolation: Company ACME cannot view or modify Company BSB's sales records")
    void testTenantIsolation() {
        CustomerDto bsbCust = createTestCustomer("CUST-ISO-BSB");
        InvoiceDto bsbInv = invoiceService.createInvoice(new InvoiceRequest(
                InvoiceDocType.INVOICE, bsbCust.id(), today, today.plusDays(30), "AED", BigDecimal.ONE, "BSB Invoice", null, null,
                List.of(new InvoiceLineRequest((short) 1, null, "Item", BigDecimal.ONE, new BigDecimal("100.00"), BigDecimal.ZERO, null, revenueAccountId, null, null))
        ));

        // Switch to ACME company
        authenticateAs(2L, acmeCompanyId, "acme_admin@finledger.com", Set.of(
                "CUSTOMER_VIEW", "CUSTOMER_CREATE", "CUSTOMER_EDIT",
                "INVOICE_VIEW", "INVOICE_CREATE", "INVOICE_SEND", "INVOICE_VOID",
                "RECEIPT_VIEW", "RECEIPT_CREATE", "RECEIPT_VOID", "REPORT_VIEW"
        ));

        // Querying BSB customer from ACME context must fail with CUSTOMER_NOT_FOUND
        BusinessException custEx = assertThrows(BusinessException.class, () ->
                customerService.getCustomerById(bsbCust.id()));
        assertEquals("CUSTOMER_NOT_FOUND", custEx.getCode());

        // Querying BSB invoice from ACME context must fail with INVOICE_NOT_FOUND
        BusinessException invEx = assertThrows(BusinessException.class, () ->
                invoiceService.getInvoiceById(bsbInv.id()));
        assertEquals("INVOICE_NOT_FOUND", invEx.getCode());

        // Creating invoice under ACME targeting BSB's customer must fail
        BusinessException targetEx = assertThrows(BusinessException.class, () ->
                invoiceService.createInvoice(new InvoiceRequest(
                        InvoiceDocType.INVOICE, bsbCust.id(), today, today.plusDays(30), "AED", BigDecimal.ONE, "Malicious", null, null,
                        List.of(new InvoiceLineRequest((short) 1, null, "Item", BigDecimal.ONE, new BigDecimal("100.00"), BigDecimal.ZERO, null, revenueAccountId, null, null))
                )));
        assertEquals("CUSTOMER_NOT_FOUND", targetEx.getCode());
    }

    private CustomerDto createTestCustomer(String code) {
        return customerService.createCustomer(new CustomerRequest(
                code,
                "Customer " + code,
                "100200300400003",
                code.toLowerCase() + "@customer.com",
                "+97140000000",
                "Business Bay, Dubai",
                "AE",
                "AED",
                (short) 30,
                new BigDecimal("50000.00"),
                arAccountId,
                null,
                null,
                true
        ));
    }

    private InvoiceDto createAndSendInvoice(Long customerId, LocalDate invoiceDate, LocalDate dueDate, BigDecimal amount) {
        InvoiceDto draft = invoiceService.createInvoice(new InvoiceRequest(
                InvoiceDocType.INVOICE,
                customerId,
                invoiceDate,
                dueDate,
                "AED",
                BigDecimal.ONE,
                "Invoice for " + amount,
                null,
                null,
                List.of(new InvoiceLineRequest((short) 1, null, "Item", BigDecimal.ONE, amount, BigDecimal.ZERO, null, revenueAccountId, null, null))
        ));
        return invoiceService.sendInvoice(draft.id());
    }
}
