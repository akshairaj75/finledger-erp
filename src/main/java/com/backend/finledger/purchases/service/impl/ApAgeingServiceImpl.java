package com.backend.finledger.purchases.service.impl;

import com.backend.finledger.purchases.dto.ApAgeingReportDto;
import com.backend.finledger.purchases.dto.ApAgeingVendorDto;
import com.backend.finledger.purchases.entity.Bill;
import com.backend.finledger.purchases.entity.BillStatus;
import com.backend.finledger.purchases.entity.Vendor;
import com.backend.finledger.purchases.repository.BillRepository;
import com.backend.finledger.purchases.repository.VendorRepository;
import com.backend.finledger.purchases.service.ApAgeingService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ApAgeingServiceImpl implements ApAgeingService {

    private final BillRepository billRepository;
    private final VendorRepository vendorRepository;

    public ApAgeingServiceImpl(BillRepository billRepository, VendorRepository vendorRepository) {
        this.billRepository = billRepository;
        this.vendorRepository = vendorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ApAgeingReportDto getApAgeingReport(LocalDate asOfDate) {
        Long companyId = CurrentUser.getCompanyId();
        LocalDate asOf = asOfDate != null ? asOfDate : LocalDate.now();

        List<Bill> openBills = billRepository.findByCompanyIdAndStatusInAndBalanceDueGreaterThan(
                companyId,
                List.of(BillStatus.APPROVED, BillStatus.PARTIAL, BillStatus.OVERDUE),
                BigDecimal.ZERO
        );

        Map<Long, List<Bill>> vendorBillMap = new LinkedHashMap<>();
        for (Bill bill : openBills) {
            vendorBillMap.computeIfAbsent(bill.getVendorId(), k -> new ArrayList<>()).add(bill);
        }

        List<ApAgeingVendorDto> vendorDtos = new ArrayList<>();

        BigDecimal totalCurrent = BigDecimal.ZERO;
        BigDecimal total1To30 = BigDecimal.ZERO;
        BigDecimal total31To60 = BigDecimal.ZERO;
        BigDecimal total61To90 = BigDecimal.ZERO;
        BigDecimal totalOver90 = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Map.Entry<Long, List<Bill>> entry : vendorBillMap.entrySet()) {
            Long vendorId = entry.getKey();
            List<Bill> bills = entry.getValue();

            Vendor vendor = vendorRepository.findByCompanyIdAndId(companyId, vendorId).orElse(null);
            String vendorCode = vendor != null ? vendor.getCode() : "N/A";
            String vendorName = vendor != null ? vendor.getName() : "Unknown";

            BigDecimal vCurrent = BigDecimal.ZERO;
            BigDecimal v1To30 = BigDecimal.ZERO;
            BigDecimal v31To60 = BigDecimal.ZERO;
            BigDecimal v61To90 = BigDecimal.ZERO;
            BigDecimal vOver90 = BigDecimal.ZERO;
            BigDecimal vTotal = BigDecimal.ZERO;

            for (Bill bill : bills) {
                BigDecimal balance = bill.getBalanceDue();
                long daysOverdue = ChronoUnit.DAYS.between(bill.getDueDate(), asOf);

                if (daysOverdue <= 0) {
                    vCurrent = vCurrent.add(balance);
                } else if (daysOverdue <= 30) {
                    v1To30 = v1To30.add(balance);
                } else if (daysOverdue <= 60) {
                    v31To60 = v31To60.add(balance);
                } else if (daysOverdue <= 90) {
                    v61To90 = v61To90.add(balance);
                } else {
                    vOver90 = vOver90.add(balance);
                }
                vTotal = vTotal.add(balance);
            }

            vendorDtos.add(new ApAgeingVendorDto(
                    vendorId,
                    vendorCode,
                    vendorName,
                    vCurrent.setScale(2, RoundingMode.HALF_UP),
                    v1To30.setScale(2, RoundingMode.HALF_UP),
                    v31To60.setScale(2, RoundingMode.HALF_UP),
                    v61To90.setScale(2, RoundingMode.HALF_UP),
                    vOver90.setScale(2, RoundingMode.HALF_UP),
                    vTotal.setScale(2, RoundingMode.HALF_UP)
            ));

            totalCurrent = totalCurrent.add(vCurrent);
            total1To30 = total1To30.add(v1To30);
            total31To60 = total31To60.add(v31To60);
            total61To90 = total61To90.add(v61To90);
            totalOver90 = totalOver90.add(vOver90);
            grandTotal = grandTotal.add(vTotal);
        }

        return new ApAgeingReportDto(
                asOf,
                vendorDtos,
                totalCurrent.setScale(2, RoundingMode.HALF_UP),
                total1To30.setScale(2, RoundingMode.HALF_UP),
                total31To60.setScale(2, RoundingMode.HALF_UP),
                total61To90.setScale(2, RoundingMode.HALF_UP),
                totalOver90.setScale(2, RoundingMode.HALF_UP),
                grandTotal.setScale(2, RoundingMode.HALF_UP)
        );
    }
}

