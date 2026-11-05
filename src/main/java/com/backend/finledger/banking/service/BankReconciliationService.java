package com.backend.finledger.banking.service;

import com.backend.finledger.banking.dto.*;

import java.util.List;

public interface BankReconciliationService {
    BankReconciliationDto startReconciliation(BankReconciliationRequest request);
    BankReconciliationDto getReconciliationById(Long id);
    List<BankReconciliationDto> getReconciliationsByBankAccountId(Long bankAccountId);
    BankReconciliationDto match(Long reconciliationId, ReconciliationMatchRequest request);
    BankReconciliationDto unmatch(Long reconciliationId, Long statementLineId);
    AutoMatchResultDto autoMatch(Long reconciliationId);
    BankReconciliationDto createBankCharge(Long reconciliationId, BankChargeRequest request);
    BankReconciliationDto completeReconciliation(Long reconciliationId);
}

