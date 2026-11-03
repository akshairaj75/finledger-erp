package com.backend.finledger.ledger.service;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.ledger.dto.CreateJournalEntryRequest;
import com.backend.finledger.ledger.dto.JournalEntryDto;
import com.backend.finledger.ledger.dto.SourceDocumentPostingRequest;
import com.backend.finledger.ledger.entity.FiscalPeriod;
import com.backend.finledger.ledger.entity.JournalEntry;
import com.backend.finledger.ledger.entity.JournalSourceType;
import com.backend.finledger.ledger.entity.JournalStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface PostingService {
    PageResponse<JournalEntryDto> getJournalEntries(
            LocalDate from, LocalDate to, JournalStatus status, JournalSourceType sourceType, Pageable pageable);

    JournalEntryDto getJournalEntryById(Long id);

    JournalEntryDto createJournalEntry(CreateJournalEntryRequest request);

    JournalEntryDto postJournalEntry(Long id);

    void validatePostingRules(JournalEntry entry, FiscalPeriod period);

    JournalEntryDto reverseJournalEntry(Long id, String reason);

    JournalEntryDto postDocument(SourceDocumentPostingRequest request);
}

