package com.backend.finledger.sales.service;

import com.backend.finledger.sales.dto.ArAgeingReportDto;

import java.time.LocalDate;

public interface ArAgeingService {

    ArAgeingReportDto getArAgeingReport(LocalDate asOfDate);
}

