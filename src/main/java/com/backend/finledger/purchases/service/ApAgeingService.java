package com.backend.finledger.purchases.service;

import com.backend.finledger.purchases.dto.ApAgeingReportDto;

import java.time.LocalDate;

public interface ApAgeingService {

    ApAgeingReportDto getApAgeingReport(LocalDate asOfDate);
}

