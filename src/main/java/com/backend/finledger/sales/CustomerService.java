package com.backend.finledger.sales;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.PageResponse;
import com.backend.finledger.ledger.Account;
import com.backend.finledger.ledger.AccountRepository;
import com.backend.finledger.sales.dto.*;
import com.backend.finledger.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final ReceiptRepository receiptRepository;
    private final AccountRepository accountRepository;
    private final SalesMapper salesMapper;

    public CustomerService(CustomerRepository customerRepository,
                           InvoiceRepository invoiceRepository,
                           ReceiptRepository receiptRepository,
                           AccountRepository accountRepository,
                           SalesMapper salesMapper) {
        this.customerRepository = customerRepository;
        this.invoiceRepository = invoiceRepository;
        this.receiptRepository = receiptRepository;
        this.accountRepository = accountRepository;
        this.salesMapper = salesMapper;
    }

    @Transactional
    @PreAuthorize("hasAuthority('CUSTOMER_CREATE')")
    public CustomerDto createCustomer(CustomerRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        if (customerRepository.existsByCompanyIdAndCode(companyId, request.code())) {
            throw new BusinessException("DUPLICATE_CUSTOMER_CODE", "Customer code already exists: " + request.code());
        }

        validateArAccount(companyId, request.arAccountId());

        Customer customer = salesMapper.toCustomerEntity(request);
        customer.setCompanyId(companyId);
        if (customer.getPaymentTermsDays() == null) {
            customer.setPaymentTermsDays((short) 30);
        }
        if (customer.getIsActive() == null) {
            customer.setIsActive(true);
        }

        Customer saved = customerRepository.save(customer);
        return salesMapper.toCustomerDto(saved);
    }

    @Transactional
    @PreAuthorize("hasAuthority('CUSTOMER_EDIT')")
    public CustomerDto updateCustomer(Long id, CustomerRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        Customer customer = customerRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found with id: " + id));

        if (!customer.getCode().equalsIgnoreCase(request.code()) &&
                customerRepository.existsByCompanyIdAndCode(companyId, request.code())) {
            throw new BusinessException("DUPLICATE_CUSTOMER_CODE", "Customer code already exists: " + request.code());
        }

        validateArAccount(companyId, request.arAccountId());

        customer.setCode(request.code());
        customer.setName(request.name());
        customer.setTrn(request.trn());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setBillingAddress(request.billingAddress());
        customer.setCountryCode(request.countryCode());
        customer.setCurrencyCode(request.currencyCode());
        customer.setPaymentTermsDays(request.paymentTermsDays() != null ? request.paymentTermsDays() : (short) 30);
        customer.setCreditLimit(request.creditLimit());
        customer.setArAccountId(request.arAccountId());
        customer.setAccountManagerId(request.accountManagerId());
        customer.setSourceLeadId(request.sourceLeadId());
        if (request.isActive() != null) {
            customer.setIsActive(request.isActive());
        }

        Customer saved = customerRepository.save(customer);
        return salesMapper.toCustomerDto(saved);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public CustomerDto getCustomerById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Customer customer = customerRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found with id: " + id));
        return salesMapper.toCustomerDto(customer);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public PageResponse<CustomerDto> getCustomers(String search, Pageable pageable) {
        Long companyId = CurrentUser.getCompanyId();
        Page<Customer> page;
        if (search != null && !search.isBlank()) {
            page = customerRepository.findByCompanyIdAndNameContainingIgnoreCase(companyId, search.trim(), pageable);
        } else {
            page = customerRepository.findByCompanyId(companyId, pageable);
        }
        return PageResponse.from(page.map(salesMapper::toCustomerDto));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('INVOICE_VIEW')")
    public List<OpenInvoiceDto> getOpenInvoices(Long customerId) {
        Long companyId = CurrentUser.getCompanyId();
        // verify customer
        customerRepository.findByCompanyIdAndId(companyId, customerId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found with id: " + customerId));

        List<Invoice> openInvoices = invoiceRepository.findByCompanyIdAndCustomerIdAndStatusInAndBalanceDueGreaterThan(
                companyId,
                customerId,
                List.of(InvoiceStatus.SENT, InvoiceStatus.PARTIAL, InvoiceStatus.OVERDUE),
                BigDecimal.ZERO
        );

        return salesMapper.toOpenInvoiceDtoList(openInvoices);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public CustomerStatementDto getCustomerStatement(Long customerId, LocalDate fromDate, LocalDate toDate) {
        Long companyId = CurrentUser.getCompanyId();
        Customer customer = customerRepository.findByCompanyIdAndId(companyId, customerId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found with id: " + customerId));

        LocalDate from = fromDate != null ? fromDate : LocalDate.now().withDayOfMonth(1);
        LocalDate to = toDate != null ? toDate : LocalDate.now();

        // 1. Calculate opening balance (prior to 'from')
        // Sum of posted invoices (debit) - credit notes (credit) - receipts (credit)
        BigDecimal openingInvoices = BigDecimal.ZERO;
        List<Invoice> priorInvoices = invoiceRepository.findPostedInvoicesBeforeDate(companyId, customerId, from);
        for (Invoice inv : priorInvoices) {
            if (inv.getDocType() == InvoiceDocType.INVOICE) {
                openingInvoices = openingInvoices.add(inv.getTotal());
            } else if (inv.getDocType() == InvoiceDocType.CREDIT_NOTE) {
                openingInvoices = openingInvoices.subtract(inv.getTotal());
            }
        }

        BigDecimal openingReceipts = BigDecimal.ZERO;
        List<Receipt> priorReceipts = receiptRepository.findPostedReceiptsBeforeDate(companyId, customerId, from);
        for (Receipt rct : priorReceipts) {
            openingReceipts = openingReceipts.add(rct.getAmount());
        }

        BigDecimal openingBalance = openingInvoices.subtract(openingReceipts).setScale(2, RoundingMode.HALF_UP);

        // 2. Fetch transactions within [from, to]
        List<Invoice> periodInvoices = invoiceRepository.findPostedInvoicesForStatement(companyId, customerId, from, to);
        List<Receipt> periodReceipts = receiptRepository.findPostedReceiptsForStatement(companyId, customerId, from, to);

        record TxEntry(LocalDate date, String type, String ref, String desc, BigDecimal debit, BigDecimal credit) {}
        List<TxEntry> entries = new ArrayList<>();

        for (Invoice inv : periodInvoices) {
            if (inv.getDocType() == InvoiceDocType.INVOICE) {
                entries.add(new TxEntry(
                        inv.getInvoiceDate(),
                        "INVOICE",
                        inv.getInvoiceNo(),
                        "Invoice #" + inv.getInvoiceNo(),
                        inv.getTotal(),
                        BigDecimal.ZERO.setScale(2)
                ));
            } else {
                entries.add(new TxEntry(
                        inv.getInvoiceDate(),
                        "CREDIT_NOTE",
                        inv.getInvoiceNo(),
                        "Credit Note #" + inv.getInvoiceNo(),
                        BigDecimal.ZERO.setScale(2),
                        inv.getTotal()
                ));
            }
        }

        for (Receipt rct : periodReceipts) {
            entries.add(new TxEntry(
                    rct.getReceiptDate(),
                    "RECEIPT",
                    rct.getReceiptNo(),
                    "Customer Receipt #" + rct.getReceiptNo() + (rct.getReference() != null ? " (" + rct.getReference() + ")" : ""),
                    BigDecimal.ZERO.setScale(2),
                    rct.getAmount()
            ));
        }

        // Sort chronologically
        entries.sort(Comparator.comparing(TxEntry::date));

        // 3. Build statement lines with running balance
        BigDecimal running = openingBalance;
        List<CustomerStatementLineDto> lines = new ArrayList<>();
        for (TxEntry entry : entries) {
            running = running.add(entry.debit()).subtract(entry.credit()).setScale(2, RoundingMode.HALF_UP);
            lines.add(new CustomerStatementLineDto(
                    entry.date(),
                    entry.type(),
                    entry.ref(),
                    entry.desc(),
                    entry.debit(),
                    entry.credit(),
                    running
            ));
        }

        return new CustomerStatementDto(
                customer.getId(),
                customer.getCode(),
                customer.getName(),
                customer.getCurrencyCode(),
                from,
                to,
                openingBalance,
                running,
                lines
        );
    }

    private void validateArAccount(Long companyId, Long accountId) {
        Account account = accountRepository.findByCompanyIdAndId(companyId, accountId)
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "AR Account not found with id: " + accountId));
        if (Boolean.FALSE.equals(account.getIsActive())) {
            throw new BusinessException("ACCOUNT_INACTIVE", "AR Account is inactive: " + account.getCode());
        }
        if (Boolean.TRUE.equals(account.getIsGroup())) {
            throw new BusinessException("GROUP_ACCOUNT_NOT_ALLOWED", "AR Account cannot be a group account: " + account.getCode());
        }
    }
}

