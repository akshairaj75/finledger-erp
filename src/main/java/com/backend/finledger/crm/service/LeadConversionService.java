package com.backend.finledger.crm.service;

import com.backend.finledger.crm.dto.LeadConvertRequest;
import com.backend.finledger.crm.dto.LeadConvertResponse;

public interface LeadConversionService {

    LeadConvertResponse convertLead(Long leadId, LeadConvertRequest request);
}

