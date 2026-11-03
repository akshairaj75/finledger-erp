package com.backend.finledger.purchases.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.PageResponse;
import com.backend.finledger.ledger.entity.Account;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.purchases.dto.OpenBillDto;
import com.backend.finledger.purchases.dto.VendorDto;
import com.backend.finledger.purchases.dto.VendorRequest;
import com.backend.finledger.purchases.entity.Bill;
import com.backend.finledger.purchases.entity.BillStatus;
import com.backend.finledger.purchases.entity.Vendor;
import com.backend.finledger.purchases.mapper.PurchasesMapper;
import com.backend.finledger.purchases.repository.BillRepository;
import com.backend.finledger.purchases.repository.VendorRepository;
import com.backend.finledger.purchases.service.VendorService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class VendorServiceImpl implements VendorService {

    private final VendorRepository vendorRepository;
    private final BillRepository billRepository;
    private final AccountRepository accountRepository;
    private final PurchasesMapper purchasesMapper;

    public VendorServiceImpl(VendorRepository vendorRepository,
                             BillRepository billRepository,
                             AccountRepository accountRepository,
                             PurchasesMapper purchasesMapper) {
        this.vendorRepository = vendorRepository;
        this.billRepository = billRepository;
        this.accountRepository = accountRepository;
        this.purchasesMapper = purchasesMapper;
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('VENDOR_CREATE')")
    public VendorDto createVendor(VendorRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        if (vendorRepository.existsByCompanyIdAndCode(companyId, request.code())) {
            throw new BusinessException("DUPLICATE_VENDOR_CODE", "Vendor code already exists: " + request.code());
        }

        validateApAccount(companyId, request.apAccountId());

        Vendor vendor = purchasesMapper.toVendorEntity(request);
        vendor.setCompanyId(companyId);
        if (vendor.getPaymentTermsDays() == null) {
            vendor.setPaymentTermsDays((short) 30);
        }
        if (vendor.getIsActive() == null) {
            vendor.setIsActive(true);
        }

        Vendor saved = vendorRepository.save(vendor);
        return purchasesMapper.toVendorDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('VENDOR_EDIT')")
    public VendorDto updateVendor(Long id, VendorRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        Vendor vendor = vendorRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("VENDOR_NOT_FOUND", "Vendor not found with id: " + id));

        if (!vendor.getCode().equalsIgnoreCase(request.code()) &&
            vendorRepository.existsByCompanyIdAndCode(companyId, request.code())) {
            throw new BusinessException("DUPLICATE_VENDOR_CODE", "Vendor code already exists: " + request.code());
        }

        validateApAccount(companyId, request.apAccountId());

        vendor.setCode(request.code());
        vendor.setName(request.name());
        vendor.setTrn(request.trn());
        vendor.setEmail(request.email());
        vendor.setPhone(request.phone());
        vendor.setAddress(request.address());
        vendor.setCurrencyCode(request.currencyCode());
        if (request.paymentTermsDays() != null) {
            vendor.setPaymentTermsDays(request.paymentTermsDays());
        }
        vendor.setBankIban(request.bankIban());
        vendor.setApAccountId(request.apAccountId());
        if (request.isActive() != null) {
            vendor.setIsActive(request.isActive());
        }

        Vendor saved = vendorRepository.save(vendor);
        return purchasesMapper.toVendorDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VENDOR_VIEW')")
    public VendorDto getVendorById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Vendor vendor = vendorRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("VENDOR_NOT_FOUND", "Vendor not found with id: " + id));
        return purchasesMapper.toVendorDto(vendor);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VENDOR_VIEW')")
    public PageResponse<VendorDto> getVendors(String search, Pageable pageable) {
        Long companyId = CurrentUser.getCompanyId();
        Page<Vendor> page;
        if (search != null && !search.trim().isEmpty()) {
            page = vendorRepository.findByCompanyIdAndNameContainingIgnoreCase(companyId, search.trim(), pageable);
        } else {
            page = vendorRepository.findByCompanyId(companyId, pageable);
        }
        return PageResponse.from(page.map(purchasesMapper::toVendorDto));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VENDOR_VIEW')")
    public List<OpenBillDto> getOpenBills(Long vendorId) {
        Long companyId = CurrentUser.getCompanyId();
        vendorRepository.findByCompanyIdAndId(companyId, vendorId)
                .orElseThrow(() -> new BusinessException("VENDOR_NOT_FOUND", "Vendor not found with id: " + vendorId));

        List<Bill> bills = billRepository.findByCompanyIdAndVendorIdAndStatusInAndBalanceDueGreaterThan(
                companyId, vendorId, Set.of(BillStatus.APPROVED, BillStatus.PARTIAL), BigDecimal.ZERO);

        return purchasesMapper.toOpenBillDtoList(bills);
    }

    private void validateApAccount(Long companyId, Long accountId) {
        Account account = accountRepository.findByCompanyIdAndId(companyId, accountId)
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "AP Account not found with id: " + accountId));

        if (Boolean.TRUE.equals(account.getIsGroup())) {
            throw new BusinessException("GROUP_ACCOUNT_NOT_ALLOWED", "AP Account cannot be a group account: " + account.getCode());
        }
        if (Boolean.FALSE.equals(account.getIsActive())) {
            throw new BusinessException("INACTIVE_ACCOUNT_NOT_ALLOWED", "AP Account cannot be inactive: " + account.getCode());
        }
    }
}

