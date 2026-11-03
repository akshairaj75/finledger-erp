package com.backend.finledger.purchases.service;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.purchases.dto.PaymentDto;
import com.backend.finledger.purchases.dto.PaymentRequest;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

    PaymentDto createPayment(PaymentRequest request);

    PaymentDto getPaymentById(Long id);

    PageResponse<PaymentDto> getPayments(Long vendorId, Pageable pageable);

    PaymentDto voidPayment(Long id, String reason);
}

