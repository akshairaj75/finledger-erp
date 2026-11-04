package com.backend.finledger.crm.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.DocType;
import com.backend.finledger.common.NumberSeriesService;
import com.backend.finledger.common.PageResponse;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.crm.dto.*;
import com.backend.finledger.crm.entity.Contact;
import com.backend.finledger.crm.entity.CrmPipelineStage;
import com.backend.finledger.crm.entity.Opportunity;
import com.backend.finledger.crm.entity.StageType;
import com.backend.finledger.crm.repository.ContactRepository;
import com.backend.finledger.crm.repository.CrmPipelineStageRepository;
import com.backend.finledger.crm.repository.OpportunityRepository;
import com.backend.finledger.crm.service.CrmPipelineStageService;
import com.backend.finledger.crm.service.OpportunityService;
import com.backend.finledger.sales.entity.Customer;
import com.backend.finledger.sales.repository.CustomerRepository;
import com.backend.finledger.security.CurrentUser;
import com.backend.finledger.user.entity.User;
import com.backend.finledger.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class OpportunityServiceImpl implements OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final CrmPipelineStageRepository stageRepository;
    private final CrmPipelineStageService stageService;
    private final CustomerRepository customerRepository;
    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final NumberSeriesService numberSeriesService;

    public OpportunityServiceImpl(
            OpportunityRepository opportunityRepository,
            CrmPipelineStageRepository stageRepository,
            CrmPipelineStageService stageService,
            CustomerRepository customerRepository,
            ContactRepository contactRepository,
            UserRepository userRepository,
            CompanyRepository companyRepository,
            NumberSeriesService numberSeriesService) {
        this.opportunityRepository = opportunityRepository;
        this.stageRepository = stageRepository;
        this.stageService = stageService;
        this.customerRepository = customerRepository;
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.numberSeriesService = numberSeriesService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OpportunityDto> getOpportunities(Long stageId, Long customerId, Long ownerUserId, String q, Pageable pageable) {
        Long companyId = CurrentUser.getCompanyId();
        String search = (q != null && !q.isBlank()) ? q.trim() : null;

        Page<Opportunity> page = opportunityRepository.searchOpportunities(companyId, stageId, customerId, ownerUserId, search, pageable);
        List<OpportunityDto> dtos = page.getContent().stream().map(this::toDto).toList();
        return new PageResponse<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public OpportunityDto getOpportunityById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Opportunity opp = opportunityRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("OPPORTUNITY_NOT_FOUND", "Opportunity not found with id: " + id));
        return toDto(opp);
    }

    @Override
    @Transactional
    public OpportunityBoardDto getOpportunityBoard() {
        Long companyId = CurrentUser.getCompanyId();
        stageService.initDefaultStagesIfEmpty(companyId);

        List<CrmPipelineStage> stages = stageRepository.findByCompanyIdOrderBySortOrderAsc(companyId);
        List<Opportunity> allOpps = opportunityRepository.findByCompanyId(companyId);

        // Group opportunities by stageId
        Map<Long, List<Opportunity>> oppsByStage = new HashMap<>();
        for (Opportunity o : allOpps) {
            oppsByStage.computeIfAbsent(o.getStageId(), k -> new ArrayList<>()).add(o);
        }

        List<OpportunityStageColumnDto> columns = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        int totalCount = allOpps.size();

        for (CrmPipelineStage stage : stages) {
            List<Opportunity> stageOpps = oppsByStage.getOrDefault(stage.getId(), Collections.emptyList());
            BigDecimal colTotal = BigDecimal.ZERO;

            List<OpportunityDto> oppDtos = new ArrayList<>();
            for (Opportunity opp : stageOpps) {
                colTotal = colTotal.add(opp.getExpectedValue() != null ? opp.getExpectedValue() : BigDecimal.ZERO);
                oppDtos.add(toDto(opp));
            }
            grandTotal = grandTotal.add(colTotal);

            columns.add(new OpportunityStageColumnDto(
                    stage.getId(),
                    stage.getName(),
                    stage.getSortOrder(),
                    stage.getProbabilityPct(),
                    stage.getStageType(),
                    colTotal,
                    stageOpps.size(),
                    oppDtos
            ));
        }

        return new OpportunityBoardDto(columns, grandTotal, totalCount);
    }

    @Override
    @Transactional
    public OpportunityDto createOpportunity(OpportunityRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        CrmPipelineStage stage = stageRepository.findByCompanyIdAndId(companyId, request.stageId())
                .orElseThrow(() -> new BusinessException("STAGE_NOT_FOUND", "Pipeline stage not found with id: " + request.stageId()));

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Company not found: " + companyId));

        Opportunity opp = new Opportunity();
        opp.setCompanyId(companyId);

        String oppNo = numberSeriesService.generateNextNumber(companyId, DocType.OPPORTUNITY);
        opp.setOpportunityNo(oppNo);

        opp.setTitle(request.title().trim());
        opp.setLeadId(request.leadId());
        opp.setCustomerId(request.customerId());
        opp.setContactId(request.contactId());
        opp.setStageId(stage.getId());
        opp.setExpectedValue(request.expectedValue() != null ? request.expectedValue() : BigDecimal.ZERO);
        opp.setCurrencyCode(request.currencyCode() != null && !request.currencyCode().isBlank()
                ? request.currencyCode().toUpperCase(Locale.ROOT)
                : company.getBaseCurrency());
        opp.setProbabilityPct(request.probabilityPct() != null ? request.probabilityPct() : stage.getProbabilityPct());
        opp.setExpectedCloseDate(request.expectedCloseDate());
        opp.setOwnerUserId(request.ownerUserId() != null ? request.ownerUserId() : CurrentUser.getUserId());

        // Validate customer requirement if initial stage is WON
        if (stage.getStageType() == StageType.WON && opp.getCustomerId() == null) {
            throw new BusinessException("CUSTOMER_REQUIRED_FOR_WON", "Moving opportunity to WON stage requires a linked customer.");
        }

        Opportunity saved = opportunityRepository.save(opp);
        return toDto(saved);
    }

    @Override
    @Transactional
    public OpportunityDto updateOpportunity(Long id, OpportunityRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        Opportunity opp = opportunityRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("OPPORTUNITY_NOT_FOUND", "Opportunity not found with id: " + id));

        opp.setTitle(request.title().trim());
        opp.setLeadId(request.leadId());
        opp.setCustomerId(request.customerId());
        opp.setContactId(request.contactId());
        if (request.expectedValue() != null) opp.setExpectedValue(request.expectedValue());
        if (request.currencyCode() != null && !request.currencyCode().isBlank()) opp.setCurrencyCode(request.currencyCode().toUpperCase(Locale.ROOT));
        if (request.probabilityPct() != null) opp.setProbabilityPct(request.probabilityPct());
        opp.setExpectedCloseDate(request.expectedCloseDate());
        if (request.ownerUserId() != null) opp.setOwnerUserId(request.ownerUserId());

        if (request.stageId() != null && !request.stageId().equals(opp.getStageId())) {
            updateStageInternal(opp, request.stageId(), null, companyId);
        }

        Opportunity saved = opportunityRepository.save(opp);
        return toDto(saved);
    }

    @Override
    @Transactional
    public OpportunityDto updateOpportunityStage(Long id, OpportunityStageRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        Opportunity opp = opportunityRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("OPPORTUNITY_NOT_FOUND", "Opportunity not found with id: " + id));

        updateStageInternal(opp, request.stageId(), request.lostReason(), companyId);

        Opportunity saved = opportunityRepository.save(opp);
        return toDto(saved);
    }

    private void updateStageInternal(Opportunity opp, Long newStageId, String lostReason, Long companyId) {
        CrmPipelineStage targetStage = stageRepository.findByCompanyIdAndId(companyId, newStageId)
                .orElseThrow(() -> new BusinessException("STAGE_NOT_FOUND", "Pipeline stage not found with id: " + newStageId));

        // Rule 1: moving to WON stage requires a linked customer
        if (targetStage.getStageType() == StageType.WON) {
            if (opp.getCustomerId() == null) {
                throw new BusinessException("CUSTOMER_REQUIRED_FOR_WON",
                        "Moving opportunity to WON stage requires a linked customer.");
            }
            opp.setClosedAt(Instant.now());
            opp.setLostReason(null);
        }
        // Rule 2: moving to LOST stage requires a lost reason
        else if (targetStage.getStageType() == StageType.LOST) {
            if (lostReason == null || lostReason.trim().isEmpty()) {
                throw new BusinessException("LOST_REASON_REQUIRED",
                        "Moving opportunity to LOST stage requires a lost reason.");
            }
            opp.setLostReason(lostReason.trim());
            opp.setClosedAt(Instant.now());
        }
        // Moving to OPEN stage resets closure
        else {
            opp.setClosedAt(null);
            opp.setLostReason(null);
        }

        opp.setStageId(targetStage.getId());
        opp.setProbabilityPct(targetStage.getProbabilityPct());
    }

    private OpportunityDto toDto(Opportunity opp) {
        String stageName = stageRepository.findById(opp.getStageId())
                .map(CrmPipelineStage::getName)
                .orElse("Stage #" + opp.getStageId());

        String customerName = null;
        if (opp.getCustomerId() != null) {
            customerName = customerRepository.findById(opp.getCustomerId())
                    .map(Customer::getName)
                    .orElse(null);
        }

        String contactName = null;
        if (opp.getContactId() != null) {
            contactName = contactRepository.findById(opp.getContactId())
                    .map(Contact::getFullName)
                    .orElse(null);
        }

        String ownerName = null;
        if (opp.getOwnerUserId() != null) {
            ownerName = userRepository.findById(opp.getOwnerUserId())
                    .map(User::getFullName)
                    .orElse("User #" + opp.getOwnerUserId());
        }

        return new OpportunityDto(
                opp.getId(),
                opp.getCompanyId(),
                opp.getOpportunityNo(),
                opp.getTitle(),
                opp.getLeadId(),
                opp.getCustomerId(),
                customerName,
                opp.getContactId(),
                contactName,
                opp.getStageId(),
                stageName,
                opp.getExpectedValue(),
                opp.getCurrencyCode(),
                opp.getProbabilityPct(),
                opp.getExpectedCloseDate(),
                opp.getOwnerUserId(),
                ownerName,
                opp.getLostReason(),
                opp.getClosedAt(),
                opp.getCreatedAt()
        );
    }
}

