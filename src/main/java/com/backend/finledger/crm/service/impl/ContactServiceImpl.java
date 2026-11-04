package com.backend.finledger.crm.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.PageResponse;
import com.backend.finledger.crm.dto.ContactDto;
import com.backend.finledger.crm.dto.ContactRequest;
import com.backend.finledger.crm.entity.Contact;
import com.backend.finledger.crm.repository.ContactRepository;
import com.backend.finledger.crm.service.ContactService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;

    public ContactServiceImpl(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContactDto> getContacts(Pageable pageable) {
        Long companyId = CurrentUser.getCompanyId();
        Page<Contact> page = contactRepository.findByCompanyId(companyId, pageable);
        List<ContactDto> content = page.getContent().stream().map(this::toDto).toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactDto> getContactsByCustomer(Long customerId) {
        Long companyId = CurrentUser.getCompanyId();
        return contactRepository.findByCompanyIdAndCustomerId(companyId, customerId)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactDto> getContactsByLead(Long leadId) {
        Long companyId = CurrentUser.getCompanyId();
        return contactRepository.findByCompanyIdAndLeadId(companyId, leadId)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ContactDto getContactById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Contact contact = contactRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("CONTACT_NOT_FOUND", "Contact not found with id: " + id));
        return toDto(contact);
    }

    @Override
    @Transactional
    public ContactDto createContact(ContactRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        Contact contact = new Contact();
        contact.setCompanyId(companyId);
        contact.setCustomerId(request.customerId());
        contact.setLeadId(request.leadId());
        contact.setFullName(request.fullName().trim());
        contact.setJobTitle(request.jobTitle());
        contact.setEmail(request.email());
        contact.setPhone(request.phone());
        contact.setIsPrimary(Boolean.TRUE.equals(request.isPrimary()));

        // If this contact is primary, unset other primaries for the same customer
        if (contact.getIsPrimary() && contact.getCustomerId() != null) {
            unsetOtherPrimaryContacts(companyId, contact.getCustomerId(), null);
        }

        Contact saved = contactRepository.save(contact);
        return toDto(saved);
    }

    @Override
    @Transactional
    public ContactDto updateContact(Long id, ContactRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        Contact contact = contactRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("CONTACT_NOT_FOUND", "Contact not found with id: " + id));

        contact.setCustomerId(request.customerId());
        contact.setLeadId(request.leadId());
        contact.setFullName(request.fullName().trim());
        contact.setJobTitle(request.jobTitle());
        contact.setEmail(request.email());
        contact.setPhone(request.phone());
        contact.setIsPrimary(Boolean.TRUE.equals(request.isPrimary()));

        if (contact.getIsPrimary() && contact.getCustomerId() != null) {
            unsetOtherPrimaryContacts(companyId, contact.getCustomerId(), contact.getId());
        }

        Contact saved = contactRepository.save(contact);
        return toDto(saved);
    }

    @Override
    public void deleteContact(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Contact contact = contactRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("CONTACT_NOT_FOUND", "Contact not found with id: " + id));
        contactRepository.delete(contact);
    }

    private void unsetOtherPrimaryContacts(Long companyId, Long customerId, Long currentContactId) {
        List<Contact> customerContacts = contactRepository.findByCompanyIdAndCustomerId(companyId, customerId);
        for (Contact c : customerContacts) {
            if (!c.getId().equals(currentContactId) && Boolean.TRUE.equals(c.getIsPrimary())) {
                c.setIsPrimary(false);
                contactRepository.save(c);
            }
        }
    }

    public ContactDto toDto(Contact contact) {
        return new ContactDto(
                contact.getId(),
                contact.getCompanyId(),
                contact.getCustomerId(),
                contact.getLeadId(),
                contact.getFullName(),
                contact.getJobTitle(),
                contact.getEmail(),
                contact.getPhone(),
                contact.getIsPrimary()
        );
    }
}
