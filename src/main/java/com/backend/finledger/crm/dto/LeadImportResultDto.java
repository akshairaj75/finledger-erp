package com.backend.finledger.crm.dto;

import java.util.List;

public record LeadImportResultDto(
    int totalProcessed,
    int importedCount,
    int skippedCount,
    List<String> errors
) {}

