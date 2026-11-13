package com.backend.finledger.crm.controller;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.crm.dto.ContactDto;
import com.backend.finledger.crm.dto.ContactRequest;
import com.backend.finledger.crm.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('LEAD_VIEW','OPPORTUNITY_VIEW','CUSTOMER_VIEW','CRM_VIEW_ALL')")
    public ResponseEntity<?> getContacts(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long leadId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (customerId != null) {
            return ResponseEntity.ok(contactService.getContactsByCustomer(customerId));
        }
        if (leadId != null) {
            return ResponseEntity.ok(contactService.getContactsByLead(leadId));
        }
        return ResponseEntity.ok(contactService.getContacts(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('LEAD_VIEW','OPPORTUNITY_VIEW','CUSTOMER_VIEW','CRM_VIEW_ALL')")
    public ResponseEntity<ContactDto> getContactById(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.getContactById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE','LEAD_EDIT','CUSTOMER_CREATE','CUSTOMER_EDIT','OPPORTUNITY_EDIT')")
    public ResponseEntity<ContactDto> createContact(@Valid @RequestBody ContactRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contactService.createContact(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('LEAD_EDIT','CUSTOMER_EDIT','OPPORTUNITY_EDIT')")
    public ResponseEntity<ContactDto> updateContact(@PathVariable Long id, @Valid @RequestBody ContactRequest request) {
        return ResponseEntity.ok(contactService.updateContact(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('LEAD_EDIT','CUSTOMER_EDIT','OPPORTUNITY_EDIT','CRM_VIEW_ALL')")
    public ResponseEntity<Void> deleteContact(@PathVariable Long id) {
        contactService.deleteContact(id);
        return ResponseEntity.noContent().build();
    }
}

