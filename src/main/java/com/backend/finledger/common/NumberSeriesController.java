package com.backend.finledger.common;

import com.backend.finledger.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/number-series")
public class NumberSeriesController {

    private final NumberSeriesRepository numberSeriesRepository;

    public NumberSeriesController(NumberSeriesRepository numberSeriesRepository) {
        this.numberSeriesRepository = numberSeriesRepository;
    }

    public record NumberSeriesDto(Long id, DocType docType, String prefix, Integer nextNumber, Byte padding) {}

    public record NumberSeriesUpdateRequest(
            @NotBlank(message = "Prefix is required") String prefix,
            @NotNull @Min(1) Integer nextNumber,
            @NotNull @Min(1) Byte padding
    ) {}

    @GetMapping
    @PreAuthorize("hasAuthority('SETTINGS_VIEW') or hasAuthority('SETTINGS_EDIT') or hasAuthority('COMPANY_VIEW')")
    public ResponseEntity<List<NumberSeriesDto>> listSeries() {
        Long companyId = CurrentUser.getCompanyId();
        List<NumberSeriesDto> result = numberSeriesRepository.findByCompanyIdOrderByDocTypeAsc(companyId).stream()
                .map(ns -> new NumberSeriesDto(ns.getId(), ns.getDocType(), ns.getPrefix(), ns.getNextNumber(), ns.getPadding()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<NumberSeriesDto> updateSeries(
            @PathVariable Long id,
            @Valid @RequestBody NumberSeriesUpdateRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        NumberSeries ns = numberSeriesRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("NUMBER_SERIES_NOT_FOUND", "Number series not found with id: " + id));
        ns.setPrefix(request.prefix().trim());
        ns.setNextNumber(request.nextNumber());
        ns.setPadding(request.padding());
        NumberSeries saved = numberSeriesRepository.save(ns);
        return ResponseEntity.ok(new NumberSeriesDto(saved.getId(), saved.getDocType(), saved.getPrefix(), saved.getNextNumber(), saved.getPadding()));
    }
}

