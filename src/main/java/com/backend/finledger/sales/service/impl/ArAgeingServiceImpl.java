package com.backend.finledger.sales.service.impl;

import com.backend.finledger.sales.dto.ArAgeingCustomerDto;
import com.backend.finledger.sales.dto.ArAgeingReportDto;
import com.backend.finledger.sales.entity.Customer;
import com.backend.finledger.sales.entity.Invoice;
import com.backend.finledger.sales.entity.InvoiceStatus;
import com.backend.finledger.sales.repository.CustomerRepository;
import com.backend.finledger.sales.repository.InvoiceRepository;
import com.backend.finledger.sales.service.ArAgeingService;
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
public class ArAgeingServiceImpl implements ArAgeingService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;

    public ArAgeingServiceImpl(InvoiceRepository invoiceRepository, CustomerRepository customerRepository) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ArAgeingReportDto getArAgeingReport(LocalDate asOfDate) {
        Long companyId = CurrentUser.getCompanyId();
        LocalDate asOf = asOfDate != null ? asOfDate : LocalDate.now();

        List<Invoice> openInvoices = invoiceRepository.findByCompanyIdAndStatusInAndBalanceDueGreaterThan(
                companyId,
                List.of(InvoiceStatus.SENT, InvoiceStatus.PARTIAL, InvoiceStatus.OVERDUE),
                BigDecimal.ZERO
        );

        Map<Long, List<Invoice>> customerInvoiceMap = new LinkedHashMap<>();
        for (Invoice inv : openInvoices) {
            customerInvoiceMap.computeIfAbsent(inv.getCustomerId(), k -> new ArrayList<>()).add(inv);
        }

        List<ArAgeingCustomerDto> customerDtos = new ArrayList<>();

        BigDecimal totalCurrent = BigDecimal.ZERO;
        BigDecimal total1To30 = BigDecimal.ZERO;
        BigDecimal total31To60 = BigDecimal.ZERO;
        BigDecimal total61To90 = BigDecimal.ZERO;
        BigDecimal totalOver90 = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Map.Entry<Long, List<Invoice>> entry : customerInvoiceMap.entrySet()) {
            Long customerId = entry.getKey();
            List<Invoice> invoices = entry.getValue();

            Customer customer = customerRepository.findByCompanyIdAndId(companyId, customerId).orElse(null);
            String custCode = customer != null ? customer.getCode() : "N/A";
            String custName = customer != null ? customer.getName() : "Unknown";

            BigDecimal custCurrent = BigDecimal.ZERO;
            BigDecimal cust1To30 = BigDecimal.ZERO;
            BigDecimal cust31To60 = BigDecimal.ZERO;
            BigDecimal cust61To90 = BigDecimal.ZERO;
            BigDecimal custOver90 = BigDecimal.ZERO;
            BigDecimal custTotal = BigDecimal.ZERO;

            for (Invoice inv : invoices) {
                BigDecimal balance = inv.getBalanceDue();
                long daysOverdue = ChronoUnit.DAYS.between(inv.getDueDate(), asOf);

                if (daysOverdue <= 0) {
                    custCurrent = custCurrent.add(balance);
                } else if (daysOverdue <= 30) {
                    cust1To30 = cust1To30.add(balance);
                } else if (daysOverdue <= 60) {
                    cust31To60 = cust31To60.add(balance);
                } else if (daysOverdue <= 90) {
                    cust61To90 = cust61To90.add(balance);
                } else {
                    custOver90 = custOver90.add(balance);
                }
                custTotal = custTotal.add(balance);
            }

            customerDtos.add(new ArAgeingCustomerDto(
                    customerId,
                    custCode,
                    custName,
                    custCurrent.setScale(2, RoundingMode.HALF_UP),
                    cust1To30.setScale(2, RoundingMode.HALF_UP),
                    cust31To60.setScale(2, RoundingMode.HALF_UP),
                    cust61To90.setScale(2, RoundingMode.HALF_UP),
                    custOver90.setScale(2, RoundingMode.HALF_UP),
                    custTotal.setScale(2, RoundingMode.HALF_UP)
            ));

            totalCurrent = totalCurrent.add(custCurrent);
            total1To30 = total1To30.add(cust1To30);
            total31To60 = total31To60.add(cust31To60);
            total61To90 = total61To90.add(cust61To90);
            totalOver90 = totalOver90.add(custOver90);
            grandTotal = grandTotal.add(custTotal);
        }

        customerDtos.sort(Comparator.comparing(ArAgeingCustomerDto::customerName));

        return new ArAgeingReportDto(
                asOf,
                customerDtos,
                totalCurrent.setScale(2, RoundingMode.HALF_UP),
                total1To30.setScale(2, RoundingMode.HALF_UP),
                total31To60.setScale(2, RoundingMode.HALF_UP),
                total61To90.setScale(2, RoundingMode.HALF_UP),
                totalOver90.setScale(2, RoundingMode.HALF_UP),
                grandTotal.setScale(2, RoundingMode.HALF_UP)
        );
    }
}

