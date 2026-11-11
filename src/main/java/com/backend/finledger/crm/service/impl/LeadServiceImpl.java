package com.backend.finledger.crm.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.DocType;
import com.backend.finledger.common.ForbiddenException;
import com.backend.finledger.common.NumberSeriesService;
import com.backend.finledger.common.PageResponse;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.crm.dto.*;
import com.backend.finledger.crm.entity.*;
import com.backend.finledger.crm.repository.ContactRepository;
import com.backend.finledger.crm.repository.CrmPipelineStageRepository;
import com.backend.finledger.crm.repository.LeadRepository;
import com.backend.finledger.crm.repository.OpportunityRepository;
import com.backend.finledger.crm.service.CrmPipelineStageService;
import com.backend.finledger.crm.service.LeadService;
import com.backend.finledger.sales.entity.Customer;
import com.backend.finledger.sales.repository.CustomerRepository;
import com.backend.finledger.security.CurrentUser;
import com.backend.finledger.user.entity.User;
import com.backend.finledger.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;
    private final OpportunityRepository opportunityRepository;
    private final CrmPipelineStageRepository stageRepository;
    private final CrmPipelineStageService stageService;
    private final ContactRepository contactRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final NumberSeriesService numberSeriesService;

    public LeadServiceImpl(
            LeadRepository leadRepository,
            OpportunityRepository opportunityRepository,
            CrmPipelineStageRepository stageRepository,
            CrmPipelineStageService stageService,
            ContactRepository contactRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            CompanyRepository companyRepository,
            NumberSeriesService numberSeriesService) {
        this.leadRepository = leadRepository;
        this.opportunityRepository = opportunityRepository;
        this.stageRepository = stageRepository;
        this.stageService = stageService;
        this.contactRepository = contactRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.numberSeriesService = numberSeriesService;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('LEAD_VIEW') or hasAuthority('CRM_VIEW_ALL')")
    public PageResponse<LeadDto> getLeads(LeadStatus status, LeadSource source, Long ownerUserId, String q, Pageable pageable) {
        Long companyId = CurrentUser.getCompanyId();
        String search = (q != null && !q.isBlank()) ? q.trim() : null;
        Long effectiveOwnerId = CurrentUser.hasAuthority("CRM_VIEW_ALL")
                ? ownerUserId
                : CurrentUser.getUserId();

        Page<Lead> page = leadRepository.searchLeads(companyId, status, source, effectiveOwnerId, search, pageable);
        List<LeadDto> dtos = page.getContent().stream().map(this::toDto).toList();
        return new PageResponse<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('LEAD_VIEW') or hasAuthority('CRM_VIEW_ALL')")
    public LeadDto getLeadById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Lead lead = leadRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("LEAD_NOT_FOUND", "Lead not found with id: " + id));
        assertLeadAccess(lead);
        return toDto(lead);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('LEAD_CREATE')")
    public LeadDto createLead(LeadRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        Lead lead = new Lead();
        lead.setCompanyId(companyId);

        String leadNo = numberSeriesService.generateNextNumber(companyId, DocType.LEAD);
        lead.setLeadNo(leadNo);

        populateLeadFields(lead, request);
        lead.setStatus(LeadStatus.NEW);

        Lead saved = leadRepository.save(lead);
        return toDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('LEAD_EDIT')")
    public LeadDto updateLead(Long id, LeadRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        Lead lead = leadRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("LEAD_NOT_FOUND", "Lead not found with id: " + id));
        assertLeadAccess(lead);

        populateLeadFields(lead, request);

        Lead saved = leadRepository.save(lead);
        return toDto(saved);
    }

    private void assertLeadAccess(Lead lead) {
        if (!CurrentUser.hasAuthority("CRM_VIEW_ALL") && lead.getOwnerUserId() != null) {
            if (!lead.getOwnerUserId().equals(CurrentUser.getUserId())) {
                throw new ForbiddenException("CRM_ACCESS_DENIED", "You can only access leads assigned to you.");
            }
        }
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('LEAD_EDIT')")
    public OpportunityDto qualifyLead(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Lead lead = leadRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("LEAD_NOT_FOUND", "Lead not found with id: " + id));

        if (lead.getStatus() == LeadStatus.CONVERTED) {
            throw new BusinessException("LEAD_ALREADY_CONVERTED", "Cannot qualify a converted lead.");
        }

        lead.setStatus(LeadStatus.QUALIFIED);
        leadRepository.save(lead);

        // Ensure default stages exist
        stageService.initDefaultStagesIfEmpty(companyId);

        // Find "Qualified" stage or first stage
        CrmPipelineStage targetStage = stageRepository.findByCompanyIdAndName(companyId, "Qualified")
                .orElseGet(() -> stageRepository.findByCompanyIdOrderBySortOrderAsc(companyId).stream().findFirst()
                        .orElseThrow(() -> new BusinessException("STAGE_NOT_FOUND", "No pipeline stage available.")));

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Company not found: " + companyId));

        Opportunity opp = new Opportunity();
        opp.setCompanyId(companyId);
        String oppNo = numberSeriesService.generateNextNumber(companyId, DocType.OPPORTUNITY);
        opp.setOpportunityNo(oppNo);

        String title = (lead.getCompanyName() != null && !lead.getCompanyName().isBlank())
                ? lead.getCompanyName().trim() + " - Deal"
                : lead.getContactName().trim() + " - Deal";
        opp.setTitle(title);
        opp.setLeadId(lead.getId());
        opp.setCustomerId(lead.getConvertedCustomerId());
        opp.setStageId(targetStage.getId());
        opp.setExpectedValue(BigDecimal.ZERO);
        opp.setCurrencyCode(company.getBaseCurrency());
        opp.setProbabilityPct(targetStage.getProbabilityPct());
        opp.setOwnerUserId(lead.getOwnerUserId() != null ? lead.getOwnerUserId() : CurrentUser.getUserId());

        // Find primary contact if exists
        contactRepository.findByCompanyIdAndLeadId(companyId, lead.getId()).stream().findFirst()
                .ifPresent(c -> opp.setContactId(c.getId()));

        Opportunity saved = opportunityRepository.save(opp);
        return toOpportunityDto(saved, targetStage.getName());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('LEAD_CREATE')")
    public LeadImportResultDto importLeadsFromCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "CSV file is empty or missing.");
        }

        Long companyId = CurrentUser.getCompanyId();
        int total = 0;
        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                return new LeadImportResultDto(0, 0, 0, List.of("File is completely empty"));
            }

            // Remove UTF-8 BOM if present
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }

            List<String> headers = parseCsvLine(headerLine);
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                headerIndex.put(headers.get(i).trim().toLowerCase(Locale.ROOT), i);
            }

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.trim().isEmpty()) continue;

                total++;
                List<String> columns = parseCsvLine(line);

                String contactName = getColValue(columns, headerIndex, "contactname", "contact_name", "contact", "name", "fullname", "full_name");
                if (contactName == null || contactName.isBlank()) {
                    skipped++;
                    errors.add("Row " + rowNumber + ": Contact name is missing.");
                    continue;
                }

                String companyName = getColValue(columns, headerIndex, "companyname", "company_name", "company", "organization");
                String email = getColValue(columns, headerIndex, "email", "e-mail", "mail");
                String phone = getColValue(columns, headerIndex, "phone", "telephone", "mobile");
                String sourceStr = getColValue(columns, headerIndex, "source", "leadsource", "lead_source");
                String industry = getColValue(columns, headerIndex, "industry", "sector");
                String city = getColValue(columns, headerIndex, "city");
                String countryCode = getColValue(columns, headerIndex, "countrycode", "country_code", "country");
                String notes = getColValue(columns, headerIndex, "notes", "note", "comment");

                LeadSource source = LeadSource.IMPORT;
                if (sourceStr != null && !sourceStr.isBlank()) {
                    try {
                        source = LeadSource.valueOf(sourceStr.trim().toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ignored) {
                        source = LeadSource.IMPORT;
                    }
                }

                Lead lead = new Lead();
                lead.setCompanyId(companyId);
                String leadNo = numberSeriesService.generateNextNumber(companyId, DocType.LEAD);
                lead.setLeadNo(leadNo);
                lead.setContactName(contactName.trim());
                lead.setCompanyName(companyName != null && !companyName.isBlank() ? companyName.trim() : null);
                lead.setEmail(email != null && !email.isBlank() ? email.trim() : null);
                lead.setPhone(phone != null && !phone.isBlank() ? phone.trim() : null);
                lead.setSource(source);
                lead.setIndustry(industry != null && !industry.isBlank() ? industry.trim() : null);
                lead.setCity(city != null && !city.isBlank() ? city.trim() : null);
                lead.setCountryCode(countryCode != null && countryCode.trim().length() <= 2 ? countryCode.trim().toUpperCase(Locale.ROOT) : "AE");
                lead.setNotes(notes != null && !notes.isBlank() ? notes.trim() : null);
                lead.setStatus(LeadStatus.NEW);
                lead.setOwnerUserId(CurrentUser.getUserId());

                leadRepository.save(lead);
                imported++;
            }

        } catch (Exception e) {
            throw new BusinessException("CSV_IMPORT_FAILED", "Failed to parse CSV file: " + e.getMessage());
        }

        return new LeadImportResultDto(total, imported, skipped, errors);
    }

    private void populateLeadFields(Lead lead, LeadRequest request) {
        lead.setContactName(request.contactName().trim());
        lead.setCompanyName(request.companyName() != null ? request.companyName().trim() : null);
        lead.setEmail(request.email() != null ? request.email().trim() : null);
        lead.setPhone(request.phone() != null ? request.phone().trim() : null);
        lead.setSource(request.source() != null ? request.source() : LeadSource.OTHER);
        lead.setIndustry(request.industry() != null ? request.industry().trim() : null);
        lead.setCity(request.city() != null ? request.city().trim() : null);
        lead.setCountryCode(request.countryCode() != null && request.countryCode().trim().length() <= 2
                ? request.countryCode().trim().toUpperCase(Locale.ROOT)
                : null);
        lead.setOwnerUserId(request.ownerUserId());
        lead.setNotes(request.notes());
    }

    private LeadDto toDto(Lead lead) {
        String ownerName = null;
        if (lead.getOwnerUserId() != null) {
            ownerName = userRepository.findById(lead.getOwnerUserId())
                    .map(User::getFullName)
                    .orElse("User #" + lead.getOwnerUserId());
        }

        String customerName = null;
        if (lead.getConvertedCustomerId() != null) {
            customerName = customerRepository.findById(lead.getConvertedCustomerId())
                    .map(Customer::getName)
                    .orElse(null);
        }

        return new LeadDto(
                lead.getId(),
                lead.getCompanyId(),
                lead.getLeadNo(),
                lead.getCompanyName(),
                lead.getContactName(),
                lead.getEmail(),
                lead.getPhone(),
                lead.getSource(),
                lead.getIndustry(),
                lead.getCity(),
                lead.getCountryCode(),
                lead.getStatus(),
                lead.getOwnerUserId(),
                ownerName,
                lead.getConvertedCustomerId(),
                customerName,
                lead.getConvertedAt(),
                lead.getNotes(),
                lead.getCreatedAt()
        );
    }

    private OpportunityDto toOpportunityDto(Opportunity opp, String stageName) {
        return new OpportunityDto(
                opp.getId(),
                opp.getCompanyId(),
                opp.getOpportunityNo(),
                opp.getTitle(),
                opp.getLeadId(),
                opp.getCustomerId(),
                null,
                opp.getContactId(),
                null,
                opp.getStageId(),
                stageName,
                opp.getExpectedValue(),
                opp.getCurrencyCode(),
                opp.getProbabilityPct(),
                opp.getExpectedCloseDate(),
                opp.getOwnerUserId(),
                null,
                opp.getLostReason(),
                opp.getClosedAt(),
                opp.getCreatedAt()
        );
    }

    private String getColValue(List<String> columns, Map<String, Integer> headerIndex, String... aliases) {
        for (String alias : aliases) {
            Integer idx = headerIndex.get(alias);
            if (idx != null && idx < columns.size()) {
                String val = columns.get(idx);
                if (val != null && !val.trim().isEmpty()) {
                    return val.trim();
                }
            }
        }
        return null;
    }

    private List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    sb.append('\"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens;
    }
}

