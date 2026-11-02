package com.backend.finledger.ledger;

import com.backend.finledger.ledger.dto.CostCenterDto;
import com.backend.finledger.ledger.dto.CostCenterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cost-centers")
public class CostCenterController {

    private final CostCenterService costCenterService;

    public CostCenterController(CostCenterService costCenterService) {
        this.costCenterService = costCenterService;
    }

    @GetMapping
    public ResponseEntity<List<CostCenterDto>> getCostCenters(
            @RequestParam(required = false) Boolean activeOnly) {
        return ResponseEntity.ok(costCenterService.getCostCenters(activeOnly));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CostCenterDto> getCostCenterById(@PathVariable Long id) {
        return ResponseEntity.ok(costCenterService.getCostCenterById(id));
    }

    @PostMapping
    public ResponseEntity<CostCenterDto> createCostCenter(@Valid @RequestBody CostCenterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(costCenterService.createCostCenter(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CostCenterDto> updateCostCenter(
            @PathVariable Long id,
            @Valid @RequestBody CostCenterRequest request) {
        return ResponseEntity.ok(costCenterService.updateCostCenter(id, request));
    }
}
