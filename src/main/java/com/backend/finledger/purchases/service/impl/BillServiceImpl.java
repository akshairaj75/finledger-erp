package com.backend.finledger.purchases.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.DocType;
import com.backend.finledger.common.NumberSeriesService;
import com.backend.finledger.common.PageResponse;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.ledger.dto.JournalEntryDto;
import com.backend.finledger.ledger.dto.JournalLineRequest;
import com.backend.finledger.ledger.dto.SourceDocumentPostingRequest;
import com.backend.finledger.ledger.entity.Account;
import com.backend.finledger.ledger.entity.JournalSourceType;
import com.backend.finledger.ledger.entity.PartyType;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.ledger.service.PostingService;
import com.backend.finledger.purchases.dto.BillDto;
import com.backend.finledger.purchases.dto.BillLineRequest;
import com.backend.finledger.purchases.dto.BillRequest;
import com.backend.finledger.purchases.entity.Bill;
import com.backend.finledger.purchases.entity.BillDocType;
import com.backend.finledger.purchases.entity.BillLine;
import com.backend.finledger.purchases.entity.BillStatus;
import com.backend.finledger.purchases.entity.Vendor;
import com.backend.finledger.purchases.mapper.PurchasesMapper;
import com.backend.finledger.purchases.repository.BillRepository;
import com.backend.finledger.purchases.repository.VendorRepository;
import com.backend.finledger.purchases.service.BillService;
import com.backend.finledger.security.CurrentUser;
import com.backend.finledger.tax.entity.TaxRate;
import com.backend.finledger.tax.repository.TaxRateRepository;
import com.backend.finledger.tax.service.TaxCalculator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BillServiceImpl implements BillService {

    private final BillRepository billRepository;
    private final VendorRepository vendorRepository;
    private final TaxRateRepository taxRateRepository;
    private final AccountRepository accountRepository;
    private final CompanyRepository companyRepository;
    private final PostingService postingService;
    private final NumberSeriesService numberSeriesService;
    private final TaxCalculator taxCalculator;
    private final PurchasesMapper purchasesMapper;

    public BillServiceImpl(BillRepository billRepository,
                           VendorRepository vendorRepository,
                           TaxRateRepository taxRateRepository,
                           AccountRepository accountRepository,
                           CompanyRepository companyRepository,
                           PostingService postingService,
                           NumberSeriesService numberSeriesService,
                           TaxCalculator taxCalculator,
                           PurchasesMapper purchasesMapper) {
        this.billRepository = billRepository;
        this.vendorRepository = vendorRepository;
        this.taxRateRepository = taxRateRepository;
        this.accountRepository = accountRepository;
        this.companyRepository = companyRepository;
        this.postingService = postingService;
        this.numberSeriesService = numberSeriesService;
        this.taxCalculator = taxCalculator;
        this.purchasesMapper = purchasesMapper;
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('BILL_CREATE')")
    public BillDto createBill(BillRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        Vendor vendor = vendorRepository.findByCompanyIdAndId(companyId, request.vendorId())
                .orElseThrow(() -> new BusinessException("VENDOR_NOT_FOUND", "Vendor not found with id: " + request.vendorId()));

        if (request.vendorInvoiceNo() != null && !request.vendorInvoiceNo().trim().isEmpty()) {
            if (billRepository.existsByVendorIdAndVendorInvoiceNo(vendor.getId(), request.vendorInvoiceNo().trim())) {
                throw new BusinessException("DUPLICATE_VENDOR_INVOICE_NO",
                        "Vendor invoice number '" + request.vendorInvoiceNo() + "' already exists for vendor " + vendor.getName());
            }
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Company not found"));

        Bill bill = new Bill();
        bill.setCompanyId(companyId);
        bill.setBillNo("DRAFT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        bill.setVendorInvoiceNo(request.vendorInvoiceNo() != null ? request.vendorInvoiceNo().trim() : null);
        bill.setDocType(request.docType() != null ? request.docType() : BillDocType.BILL);
        bill.setVendorId(vendor.getId());
        bill.setBillDate(request.billDate());

        LocalDate dueDate = request.dueDate();
        if (dueDate == null) {
            short terms = vendor.getPaymentTermsDays() != null ? vendor.getPaymentTermsDays() : 30;
            dueDate = request.billDate().plusDays(terms);
        }
        bill.setDueDate(dueDate);

        String currency = request.currencyCode() != null ? request.currencyCode() : vendor.getCurrencyCode();
        if (currency == null) {
            currency = company.getBaseCurrency();
        }
        bill.setCurrencyCode(currency);

        BigDecimal exchangeRate = (request.exchangeRate() != null && request.exchangeRate().compareTo(BigDecimal.ZERO) > 0)
                ? request.exchangeRate()
                : BigDecimal.ONE;
        bill.setExchangeRate(exchangeRate);
        bill.setStatus(BillStatus.DRAFT);
        bill.setNotes(request.notes());

        populateAndCalculateLines(bill, request.lines(), companyId);

        Bill saved = billRepository.save(bill);
        return mapToDtoWithVendor(saved, vendor);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('BILL_CREATE')")
    public BillDto updateBill(Long id, BillRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        Bill bill = billRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("BILL_NOT_FOUND", "Bill not found with id: " + id));

        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BusinessException("BILL_NOT_EDITABLE", "Only draft bills can be edited. Current status: " + bill.getStatus());
        }

        Vendor vendor = vendorRepository.findByCompanyIdAndId(companyId, request.vendorId())
                .orElseThrow(() -> new BusinessException("VENDOR_NOT_FOUND", "Vendor not found with id: " + request.vendorId()));

        if (request.vendorInvoiceNo() != null && !request.vendorInvoiceNo().trim().isEmpty()) {
            if (billRepository.existsByVendorIdAndVendorInvoiceNoAndIdNot(vendor.getId(), request.vendorInvoiceNo().trim(), id)) {
                throw new BusinessException("DUPLICATE_VENDOR_INVOICE_NO",
                        "Vendor invoice number '" + request.vendorInvoiceNo() + "' already exists for vendor " + vendor.getName());
            }
        }

        bill.setDocType(request.docType() != null ? request.docType() : bill.getDocType());
        bill.setVendorInvoiceNo(request.vendorInvoiceNo() != null ? request.vendorInvoiceNo().trim() : null);
        bill.setVendorId(vendor.getId());
        bill.setBillDate(request.billDate());

        LocalDate dueDate = request.dueDate();
        if (dueDate == null) {
            short terms = vendor.getPaymentTermsDays() != null ? vendor.getPaymentTermsDays() : 30;
            dueDate = request.billDate().plusDays(terms);
        }
        bill.setDueDate(dueDate);

        if (request.currencyCode() != null) {
            bill.setCurrencyCode(request.currencyCode());
        }
        if (request.exchangeRate() != null && request.exchangeRate().compareTo(BigDecimal.ZERO) > 0) {
            bill.setExchangeRate(request.exchangeRate());
        }
        bill.setNotes(request.notes());

        bill.clearLines();
        populateAndCalculateLines(bill, request.lines(), companyId);

        Bill saved = billRepository.save(bill);
        return mapToDtoWithVendor(saved, vendor);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('BILL_VIEW')")
    public BillDto getBillById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Bill bill = billRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("BILL_NOT_FOUND", "Bill not found with id: " + id));

        Vendor vendor = vendorRepository.findByCompanyIdAndId(companyId, bill.getVendorId()).orElse(null);
        return mapToDtoWithVendor(bill, vendor);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('BILL_VIEW')")
    public PageResponse<BillDto> getBills(Long vendorId, Pageable pageable) {
        Long companyId = CurrentUser.getCompanyId();
        Page<Bill> page;
        if (vendorId != null) {
            page = billRepository.findByCompanyIdAndVendorId(companyId, vendorId, pageable);
        } else {
            page = billRepository.findByCompanyId(companyId, pageable);
        }

        return PageResponse.from(page.map(b -> {
            Vendor vendor = vendorRepository.findByCompanyIdAndId(companyId, b.getVendorId()).orElse(null);
            return mapToDtoWithVendor(b, vendor);
        }));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('BILL_APPROVE')")
    public BillDto approveBill(Long id) {
        Long companyId = CurrentUser.getCompanyId();

        Bill bill = billRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("BILL_NOT_FOUND", "Bill not found with id: " + id));

        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BusinessException("INVALID_STATUS", "Only draft bills can be approved. Current status: " + bill.getStatus());
        }

        Vendor vendor = vendorRepository.findByCompanyIdAndId(companyId, bill.getVendorId())
                .orElseThrow(() -> new BusinessException("VENDOR_NOT_FOUND", "Vendor not found for id: " + bill.getVendorId()));

        DocType numberSeriesDocType = DocType.BILL;
        String generatedNo = numberSeriesService.generateNextNumber(companyId, numberSeriesDocType);
        bill.setBillNo(generatedNo);
        bill.setStatus(BillStatus.APPROVED);

        List<JournalLineRequest> journalLines = new ArrayList<>();

        if (bill.getDocType() == BillDocType.BILL) {
            // Posting Rule:
            // Dr Expense/Inventory per line (net amount)
            // Dr Input VAT (tax amount)
            // Cr Accounts Payable (total)
            for (BillLine line : bill.getLines()) {
                BigDecimal netAmount = line.getLineTotal().subtract(line.getTaxAmount());
                if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                    journalLines.add(new JournalLineRequest(
                            line.getAccountId(),
                            line.getCostCenterId(),
                            line.getDescription(),
                            netAmount,
                            BigDecimal.ZERO,
                            null,
                            null
                    ));
                }

                if (line.getTaxAmount().compareTo(BigDecimal.ZERO) > 0 && line.getTaxRateId() != null) {
                    TaxRate taxRate = taxRateRepository.findByCompanyIdAndId(companyId, line.getTaxRateId())
                            .orElseThrow(() -> new BusinessException("TAX_RATE_NOT_FOUND", "Tax rate not found for id: " + line.getTaxRateId()));

                    Long purchaseTaxAccountId = taxRate.getPurchaseAccountId();
                    if (purchaseTaxAccountId == null) {
                        throw new BusinessException("TAX_ACCOUNT_MISSING", "Tax rate " + taxRate.getCode() + " is missing a purchase GL account");
                    }

                    journalLines.add(new JournalLineRequest(
                            purchaseTaxAccountId,
                            line.getCostCenterId(),
                            "Input VAT (" + taxRate.getCode() + ") - " + bill.getBillNo(),
                            line.getTaxAmount(),
                            BigDecimal.ZERO,
                            null,
                            null
                    ));
                }
            }

            journalLines.add(new JournalLineRequest(
                    vendor.getApAccountId(),
                    null,
                    "AP - Bill #" + bill.getBillNo() + (bill.getVendorInvoiceNo() != null ? " (" + bill.getVendorInvoiceNo() + ")" : ""),
                    BigDecimal.ZERO,
                    bill.getTotal(),
                    PartyType.VENDOR,
                    vendor.getId()
            ));
        } else {
            // DEBIT NOTE
            // Dr Accounts Payable (total)
            // Cr Expense/Inventory per line (net amount)
            // Cr Input VAT (tax amount)
            journalLines.add(new JournalLineRequest(
                    vendor.getApAccountId(),
                    null,
                    "AP Debit Note #" + bill.getBillNo(),
                    bill.getTotal(),
                    BigDecimal.ZERO,
                    PartyType.VENDOR,
                    vendor.getId()
            ));

            for (BillLine line : bill.getLines()) {
                BigDecimal netAmount = line.getLineTotal().subtract(line.getTaxAmount());
                if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                    journalLines.add(new JournalLineRequest(
                            line.getAccountId(),
                            line.getCostCenterId(),
                            "Debit Note: " + line.getDescription(),
                            BigDecimal.ZERO,
                            netAmount,
                            null,
                            null
                    ));
                }

                if (line.getTaxAmount().compareTo(BigDecimal.ZERO) > 0 && line.getTaxRateId() != null) {
                    TaxRate taxRate = taxRateRepository.findByCompanyIdAndId(companyId, line.getTaxRateId())
                            .orElseThrow(() -> new BusinessException("TAX_RATE_NOT_FOUND", "Tax rate not found for id: " + line.getTaxRateId()));

                    Long purchaseTaxAccountId = taxRate.getPurchaseAccountId();
                    if (purchaseTaxAccountId == null) {
                        throw new BusinessException("TAX_ACCOUNT_MISSING", "Tax rate " + taxRate.getCode() + " is missing a purchase GL account");
                    }

                    journalLines.add(new JournalLineRequest(
                            purchaseTaxAccountId,
                            line.getCostCenterId(),
                            "Input VAT Reversal (" + taxRate.getCode() + ") - " + bill.getBillNo(),
                            BigDecimal.ZERO,
                            line.getTaxAmount(),
                            null,
                            null
                    ));
                }
            }
        }

        String reference = bill.getBillNo();
        if (bill.getVendorInvoiceNo() != null && !bill.getVendorInvoiceNo().isBlank()) {
            reference += " / " + bill.getVendorInvoiceNo();
        }

        SourceDocumentPostingRequest postingRequest = new SourceDocumentPostingRequest(
                JournalSourceType.BILL,
                bill.getId(),
                bill.getBillDate(),
                reference,
                bill.getDocType() + " #" + bill.getBillNo() + " - " + vendor.getName(),
                bill.getCurrencyCode(),
                bill.getExchangeRate(),
                journalLines
        );

        JournalEntryDto postedEntry = postingService.postDocument(postingRequest);
        bill.setJournalEntryId(postedEntry.id());

        Bill saved = billRepository.save(bill);
        return mapToDtoWithVendor(saved, vendor);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('BILL_VOID')")
    public BillDto voidBill(Long id, String reason) {
        Long companyId = CurrentUser.getCompanyId();

        Bill bill = billRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("BILL_NOT_FOUND", "Bill not found with id: " + id));

        if (bill.getStatus() == BillStatus.VOID) {
            throw new BusinessException("ALREADY_VOID", "Bill is already voided");
        }

        if (bill.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("CANNOT_VOID_PAID_BILL", "Cannot void bill with payments allocated. Void payments first.");
        }

        if (bill.getStatus() == BillStatus.APPROVED || bill.getStatus() == BillStatus.PARTIAL) {
            if (bill.getJournalEntryId() != null) {
                postingService.reverseJournalEntry(bill.getJournalEntryId(), reason != null ? reason : "Bill voided");
            }
        }

        bill.setStatus(BillStatus.VOID);
        bill.setBalanceDue(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        Bill saved = billRepository.save(bill);
        Vendor vendor = vendorRepository.findByCompanyIdAndId(companyId, bill.getVendorId()).orElse(null);
        return mapToDtoWithVendor(saved, vendor);
    }

    private void populateAndCalculateLines(Bill bill, List<BillLineRequest> lineRequests, Long companyId) {
        short lineNo = 1;
        List<TaxCalculator.CalculatedLine> calculatedLines = new ArrayList<>();

        for (BillLineRequest lr : lineRequests) {
            Account account = accountRepository.findByCompanyIdAndId(companyId, lr.accountId())
                    .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "Account not found for id: " + lr.accountId()));
            if (Boolean.FALSE.equals(account.getIsActive())) {
                throw new BusinessException("ACCOUNT_INACTIVE", "Cannot select inactive account: " + account.getCode());
            }
            if (Boolean.TRUE.equals(account.getIsGroup())) {
                throw new BusinessException("GROUP_ACCOUNT_NOT_ALLOWED", "Cannot post to group account: " + account.getCode());
            }

            BigDecimal taxRateValue = BigDecimal.ZERO;
            if (lr.taxRateId() != null) {
                TaxRate taxRate = taxRateRepository.findByCompanyIdAndId(companyId, lr.taxRateId())
                        .orElseThrow(() -> new BusinessException("TAX_RATE_NOT_FOUND", "Tax rate not found for id: " + lr.taxRateId()));
                if (Boolean.FALSE.equals(taxRate.getIsActive())) {
                    throw new BusinessException("TAX_RATE_INACTIVE", "Tax rate is inactive: " + taxRate.getCode());
                }
                taxRateValue = taxRate.getRate();
            }

            TaxCalculator.CalculatedLine calc = taxCalculator.calculateLine(
                    lr.quantity(),
                    lr.unitPrice(),
                    BigDecimal.ZERO,
                    taxRateValue
            );
            calculatedLines.add(calc);

            BillLine line = new BillLine();
            line.setLineNo(lineNo++);
            line.setItemId(lr.itemId());
            line.setDescription(lr.description());
            line.setQuantity(lr.quantity());
            line.setUnitPrice(lr.unitPrice().setScale(2, RoundingMode.HALF_UP));
            line.setTaxRateId(lr.taxRateId());
            line.setTaxAmount(calc.taxAmount());
            line.setLineTotal(calc.lineTotal());
            line.setAccountId(lr.accountId());
            line.setWarehouseId(lr.warehouseId());
            line.setCostCenterId(lr.costCenterId());

            bill.addLine(line);
        }

        TaxCalculator.CalculatedDocumentTotals totals = taxCalculator.calculateDocumentTotals(calculatedLines);
        bill.setSubtotal(totals.subtotal());
        bill.setTaxTotal(totals.taxTotal());
        bill.setTotal(totals.grandTotal());
        bill.setAmountPaid(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        bill.setBalanceDue(totals.grandTotal());
    }

    private BillDto mapToDtoWithVendor(Bill bill, Vendor vendor) {
        BillDto base = purchasesMapper.toBillDto(bill);
        return new BillDto(
                base.id(),
                base.companyId(),
                base.billNo(),
                base.vendorInvoiceNo(),
                base.docType(),
                base.vendorId(),
                vendor != null ? vendor.getName() : null,
                vendor != null ? vendor.getCode() : null,
                base.billDate(),
                base.dueDate(),
                base.currencyCode(),
                base.exchangeRate(),
                base.subtotal(),
                base.taxTotal(),
                base.total(),
                base.amountPaid(),
                base.balanceDue(),
                base.status(),
                base.journalEntryId(),
                base.notes(),
                base.lines()
        );
    }
}
