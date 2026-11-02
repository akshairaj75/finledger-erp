package com.backend.finledger.sales;

import com.backend.finledger.banking.BankAccount;
import com.backend.finledger.banking.BankAccountRepository;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.DocType;
import com.backend.finledger.common.NumberSeriesService;
import com.backend.finledger.common.PageResponse;
import com.backend.finledger.company.Company;
import com.backend.finledger.company.CompanyRepository;
import com.backend.finledger.ledger.JournalSourceType;
import com.backend.finledger.ledger.PartyType;
import com.backend.finledger.ledger.PostingService;
import com.backend.finledger.ledger.dto.JournalEntryDto;
import com.backend.finledger.ledger.dto.JournalLineRequest;
import com.backend.finledger.ledger.dto.SourceDocumentPostingRequest;
import com.backend.finledger.sales.dto.*;
import com.backend.finledger.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final BankAccountRepository bankAccountRepository;
    private final CompanyRepository companyRepository;
    private final PostingService postingService;
    private final NumberSeriesService numberSeriesService;
    private final SalesMapper salesMapper;

    public ReceiptService(ReceiptRepository receiptRepository,
                          CustomerRepository customerRepository,
                          InvoiceRepository invoiceRepository,
                          BankAccountRepository bankAccountRepository,
                          CompanyRepository companyRepository,
                          PostingService postingService,
                          NumberSeriesService numberSeriesService,
                          SalesMapper salesMapper) {
        this.receiptRepository = receiptRepository;
        this.customerRepository = customerRepository;
        this.invoiceRepository = invoiceRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.companyRepository = companyRepository;
        this.postingService = postingService;
        this.numberSeriesService = numberSeriesService;
        this.salesMapper = salesMapper;
    }

    @Transactional
    @PreAuthorize("hasAuthority('RECEIPT_CREATE')")
    public ReceiptDto createReceipt(ReceiptRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        Customer customer = customerRepository.findByCompanyIdAndId(companyId, request.customerId())
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found with id: " + request.customerId()));

        BankAccount bankAccount = bankAccountRepository.findByCompanyIdAndId(companyId, request.bankAccountId())
                .orElseThrow(() -> new BusinessException("BANK_ACCOUNT_NOT_FOUND", "Bank account not found with id: " + request.bankAccountId()));

        if (Boolean.FALSE.equals(bankAccount.getIsActive())) {
            throw new BusinessException("BANK_ACCOUNT_INACTIVE", "Bank account is inactive: " + bankAccount.getName());
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Company not found"));

        String currency = request.currencyCode() != null ? request.currencyCode() : customer.getCurrencyCode();
        if (currency == null) {
            currency = company.getBaseCurrency();
        }

        Receipt receipt = new Receipt();
        receipt.setCompanyId(companyId);
        receipt.setCustomerId(customer.getId());
        receipt.setReceiptDate(request.receiptDate());
        receipt.setBankAccountId(bankAccount.getId());
        receipt.setPaymentMethod(request.paymentMethod());
        receipt.setReference(request.reference());
        receipt.setCurrencyCode(currency);
        receipt.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));

        // Validate allocations
        BigDecimal totalAllocated = BigDecimal.ZERO;
        Set<Long> seenInvoiceIds = new HashSet<>();

        if (request.allocations() != null && !request.allocations().isEmpty()) {
            for (ReceiptAllocationRequest ar : request.allocations()) {
                if (!seenInvoiceIds.add(ar.invoiceId())) {
                    throw new BusinessException("DUPLICATE_ALLOCATION", "Duplicate allocation for invoice id: " + ar.invoiceId());
                }

                Invoice invoice = invoiceRepository.findByCompanyIdAndId(companyId, ar.invoiceId())
                        .orElseThrow(() -> new BusinessException("INVOICE_NOT_FOUND", "Invoice not found with id: " + ar.invoiceId()));

                if (!invoice.getCustomerId().equals(customer.getId())) {
                    throw new BusinessException("INVOICE_CUSTOMER_MISMATCH",
                            "Invoice #" + invoice.getInvoiceNo() + " does not belong to customer " + customer.getName());
                }

                if (invoice.getStatus() == InvoiceStatus.DRAFT || invoice.getStatus() == InvoiceStatus.VOID) {
                    throw new BusinessException("INVALID_INVOICE_STATUS",
                            "Cannot allocate payment to invoice #" + invoice.getInvoiceNo() + " with status: " + invoice.getStatus());
                }

                BigDecimal allocAmount = ar.amount().setScale(2, RoundingMode.HALF_UP);
                if (allocAmount.compareTo(invoice.getBalanceDue()) > 0) {
                    throw new BusinessException("OVER_ALLOCATION",
                            "Allocation amount (" + allocAmount + ") exceeds invoice #" + invoice.getInvoiceNo() + " balance due (" + invoice.getBalanceDue() + ")");
                }

                totalAllocated = totalAllocated.add(allocAmount);

                // Update invoice balance and status
                BigDecimal newPaid = invoice.getAmountPaid().add(allocAmount).setScale(2, RoundingMode.HALF_UP);
                BigDecimal newDue = invoice.getBalanceDue().subtract(allocAmount).setScale(2, RoundingMode.HALF_UP);
                invoice.setAmountPaid(newPaid);
                invoice.setBalanceDue(newDue);

                if (newDue.compareTo(BigDecimal.ZERO) == 0) {
                    invoice.setStatus(InvoiceStatus.PAID);
                } else {
                    invoice.setStatus(InvoiceStatus.PARTIAL);
                }

                invoiceRepository.save(invoice);

                ReceiptAllocation allocation = new ReceiptAllocation(receipt, invoice.getId(), allocAmount);
                receipt.addAllocation(allocation);
            }
        }

        if (totalAllocated.compareTo(receipt.getAmount()) > 0) {
            throw new BusinessException("EXCEEDS_RECEIPT_AMOUNT",
                    "Total allocated (" + totalAllocated + ") cannot exceed receipt amount (" + receipt.getAmount() + ")");
        }

        BigDecimal unapplied = receipt.getAmount().subtract(totalAllocated).setScale(2, RoundingMode.HALF_UP);
        receipt.setUnappliedAmount(unapplied);

        // Generate gap-free receipt number
        String receiptNo = numberSeriesService.generateNextNumber(companyId, DocType.RECEIPT);
        receipt.setReceiptNo(receiptNo);
        receipt.setStatus(ReceiptStatus.POSTED);

        // Double-entry GL posting:
        // Dr: Bank/Cash GL Account (receipt amount)
        // Cr: Accounts Receivable GL Account (receipt amount)
        List<JournalLineRequest> journalLines = List.of(
                new JournalLineRequest(
                        bankAccount.getGlAccountId(),
                        null,
                        "Receipt #" + receipt.getReceiptNo() + " via " + receipt.getPaymentMethod(),
                        receipt.getAmount(),
                        BigDecimal.ZERO,
                        null,
                        null
                ),
                new JournalLineRequest(
                        customer.getArAccountId(),
                        null,
                        "Receipt #" + receipt.getReceiptNo() + " - " + customer.getName(),
                        BigDecimal.ZERO,
                        receipt.getAmount(),
                        PartyType.CUSTOMER,
                        customer.getId()
                )
        );

        SourceDocumentPostingRequest postingRequest = new SourceDocumentPostingRequest(
                JournalSourceType.RECEIPT,
                null, // will be linked or updated
                receipt.getReceiptDate(),
                receipt.getReceiptNo(),
                "Customer Receipt " + receipt.getReceiptNo() + " - " + customer.getName(),
                receipt.getCurrencyCode(),
                BigDecimal.ONE,
                journalLines
        );

        JournalEntryDto postedJournal = postingService.postDocument(postingRequest);
        receipt.setJournalEntryId(postedJournal.id());

        Receipt saved = receiptRepository.save(receipt);
        return mapToDto(saved, customer, bankAccount);
    }

    @Transactional
    @PreAuthorize("hasAuthority('RECEIPT_VOID')")
    public ReceiptDto voidReceipt(Long id, String reason) {
        Long companyId = CurrentUser.getCompanyId();

        Receipt receipt = receiptRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("RECEIPT_NOT_FOUND", "Receipt not found with id: " + id));

        if (receipt.getStatus() == ReceiptStatus.VOID) {
            throw new BusinessException("ALREADY_VOID", "Receipt is already void");
        }

        // Restore invoice balances
        for (ReceiptAllocation alloc : receipt.getAllocations()) {
            Invoice invoice = invoiceRepository.findByCompanyIdAndId(companyId, alloc.getInvoiceId()).orElse(null);
            if (invoice != null) {
                BigDecimal restoredDue = invoice.getBalanceDue().add(alloc.getAmount()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal restoredPaid = invoice.getAmountPaid().subtract(alloc.getAmount()).setScale(2, RoundingMode.HALF_UP);
                invoice.setBalanceDue(restoredDue);
                invoice.setAmountPaid(restoredPaid);

                if (restoredDue.compareTo(invoice.getTotal()) == 0) {
                    invoice.setStatus(InvoiceStatus.SENT);
                } else {
                    invoice.setStatus(InvoiceStatus.PARTIAL);
                }

                invoiceRepository.save(invoice);
            }
        }

        // Reverse journal entry
        if (receipt.getJournalEntryId() != null) {
            postingService.reverseJournalEntry(receipt.getJournalEntryId(),
                    "Voiding Receipt " + receipt.getReceiptNo() + (reason != null ? ": " + reason : ""));
        }

        receipt.setStatus(ReceiptStatus.VOID);
        receipt.setUnappliedAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        Receipt saved = receiptRepository.save(receipt);
        Customer customer = customerRepository.findByCompanyIdAndId(companyId, receipt.getCustomerId()).orElse(null);
        BankAccount bankAccount = bankAccountRepository.findByCompanyIdAndId(companyId, receipt.getBankAccountId()).orElse(null);
        return mapToDto(saved, customer, bankAccount);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('RECEIPT_VIEW')")
    public ReceiptDto getReceiptById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Receipt receipt = receiptRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("RECEIPT_NOT_FOUND", "Receipt not found with id: " + id));

        Customer customer = customerRepository.findByCompanyIdAndId(companyId, receipt.getCustomerId()).orElse(null);
        BankAccount bankAccount = bankAccountRepository.findByCompanyIdAndId(companyId, receipt.getBankAccountId()).orElse(null);
        return mapToDto(receipt, customer, bankAccount);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('RECEIPT_VIEW')")
    public PageResponse<ReceiptDto> getReceipts(Long customerId, Pageable pageable) {
        Long companyId = CurrentUser.getCompanyId();
        Page<Receipt> page;
        if (customerId != null) {
            page = receiptRepository.findByCompanyIdAndCustomerId(companyId, customerId, pageable);
        } else {
            page = receiptRepository.findByCompanyId(companyId, pageable);
        }

        return PageResponse.from(page.map(r -> {
            Customer cust = customerRepository.findByCompanyIdAndId(companyId, r.getCustomerId()).orElse(null);
            BankAccount bank = bankAccountRepository.findByCompanyIdAndId(companyId, r.getBankAccountId()).orElse(null);
            return mapToDto(r, cust, bank);
        }));
    }

    private ReceiptDto mapToDto(Receipt receipt, Customer customer, BankAccount bankAccount) {
        ReceiptDto dto = salesMapper.toReceiptDto(receipt);
        String custName = customer != null ? customer.getName() : null;
        String custCode = customer != null ? customer.getCode() : null;
        String bankName = bankAccount != null ? bankAccount.getName() : null;

        List<ReceiptAllocationDto> allocationDtos = new ArrayList<>();
        for (ReceiptAllocation alloc : receipt.getAllocations()) {
            Invoice inv = invoiceRepository.findById(alloc.getInvoiceId()).orElse(null);
            String invNo = inv != null ? inv.getInvoiceNo() : null;
            allocationDtos.add(new ReceiptAllocationDto(alloc.getId(), alloc.getInvoiceId(), invNo, alloc.getAmount()));
        }

        return new ReceiptDto(
                dto.id(),
                dto.companyId(),
                dto.receiptNo(),
                dto.customerId(),
                custName,
                custCode,
                dto.receiptDate(),
                dto.bankAccountId(),
                bankName,
                dto.paymentMethod(),
                dto.reference(),
                dto.currencyCode(),
                dto.amount(),
                dto.unappliedAmount(),
                dto.status(),
                dto.journalEntryId(),
                allocationDtos
        );
    }
}

