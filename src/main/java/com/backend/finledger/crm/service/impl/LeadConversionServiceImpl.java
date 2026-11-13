package com.backend.finledger.crm.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.crm.dto.LeadConvertRequest;
import com.backend.finledger.crm.dto.LeadConvertResponse;
import com.backend.finledger.crm.entity.Contact;
import com.backend.finledger.crm.entity.Lead;
import com.backend.finledger.crm.entity.LeadStatus;
import com.backend.finledger.crm.repository.ContactRepository;
import com.backend.finledger.crm.repository.LeadRepository;
import com.backend.finledger.crm.service.LeadConversionService;
import com.backend.finledger.ledger.entity.Account;
import com.backend.finledger.ledger.entity.AccountType;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.sales.entity.Customer;
import com.backend.finledger.sales.repository.CustomerRepository;
import com.backend.finledger.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class LeadConversionServiceImpl implements LeadConversionService {

    private final LeadRepository leadRepository;
    private final ContactRepository contactRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final CompanyRepository companyRepository;

    public LeadConversionServiceImpl(
            LeadRepository leadRepository,
            ContactRepository contactRepository,
            CustomerRepository customerRepository,
            AccountRepository accountRepository,
            CompanyRepository companyRepository) {
        this.leadRepository = leadRepository;
        this.contactRepository = contactRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('LEAD_CONVERT')")
    public LeadConvertResponse convertLead(Long leadId, LeadConvertRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        Lead lead = leadRepository.findByCompanyIdAndId(companyId, leadId)
                .orElseThrow(() -> new BusinessException("LEAD_NOT_FOUND", "Lead not found with id: " + leadId));

        if (lead.getStatus() == LeadStatus.CONVERTED) {
            throw new BusinessException("LEAD_ALREADY_CONVERTED",
                    "Lead " + lead.getLeadNo() + " has already been converted to customer #" + lead.getConvertedCustomerId());
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Company not found with id: " + companyId));

        // Check for matching existing customer to avoid duplicates (TRN or Email)
        Customer customer = null;
        boolean matchedExistingCustomer = false;

        if (request != null && request.matchExistingCustomerId() != null) {
            customer = customerRepository.findByCompanyIdAndId(companyId, request.matchExistingCustomerId())
                    .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND",
                            "Matched customer not found with id: " + request.matchExistingCustomerId()));
            matchedExistingCustomer = true;
        } else if (request == null || !Boolean.TRUE.equals(request.forceCreate())) {
            // Check TRN match
            String trnToCheck = (request != null && request.trn() != null && !request.trn().isBlank())
                    ? request.trn().trim()
                    : null;
            if (trnToCheck != null) {
                Optional<Customer> existingByTrn = customerRepository.findFirstByCompanyIdAndTrn(companyId, trnToCheck);
                if (existingByTrn.isPresent()) {
                    customer = existingByTrn.get();
                    matchedExistingCustomer = true;
                }
            }

            // Check Email match if not matched by TRN
            if (customer == null && lead.getEmail() != null && !lead.getEmail().isBlank()) {
                Optional<Customer> existingByEmail = customerRepository.findFirstByCompanyIdAndEmailIgnoreCase(companyId, lead.getEmail().trim());
                if (existingByEmail.isPresent()) {
                    customer = existingByEmail.get();
                    matchedExistingCustomer = true;
                }
            }
        }

        // If no matching customer found (or forceCreate is true), create a new customer
        if (customer == null) {
            customer = new Customer();
            customer.setCompanyId(companyId);

            // Determine code
            String code = (request != null && request.customerCode() != null && !request.customerCode().isBlank())
                    ? request.customerCode().trim().toUpperCase()
                    : generateUniqueCustomerCode(companyId);

            if (customerRepository.existsByCompanyIdAndCode(companyId, code)) {
                throw new BusinessException("DUPLICATE_CUSTOMER_CODE", "Customer code already exists: " + code);
            }
            customer.setCode(code);

            // Determine name: requested name -> lead company name -> lead contact name
            String custName = (request != null && request.customerName() != null && !request.customerName().isBlank())
                    ? request.customerName().trim()
                    : (lead.getCompanyName() != null && !lead.getCompanyName().isBlank()
                            ? lead.getCompanyName().trim()
                            : lead.getContactName().trim());
            customer.setName(custName);

            if (request != null && request.trn() != null && !request.trn().isBlank()) {
                customer.setTrn(request.trn().trim());
            }
            customer.setEmail(lead.getEmail());
            customer.setPhone(lead.getPhone());
            customer.setCountryCode(lead.getCountryCode() != null ? lead.getCountryCode() : "AE");
            customer.setCurrencyCode(company.getBaseCurrency());
            customer.setPaymentTermsDays((short) 30);
            customer.setSourceLeadId(lead.getId());
            customer.setAccountManagerId(lead.getOwnerUserId());
            customer.setIsActive(true);

            // Assign AR account
            Long arAccountId = (request != null && request.arAccountId() != null)
                    ? request.arAccountId()
                    : resolveDefaultArAccount(companyId);
            customer.setArAccountId(arAccountId);

            customer = customerRepository.save(customer);
        } else {
            // If linked to existing customer, attach source lead if not set
            if (customer.getSourceLeadId() == null) {
                customer.setSourceLeadId(lead.getId());
                customer = customerRepository.save(customer);
            }
        }

        // Create primary contact for this customer
        Contact contact = new Contact();
        contact.setCompanyId(companyId);
        contact.setCustomerId(customer.getId());
        contact.setLeadId(lead.getId());
        contact.setFullName(lead.getContactName());
        contact.setEmail(lead.getEmail());
        contact.setPhone(lead.getPhone());
        contact.setIsPrimary(true);
        contact = contactRepository.save(contact);

        // Update lead status to CONVERTED and link back
        lead.setStatus(LeadStatus.CONVERTED);
        lead.setConvertedCustomerId(customer.getId());
        lead.setConvertedAt(Instant.now());
        leadRepository.save(lead);

        return new LeadConvertResponse(
                lead.getId(),
                lead.getLeadNo(),
                customer.getId(),
                customer.getCode(),
                customer.getName(),
                contact.getId(),
                contact.getFullName(),
                lead.getConvertedAt(),
                matchedExistingCustomer
        );
    }

    private Long resolveDefaultArAccount(Long companyId) {
        // Look for 1200 Accounts Receivable
        Optional<Account> defaultAr = accountRepository.findByCompanyIdAndCode(companyId, "1200");
        if (defaultAr.isPresent() && Boolean.TRUE.equals(defaultAr.get().getIsActive()) && !Boolean.TRUE.equals(defaultAr.get().getIsGroup())) {
            return defaultAr.get().getId();
        }

        // Fallback: look for subType RECEIVABLE
        for (Account a : accountRepository.findByCompanyIdOrderByCodeAsc(companyId)) {
            if ("RECEIVABLE".equalsIgnoreCase(a.getSubType()) && Boolean.TRUE.equals(a.getIsActive()) && !Boolean.TRUE.equals(a.getIsGroup())) {
                return a.getId();
            }
        }

        // Fallback: any active ASSET account
        for (Account a : accountRepository.findByCompanyIdOrderByCodeAsc(companyId)) {
            if (a.getAccountType() == AccountType.ASSET && Boolean.TRUE.equals(a.getIsActive()) && !Boolean.TRUE.equals(a.getIsGroup())) {
                return a.getId();
            }
        }

        throw new BusinessException("AR_ACCOUNT_NOT_FOUND", "No active Accounts Receivable account found for company: " + companyId);
    }

    private String generateUniqueCustomerCode(Long companyId) {
        long count = customerRepository.countByCompanyId(companyId) + 1;
        String candidate = String.format("CUST-%04d", count);
        while (customerRepository.existsByCompanyIdAndCode(companyId, candidate)) {
            count++;
            candidate = String.format("CUST-%04d", count);
        }
        return candidate;
    }
}
