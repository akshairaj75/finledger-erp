package com.backend.finledger.banking.service.impl;

import com.backend.finledger.banking.dto.BankStatementDto;
import com.backend.finledger.banking.dto.BankStatementLineDto;
import com.backend.finledger.banking.entity.BankAccount;
import com.backend.finledger.banking.entity.BankStatement;
import com.backend.finledger.banking.entity.BankStatementLine;
import com.backend.finledger.banking.entity.MatchStatus;
import com.backend.finledger.banking.repository.BankAccountRepository;
import com.backend.finledger.banking.repository.BankStatementLineRepository;
import com.backend.finledger.banking.repository.BankStatementRepository;
import com.backend.finledger.banking.service.BankStatementService;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@PreAuthorize("hasAuthority('BANK_RECONCILE')")
public class BankStatementServiceImpl implements BankStatementService {

    private final BankStatementRepository bankStatementRepository;
    private final BankStatementLineRepository bankStatementLineRepository;
    private final BankAccountRepository bankAccountRepository;

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,                     // 2026-10-31
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),             // 31-10-2026
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),             // 31/10/2026
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),             // 10/31/2026
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),             // 2026/10/31
            DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH), // 31-Oct-2026
            DateTimeFormatter.ofPattern("d/MMM/yyyy", Locale.ENGLISH)
    );

    public BankStatementServiceImpl(
            BankStatementRepository bankStatementRepository,
            BankStatementLineRepository bankStatementLineRepository,
            BankAccountRepository bankAccountRepository) {
        this.bankStatementRepository = bankStatementRepository;
        this.bankStatementLineRepository = bankStatementLineRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    @Override
    @Transactional
    public BankStatementDto importStatement(
            MultipartFile file,
            Long bankAccountId,
            LocalDate statementDate,
            BigDecimal openingBalance,
            BigDecimal closingBalance) {

        Long companyId = CurrentUser.getCompanyId();
        BankAccount bankAccount = bankAccountRepository.findByCompanyIdAndId(companyId, bankAccountId)
                .orElseThrow(() -> new BusinessException("BANK_ACCOUNT_NOT_FOUND", "Bank account not found: " + bankAccountId));

        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "Uploaded statement file cannot be empty");
        }

        List<ParsedLine> parsedLines = parseCsv(file);
        if (parsedLines.isEmpty()) {
            throw new BusinessException("NO_LINES_FOUND", "No valid transaction rows found in CSV file");
        }

        // Determine statement date from max transaction date if not provided
        LocalDate finalStatementDate = statementDate;
        if (finalStatementDate == null) {
            finalStatementDate = parsedLines.stream()
                    .map(ParsedLine::date)
                    .max(LocalDate::compareTo)
                    .orElse(LocalDate.now());
        }

        BigDecimal finalOpening = openingBalance != null ? openingBalance : BigDecimal.ZERO;
        BigDecimal netChange = parsedLines.stream()
                .map(ParsedLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal finalClosing = closingBalance != null ? closingBalance : finalOpening.add(netChange);

        BankStatement statement = new BankStatement();
        statement.setBankAccountId(bankAccountId);
        statement.setStatementDate(finalStatementDate);
        statement.setOpeningBalance(finalOpening);
        statement.setClosingBalance(finalClosing);
        statement.setFileName(file.getOriginalFilename());

        BankStatement savedStatement = bankStatementRepository.save(statement);

        List<BankStatementLine> linesToSave = new ArrayList<>();
        for (ParsedLine pl : parsedLines) {
            BankStatementLine bsl = new BankStatementLine();
            bsl.setBankStatement(savedStatement);
            bsl.setTxnDate(pl.date());
            bsl.setDescription(pl.description());
            bsl.setReference(pl.reference());
            bsl.setAmount(pl.amount());
            bsl.setMatchStatus(MatchStatus.UNMATCHED);
            linesToSave.add(bsl);
        }

        List<BankStatementLine> savedLines = bankStatementLineRepository.saveAll(linesToSave);
        savedStatement.setLines(savedLines);

        return toDto(savedStatement, savedLines);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankStatementDto> getStatementsByBankAccountId(Long bankAccountId) {
        Long companyId = CurrentUser.getCompanyId();
        bankAccountRepository.findByCompanyIdAndId(companyId, bankAccountId)
                .orElseThrow(() -> new BusinessException("BANK_ACCOUNT_NOT_FOUND", "Bank account not found: " + bankAccountId));

        return bankStatementRepository.findByBankAccountIdOrderByStatementDateDesc(bankAccountId).stream()
                .map(s -> {
                    List<BankStatementLine> lines = bankStatementLineRepository.findByBankStatementIdOrderByTxnDateAsc(s.getId());
                    return toDto(s, lines);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BankStatementDto getStatementById(Long id) {
        BankStatement statement = bankStatementRepository.findById(id)
                .orElseThrow(() -> new BusinessException("STATEMENT_NOT_FOUND", "Bank statement not found: " + id));

        Long companyId = CurrentUser.getCompanyId();
        bankAccountRepository.findByCompanyIdAndId(companyId, statement.getBankAccountId())
                .orElseThrow(() -> new BusinessException("BANK_ACCOUNT_NOT_FOUND", "Unauthorized access to statement"));

        List<BankStatementLine> lines = bankStatementLineRepository.findByBankStatementIdOrderByTxnDateAsc(statement.getId());
        return toDto(statement, lines);
    }

    private record ParsedLine(LocalDate date, String description, String reference, BigDecimal amount) {}

    private List<ParsedLine> parseCsv(MultipartFile file) {
        List<ParsedLine> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return result;

            // Remove UTF-8 BOM if present
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }

            char separator = detectSeparator(headerLine);
            String[] headers = parseCsvLine(headerLine, separator);

            int dateIdx = -1;
            int descIdx = -1;
            int refIdx = -1;
            int amountIdx = -1;
            int depositIdx = -1;
            int withdrawalIdx = -1;

            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].toLowerCase().trim().replace("\"", "").replace("_", " ");
                if (h.contains("date")) dateIdx = i;
                else if (h.contains("desc") || h.contains("narrat") || h.contains("detail") || h.contains("memo") || h.contains("remark") || h.contains("particular")) descIdx = i;
                else if (h.contains("ref") || h.contains("chq") || h.contains("cheque") || h.contains("trans") || h.contains("id")) refIdx = i;
                else if (h.equals("amount") || h.contains("net amount")) amountIdx = i;
                else if (h.contains("deposit") || h.contains("credit") || h.equals("in")) depositIdx = i;
                else if (h.contains("withdrawal") || h.contains("debit") || h.equals("out")) withdrawalIdx = i;
            }

            if (dateIdx == -1) dateIdx = 0; // fallback to column 0
            if (descIdx == -1 && headers.length > 1) descIdx = 1;
            if (amountIdx == -1 && depositIdx == -1 && headers.length > 2) amountIdx = headers.length - 1;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] tokens = parseCsvLine(line, separator);
                if (tokens.length <= dateIdx) continue;

                LocalDate date = parseDate(tokens[dateIdx].trim());
                if (date == null) continue; // Skip unparseable date rows

                String desc = (descIdx != -1 && tokens.length > descIdx) ? tokens[descIdx].trim() : "Bank transaction";
                String ref = (refIdx != -1 && tokens.length > refIdx) ? tokens[refIdx].trim() : null;

                BigDecimal amount = BigDecimal.ZERO;
                if (amountIdx != -1 && tokens.length > amountIdx) {
                    amount = parseAmount(tokens[amountIdx]);
                } else if (depositIdx != -1 || withdrawalIdx != -1) {
                    BigDecimal deposit = depositIdx != -1 && tokens.length > depositIdx ? parseAmount(tokens[depositIdx]) : BigDecimal.ZERO;
                    BigDecimal withdrawal = withdrawalIdx != -1 && tokens.length > withdrawalIdx ? parseAmount(tokens[withdrawalIdx]) : BigDecimal.ZERO;
                    amount = deposit.subtract(withdrawal.abs());
                }

                result.add(new ParsedLine(date, desc, ref, amount));
            }
        } catch (Exception e) {
            throw new BusinessException("CSV_PARSE_ERROR", "Error parsing CSV bank statement: " + e.getMessage());
        }
        return result;
    }

    private char detectSeparator(String headerLine) {
        if (headerLine.contains("\t")) return '\t';
        if (headerLine.contains(";")) return ';';
        return ',';
    }

    private String[] parseCsvLine(String line, char separator) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == separator && !inQuotes) {
                tokens.add(sb.toString().trim().replaceAll("^\"|\"$", ""));
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim().replaceAll("^\"|\"$", ""));
        return tokens.toArray(new String[0]);
    }

    private LocalDate parseDate(String raw) {
        String clean = raw.replaceAll("^\"|\"$", "").trim();
        for (DateTimeFormatter dtf : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(clean, dtf);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private BigDecimal parseAmount(String raw) {
        if (raw == null || raw.trim().isEmpty()) return BigDecimal.ZERO;
        String clean = raw.replaceAll("^\"|\"$", "")
                .replace(",", "")
                .replace("AED", "")
                .replace("$", "")
                .trim();
        try {
            return new BigDecimal(clean);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private BankStatementDto toDto(BankStatement s, List<BankStatementLine> lines) {
        List<BankStatementLineDto> lineDtos = lines.stream()
                .map(l -> new BankStatementLineDto(
                        l.getId(),
                        s.getId(),
                        l.getTxnDate(),
                        l.getDescription(),
                        l.getReference(),
                        l.getAmount(),
                        l.getMatchedJournalLineId(),
                        l.getMatchStatus()
                ))
                .collect(Collectors.toList());

        return new BankStatementDto(
                s.getId(),
                s.getBankAccountId(),
                s.getStatementDate(),
                s.getOpeningBalance(),
                s.getClosingBalance(),
                s.getFileName(),
                lineDtos
        );
    }
}

