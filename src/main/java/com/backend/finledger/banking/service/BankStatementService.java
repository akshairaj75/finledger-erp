package com.backend.finledger.banking.service;

import com.backend.finledger.banking.dto.BankStatementDto;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface BankStatementService {
    BankStatementDto importStatement(
            MultipartFile file,
            Long bankAccountId,
            LocalDate statementDate,
            BigDecimal openingBalance,
            BigDecimal closingBalance
    );

    List<BankStatementDto> getStatementsByBankAccountId(Long bankAccountId);

    BankStatementDto getStatementById(Long id);
}

