package com.backend.finledger.purchases.controller;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.purchases.dto.PaymentDto;
import com.backend.finledger.purchases.dto.PaymentRequest;
import com.backend.finledger.purchases.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentDto> createPayment(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPayment(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto> getPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<PaymentDto>> getPayments(
            @RequestParam(required = false) Long vendorId,
            Pageable pageable) {
        return ResponseEntity.ok(paymentService.getPayments(vendorId, pageable));
    }

    @PostMapping("/{id}/void")
    public ResponseEntity<PaymentDto> voidPayment(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(paymentService.voidPayment(id, reason));
    }
}

