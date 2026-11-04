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
    public ResponseEntity<ContactDto> getContactById(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.getContactById(id));
    }

    @PostMapping
    public ResponseEntity<ContactDto> createContact(@Valid @RequestBody ContactRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contactService.createContact(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContactDto> updateContact(@PathVariable Long id, @Valid @RequestBody ContactRequest request) {
        return ResponseEntity.ok(contactService.updateContact(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContact(@PathVariable Long id) {
        contactService.deleteContact(id);
        return ResponseEntity.noContent().build();
    }
}

