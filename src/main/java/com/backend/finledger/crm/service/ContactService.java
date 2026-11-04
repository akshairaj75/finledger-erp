package com.backend.finledger.crm.service;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.crm.dto.ContactDto;
import com.backend.finledger.crm.dto.ContactRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ContactService {

    PageResponse<ContactDto> getContacts(Pageable pageable);

    List<ContactDto> getContactsByCustomer(Long customerId);

    List<ContactDto> getContactsByLead(Long leadId);

    ContactDto getContactById(Long id);

    ContactDto createContact(ContactRequest request);

    ContactDto updateContact(Long id, ContactRequest request);

    void deleteContact(Long id);
}
