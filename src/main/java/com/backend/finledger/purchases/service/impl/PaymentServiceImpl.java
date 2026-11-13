package com.backend.finledger.purchases.service.impl;

import com.backend.finledger.banking.entity.BankAccount;
import com.backend.finledger.banking.repository.BankAccountRepository;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.DocType;
import com.backend.finledger.common.NumberSeriesService;
import com.backend.finledger.common.PageResponse;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.ledger.dto.JournalEntryDto;
import com.backend.finledger.ledger.dto.JournalLineRequest;
import com.backend.finledger.ledger.dto.SourceDocumentPostingRequest;
import com.backend.finledger.ledger.entity.JournalSourceType;
import com.backend.finledger.ledger.entity.PartyType;
import com.backend.finledger.ledger.service.PostingService;
import com.backend.finledger.purchases.dto.PaymentAllocationDto;
import com.backend.finledger.purchases.dto.PaymentAllocationRequest;
import com.backend.finledger.purchases.dto.PaymentDto;
import com.backend.finledger.purchases.dto.PaymentRequest;
import com.backend.finledger.purchases.entity.Bill;
import com.backend.finledger.purchases.entity.BillStatus;
import com.backend.finledger.purchases.entity.Payment;
import com.backend.finledger.purchases.entity.PaymentAllocation;
import com.backend.finledger.purchases.entity.PaymentStatus;
import com.backend.finledger.purchases.entity.Vendor;
import com.backend.finledger.purchases.mapper.PurchasesMapper;
import com.backend.finledger.purchases.repository.BillRepository;
import com.backend.finledger.purchases.repository.PaymentRepository;
import com.backend.finledger.purchases.repository.VendorRepository;
import com.backend.finledger.purchases.service.PaymentService;
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
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final VendorRepository vendorRepository;
    private final BillRepository billRepository;
    private final BankAccountRepository bankAccountRepository;
    private final CompanyRepository companyRepository;
    private final PostingService postingService;
    private final NumberSeriesService numberSeriesService;
    private final PurchasesMapper purchasesMapper;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              VendorRepository vendorRepository,
                              BillRepository billRepository,
                              BankAccountRepository bankAccountRepository,
                              CompanyRepository companyRepository,
                              PostingService postingService,
                              NumberSeriesService numberSeriesService,
                              PurchasesMapper purchasesMapper) {
        this.paymentRepository = paymentRepository;
        this.vendorRepository = vendorRepository;
        this.billRepository = billRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.companyRepository = companyRepository;
        this.postingService = postingService;
        this.numberSeriesService = numberSeriesService;
        this.purchasesMapper = purchasesMapper;
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PAYMENT_CREATE')")
    public PaymentDto createPayment(PaymentRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        Vendor vendor = vendorRepository.findByCompanyIdAndId(companyId, request.vendorId())
                .orElseThrow(() -> new BusinessException("VENDOR_NOT_FOUND", "Vendor not found with id: " + request.vendorId()));

        BankAccount bankAccount = bankAccountRepository.findByCompanyIdAndId(companyId, request.bankAccountId())
                .orElseThrow(() -> new BusinessException("BANK_ACCOUNT_NOT_FOUND", "Bank account not found with id: " + request.bankAccountId()));

        if (Boolean.FALSE.equals(bankAccount.getIsActive())) {
            throw new BusinessException("BANK_ACCOUNT_INACTIVE", "Bank account is inactive: " + bankAccount.getName());
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Company not found"));

        String currency = request.currencyCode() != null ? request.currencyCode() : vendor.getCurrencyCode();
        if (currency == null) {
            currency = company.getBaseCurrency();
        }

        Payment payment = new Payment();
        payment.setCompanyId(companyId);
        payment.setVendorId(vendor.getId());
        payment.setPaymentDate(request.paymentDate());
        payment.setBankAccountId(bankAccount.getId());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setReference(request.reference());
        payment.setCurrencyCode(currency);
        payment.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));

        BigDecimal totalAllocated = BigDecimal.ZERO;
        Set<Long> seenBillIds = new HashSet<>();

        if (request.allocations() != null && !request.allocations().isEmpty()) {
            for (PaymentAllocationRequest ar : request.allocations()) {
                if (!seenBillIds.add(ar.billId())) {
                    throw new BusinessException("DUPLICATE_ALLOCATION", "Duplicate allocation for bill id: " + ar.billId());
                }

                Bill bill = billRepository.findByCompanyIdAndId(companyId, ar.billId())
                        .orElseThrow(() -> new BusinessException("BILL_NOT_FOUND", "Bill not found with id: " + ar.billId()));

                if (!bill.getVendorId().equals(vendor.getId())) {
                    throw new BusinessException("BILL_VENDOR_MISMATCH",
                            "Bill #" + bill.getBillNo() + " does not belong to vendor " + vendor.getName());
                }

                if (bill.getStatus() == BillStatus.DRAFT || bill.getStatus() == BillStatus.VOID) {
                    throw new BusinessException("INVALID_BILL_STATUS",
                            "Cannot allocate payment to bill #" + bill.getBillNo() + " with status: " + bill.getStatus());
                }

                BigDecimal allocAmount = ar.amount().setScale(2, RoundingMode.HALF_UP);
                if (allocAmount.compareTo(bill.getBalanceDue()) > 0) {
                    throw new BusinessException("OVER_ALLOCATION",
                            "Allocation amount (" + allocAmount + ") exceeds bill #" + bill.getBillNo() + " balance due (" + bill.getBalanceDue() + ")");
                }

                totalAllocated = totalAllocated.add(allocAmount);

                BigDecimal newPaid = bill.getAmountPaid().add(allocAmount).setScale(2, RoundingMode.HALF_UP);
                BigDecimal newDue = bill.getBalanceDue().subtract(allocAmount).setScale(2, RoundingMode.HALF_UP);
                bill.setAmountPaid(newPaid);
                bill.setBalanceDue(newDue);

                if (newDue.compareTo(BigDecimal.ZERO) == 0) {
                    bill.setStatus(BillStatus.PAID);
                } else {
                    bill.setStatus(BillStatus.PARTIAL);
                }

                billRepository.save(bill);

                PaymentAllocation allocation = new PaymentAllocation(payment, bill.getId(), allocAmount);
                payment.addAllocation(allocation);
            }
        }

        if (totalAllocated.compareTo(payment.getAmount()) > 0) {
            throw new BusinessException("EXCEEDS_PAYMENT_AMOUNT",
                    "Total allocated (" + totalAllocated + ") cannot exceed payment amount (" + payment.getAmount() + ")");
        }

        BigDecimal unapplied = payment.getAmount().subtract(totalAllocated).setScale(2, RoundingMode.HALF_UP);
        payment.setUnappliedAmount(unapplied);

        String paymentNo = numberSeriesService.generateNextNumber(companyId, DocType.PAYMENT);
        payment.setPaymentNo(paymentNo);
        payment.setStatus(PaymentStatus.POSTED);
        payment = paymentRepository.save(payment);

        List<JournalLineRequest> journalLines = List.of(
                new JournalLineRequest(
                        vendor.getApAccountId(),
                        null,
                        "Payment #" + payment.getPaymentNo() + " to " + vendor.getName(),
                        payment.getAmount(),
                        BigDecimal.ZERO,
                        PartyType.VENDOR,
                        vendor.getId()
                ),
                new JournalLineRequest(
                        bankAccount.getGlAccountId(),
                        null,
                        "Payment #" + payment.getPaymentNo() + " via " + payment.getPaymentMethod(),
                        BigDecimal.ZERO,
                        payment.getAmount(),
                        null,
                        null
                )
        );

        String reference = payment.getPaymentNo();
        if (payment.getReference() != null && !payment.getReference().isBlank()) {
            reference += " (" + payment.getReference() + ")";
        }

        SourceDocumentPostingRequest postingRequest = new SourceDocumentPostingRequest(
                JournalSourceType.PAYMENT,
                payment.getId(),
                payment.getPaymentDate(),
                reference,
                "Vendor Payment #" + payment.getPaymentNo() + " - " + vendor.getName(),
                payment.getCurrencyCode(),
                BigDecimal.ONE,
                journalLines
        );

        JournalEntryDto postedEntry = postingService.postDocument(postingRequest);
        payment.setJournalEntryId(postedEntry.id());

        Payment saved = paymentRepository.save(payment);
        return mapToDto(saved, vendor, bankAccount, unapplied);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PAYMENT_VIEW')")
    public PaymentDto getPaymentById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Payment payment = paymentRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND", "Payment not found with id: " + id));

        Vendor vendor = vendorRepository.findByCompanyIdAndId(companyId, payment.getVendorId()).orElse(null);
        BankAccount bankAccount = bankAccountRepository.findByCompanyIdAndId(companyId, payment.getBankAccountId()).orElse(null);

        BigDecimal allocatedSum = payment.getAllocations().stream()
                .map(PaymentAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unapplied = payment.getAmount().subtract(allocatedSum);

        return mapToDto(payment, vendor, bankAccount, unapplied);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PAYMENT_VIEW')")
    public PageResponse<PaymentDto> getPayments(Long vendorId, Pageable pageable) {
        Long companyId = CurrentUser.getCompanyId();
        Page<Payment> page;
        if (vendorId != null) {
            page = paymentRepository.findByCompanyIdAndVendorId(companyId, vendorId, pageable);
        } else {
            page = paymentRepository.findByCompanyId(companyId, pageable);
        }

        return PageResponse.from(page.map(payment -> {
            Vendor vendor = vendorRepository.findByCompanyIdAndId(companyId, payment.getVendorId()).orElse(null);
            BankAccount bankAccount = bankAccountRepository.findByCompanyIdAndId(companyId, payment.getBankAccountId()).orElse(null);

            BigDecimal allocatedSum = payment.getAllocations().stream()
                    .map(PaymentAllocation::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal unapplied = payment.getAmount().subtract(allocatedSum);

            return mapToDto(payment, vendor, bankAccount, unapplied);
        }));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PAYMENT_VOID')")
    public PaymentDto voidPayment(Long id, String reason) {
        Long companyId = CurrentUser.getCompanyId();
        Payment payment = paymentRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND", "Payment not found with id: " + id));

        if (payment.getStatus() == PaymentStatus.VOID) {
            throw new BusinessException("ALREADY_VOID", "Payment is already voided");
        }

        // 1. Roll back all bill allocations
        for (PaymentAllocation alloc : payment.getAllocations()) {
            Bill bill = billRepository.findByCompanyIdAndId(companyId, alloc.getBillId()).orElse(null);
            if (bill != null && bill.getStatus() != BillStatus.VOID) {
                BigDecimal restoredPaid = bill.getAmountPaid().subtract(alloc.getAmount()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal restoredDue = bill.getBalanceDue().add(alloc.getAmount()).setScale(2, RoundingMode.HALF_UP);
                bill.setAmountPaid(restoredPaid.max(BigDecimal.ZERO));
                bill.setBalanceDue(restoredDue.min(bill.getTotal()));

                if (bill.getBalanceDue().compareTo(bill.getTotal()) == 0) {
                    bill.setStatus(BillStatus.APPROVED);
                } else {
                    bill.setStatus(BillStatus.PARTIAL);
                }
                billRepository.save(bill);
            }
        }

        // 2. Reverse journal entry
        if (payment.getJournalEntryId() != null) {
            postingService.reverseJournalEntry(payment.getJournalEntryId(), reason != null ? reason : "Payment voided");
        }

        payment.setStatus(PaymentStatus.VOID);
        Payment saved = paymentRepository.save(payment);

        Vendor vendor = vendorRepository.findByCompanyIdAndId(companyId, payment.getVendorId()).orElse(null);
        BankAccount bankAccount = bankAccountRepository.findByCompanyIdAndId(companyId, payment.getBankAccountId()).orElse(null);
        return mapToDto(saved, vendor, bankAccount, BigDecimal.ZERO);
    }

    private PaymentDto mapToDto(Payment payment, Vendor vendor, BankAccount bankAccount, BigDecimal unapplied) {
        List<PaymentAllocationDto> allocDtos = payment.getAllocations().stream().map(a -> {
            String bNo = null;
            Optional<Bill> optBill = billRepository.findByCompanyIdAndId(payment.getCompanyId(), a.getBillId());
            if (optBill.isPresent()) {
                bNo = optBill.get().getBillNo();
            }
            return new PaymentAllocationDto(a.getId(), a.getBillId(), bNo, a.getAmount());
        }).toList();

        return new PaymentDto(
                payment.getId(),
                payment.getCompanyId(),
                payment.getPaymentNo(),
                payment.getVendorId(),
                vendor != null ? vendor.getName() : null,
                vendor != null ? vendor.getCode() : null,
                payment.getPaymentDate(),
                payment.getBankAccountId(),
                bankAccount != null ? bankAccount.getName() : null,
                payment.getPaymentMethod(),
                payment.getReference(),
                payment.getCurrencyCode(),
                payment.getAmount(),
                unapplied != null ? unapplied : BigDecimal.ZERO,
                payment.getStatus(),
                payment.getJournalEntryId(),
                allocDtos
        );
    }
}
