package com.backend.finledger.crm.service;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.crm.dto.LeadDto;
import com.backend.finledger.crm.dto.LeadImportResultDto;
import com.backend.finledger.crm.dto.LeadRequest;
import com.backend.finledger.crm.dto.OpportunityDto;
import com.backend.finledger.crm.entity.LeadSource;
import com.backend.finledger.crm.entity.LeadStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface LeadService {

    PageResponse<LeadDto> getLeads(LeadStatus status, LeadSource source, Long ownerUserId, String q, Pageable pageable);

    LeadDto getLeadById(Long id);

    LeadDto createLead(LeadRequest request);

    LeadDto updateLead(Long id, LeadRequest request);

    OpportunityDto qualifyLead(Long id);

    LeadImportResultDto importLeadsFromCsv(MultipartFile file);
}

