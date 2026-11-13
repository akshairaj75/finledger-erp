package com.backend.finledger.purchases.controller;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.purchases.dto.OpenBillDto;
import com.backend.finledger.purchases.dto.VendorDto;
import com.backend.finledger.purchases.dto.VendorRequest;
import com.backend.finledger.purchases.service.VendorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vendors")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('VENDOR_CREATE')")
    public ResponseEntity<VendorDto> createVendor(@Valid @RequestBody VendorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vendorService.createVendor(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('VENDOR_EDIT')")
    public ResponseEntity<VendorDto> updateVendor(@PathVariable Long id,
                                                  @Valid @RequestBody VendorRequest request) {
        return ResponseEntity.ok(vendorService.updateVendor(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VENDOR_VIEW')")
    public ResponseEntity<VendorDto> getVendor(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.getVendorById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VENDOR_VIEW')")
    public ResponseEntity<PageResponse<VendorDto>> getVendors(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(vendorService.getVendors(search, pageable));
    }

    @GetMapping("/{id}/open-bills")
    @PreAuthorize("hasAuthority('VENDOR_VIEW') or hasAuthority('BILL_VIEW') or hasAuthority('PAYMENT_CREATE')")
    public ResponseEntity<List<OpenBillDto>> getOpenBills(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.getOpenBills(id));
    }
}

