package com.backend.finledger.ledger;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.ledger.dto.CreateJournalEntryRequest;
import com.backend.finledger.ledger.dto.JournalEntryDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/journal-entries")
public class JournalEntryController {

    private final PostingService postingService;

    public JournalEntryController(PostingService postingService) {
        this.postingService = postingService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<JournalEntryDto>> getJournalEntries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) JournalStatus status,
            @RequestParam(required = false) JournalSourceType sourceType,
            @PageableDefault(sort = "entryDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(postingService.getJournalEntries(from, to, status, sourceType, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JournalEntryDto> getJournalEntryById(@PathVariable Long id) {
        return ResponseEntity.ok(postingService.getJournalEntryById(id));
    }

    @PostMapping
    public ResponseEntity<JournalEntryDto> createJournalEntry(@Valid @RequestBody CreateJournalEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postingService.createJournalEntry(request));
    }

    @PostMapping("/{id}/post")
    public ResponseEntity<JournalEntryDto> postJournalEntry(@PathVariable Long id) {
        return ResponseEntity.ok(postingService.postJournalEntry(id));
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<JournalEntryDto> reverseJournalEntry(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = (body != null) ? body.get("reason") : null;
        return ResponseEntity.ok(postingService.reverseJournalEntry(id, reason));
    }
}
