package com.backend.finledger.inventory.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.DocType;
import com.backend.finledger.common.NumberSeriesService;
import com.backend.finledger.common.PageResponse;
import com.backend.finledger.inventory.dto.*;
import com.backend.finledger.inventory.entity.*;
import com.backend.finledger.inventory.repository.*;
import com.backend.finledger.inventory.service.CostingService;
import com.backend.finledger.inventory.service.StockAdjustmentService;
import com.backend.finledger.inventory.service.StockService;
import com.backend.finledger.ledger.dto.JournalLineRequest;
import com.backend.finledger.ledger.dto.SourceDocumentPostingRequest;
import com.backend.finledger.ledger.entity.Account;
import com.backend.finledger.ledger.entity.JournalSourceType;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.ledger.service.PostingService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class StockAdjustmentServiceImpl implements StockAdjustmentService {

    private final StockAdjustmentRepository adjustmentRepository;
    private final StockAdjustmentLineRepository lineRepository;
    private final WarehouseRepository warehouseRepository;
    private final ItemRepository itemRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final AccountRepository accountRepository;
    private final NumberSeriesService numberSeriesService;
    private final PostingService postingService;
    private final StockService stockService;
    private final CostingService costingService;

    public StockAdjustmentServiceImpl(
            StockAdjustmentRepository adjustmentRepository,
            StockAdjustmentLineRepository lineRepository,
            WarehouseRepository warehouseRepository,
            ItemRepository itemRepository,
            StockBalanceRepository stockBalanceRepository,
            AccountRepository accountRepository,
            NumberSeriesService numberSeriesService,
            PostingService postingService,
            StockService stockService,
            CostingService costingService) {
        this.adjustmentRepository = adjustmentRepository;
        this.lineRepository = lineRepository;
        this.warehouseRepository = warehouseRepository;
        this.itemRepository = itemRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.accountRepository = accountRepository;
        this.numberSeriesService = numberSeriesService;
        this.postingService = postingService;
        this.stockService = stockService;
        this.costingService = costingService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockAdjustmentDto> getAdjustments(Pageable pageable) {
        Long companyId = CurrentUser.getCompanyId();
        Page<StockAdjustment> page = adjustmentRepository.findByCompanyId(companyId, pageable);
        List<StockAdjustmentDto> dtos = page.getContent().stream()
                .map(this::enrichAdjustmentDto)
                .toList();
        return new PageResponse<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public StockAdjustmentDto getAdjustmentById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        StockAdjustment adj = adjustmentRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("ADJUSTMENT_NOT_FOUND", "Stock adjustment not found with id: " + id));
        return enrichAdjustmentDto(adj);
    }

    @Override
    public StockAdjustmentDto createDraftAdjustment(StockAdjustmentRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        Warehouse warehouse = warehouseRepository.findByCompanyIdAndId(companyId, request.warehouseId())
                .orElseThrow(() -> new BusinessException("WAREHOUSE_NOT_FOUND", "Warehouse not found with id: " + request.warehouseId()));

        StockAdjustment adj = new StockAdjustment();
        adj.setCompanyId(companyId);
        adj.setAdjustmentNo("ADJ-DRAFT-" + System.currentTimeMillis());
        adj.setAdjustmentDate(request.adjustmentDate() != null ? request.adjustmentDate() : LocalDate.now());
        adj.setWarehouseId(warehouse.getId());
        adj.setReason(request.reason());
        adj.setAdjustmentAccountId(request.adjustmentAccountId());
        adj.setNotes(request.notes());
        adj.setStatus(AdjustmentStatus.DRAFT);

        BigDecimal totalVal = BigDecimal.ZERO;

        for (StockAdjustmentLineRequest lReq : request.lines()) {
            Item item = itemRepository.findByCompanyIdAndId(companyId, lReq.itemId())
                    .orElseThrow(() -> new BusinessException("ITEM_NOT_FOUND", "Item not found with id: " + lReq.itemId()));

            StockBalance balance = stockBalanceRepository.findByItemIdAndWarehouseId(item.getId(), warehouse.getId())
                    .orElse(new StockBalance(item.getId(), warehouse.getId()));

            BigDecimal systemQty = balance.getQtyOnHand();
            BigDecimal countedQty = lReq.countedQty();
            BigDecimal diffQty = countedQty.subtract(systemQty);

            BigDecimal unitCost = lReq.unitCost();
            if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) <= 0) {
                unitCost = costingService.getCurrentAverageCost(item.getId(), warehouse.getId());
            }

            BigDecimal lineValueImpact = diffQty.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
            totalVal = totalVal.add(lineValueImpact);

            StockAdjustmentLine line = new StockAdjustmentLine();
            line.setItemId(item.getId());
            line.setSystemQty(systemQty);
            line.setCountedQty(countedQty);
            line.setDifferenceQty(diffQty);
            line.setUnitCost(unitCost);
            line.setValueImpact(lineValueImpact);

            adj.addLine(line);
        }

        adj.setTotalValue(totalVal);
        adj = adjustmentRepository.save(adj);

        return enrichAdjustmentDto(adj);
    }

    @Override
    public StockAdjustmentDto postAdjustment(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        StockAdjustment adj = adjustmentRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("ADJUSTMENT_NOT_FOUND", "Stock adjustment not found with id: " + id));

        if (adj.getStatus() != AdjustmentStatus.DRAFT) {
            throw new BusinessException("INVALID_STATUS", "Only draft adjustments can be posted. Current status: " + adj.getStatus());
        }

        // Generate gap-free adjustment number
        String permanentNo = numberSeriesService.generateNextNumber(companyId, DocType.ADJUSTMENT);
        adj.setAdjustmentNo(permanentNo);

        List<JournalLineRequest> journalLines = new ArrayList<>();

        for (StockAdjustmentLine line : adj.getLines()) {
            BigDecimal diff = line.getDifferenceQty();
            if (diff.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            Item item = itemRepository.findByCompanyIdAndId(companyId, line.getItemId())
                    .orElseThrow(() -> new BusinessException("ITEM_NOT_FOUND", "Item not found with id: " + line.getItemId()));

            Long inventoryAccId = item.getInventoryAccountId();
            if (inventoryAccId == null) {
                throw new BusinessException("ACCOUNT_NOT_CONFIGURED", "Item " + item.getSku() + " has no Inventory GL account configured");
            }

            BigDecimal impactAbs = line.getValueImpact().abs();

            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                // Surplus: receive stock, Dr Inventory Asset, Cr Adjustment Account
                stockService.receiveStock(
                        companyId,
                        item.getId(),
                        adj.getWarehouseId(),
                        diff,
                        line.getUnitCost(),
                        MovementType.ADJUSTMENT_IN,
                        StockSourceType.ADJUSTMENT,
                        adj.getId(),
                        line.getId(),
                        null
                );

                if (impactAbs.compareTo(BigDecimal.ZERO) > 0) {
                    journalLines.add(new JournalLineRequest(
                            inventoryAccId,
                            null,
                            "Stock Surplus (" + item.getSku() + ") - " + adj.getAdjustmentNo(),
                            impactAbs,
                            BigDecimal.ZERO,
                            null,
                            null
                    ));
                    journalLines.add(new JournalLineRequest(
                            adj.getAdjustmentAccountId(),
                            null,
                            "Stock Surplus Offset - " + adj.getAdjustmentNo(),
                            BigDecimal.ZERO,
                            impactAbs,
                            null,
                            null
                    ));
                }
            } else {
                // Shortage: issue stock, Dr Adjustment Account, Cr Inventory Asset
                BigDecimal qtyToIssue = diff.abs();
                stockService.issueStock(
                        companyId,
                        item.getId(),
                        adj.getWarehouseId(),
                        qtyToIssue,
                        MovementType.ADJUSTMENT_OUT,
                        StockSourceType.ADJUSTMENT,
                        adj.getId(),
                        line.getId(),
                        null
                );

                if (impactAbs.compareTo(BigDecimal.ZERO) > 0) {
                    journalLines.add(new JournalLineRequest(
                            adj.getAdjustmentAccountId(),
                            null,
                            "Stock Shortage (" + item.getSku() + ") - " + adj.getAdjustmentNo(),
                            impactAbs,
                            BigDecimal.ZERO,
                            null,
                            null
                    ));
                    journalLines.add(new JournalLineRequest(
                            inventoryAccId,
                            null,
                            "Stock Shortage Offset - " + adj.getAdjustmentNo(),
                            BigDecimal.ZERO,
                            impactAbs,
                            null,
                            null
                    ));
                }
            }
        }

        // Post to General Ledger if there is any financial impact
        if (!journalLines.isEmpty()) {
            SourceDocumentPostingRequest postingRequest = new SourceDocumentPostingRequest(
                    JournalSourceType.STOCK_ADJUSTMENT,
                    adj.getId(),
                    adj.getAdjustmentDate(),
                    adj.getAdjustmentNo(),
                    "Stock Adjustment - " + adj.getReason() + " (" + adj.getAdjustmentNo() + ")",
                    "AED",
                    BigDecimal.ONE,
                    journalLines
            );
            var postedJournal = postingService.postDocument(postingRequest);
            adj.setJournalEntryId(postedJournal.id());
        }

        adj.setStatus(AdjustmentStatus.POSTED);
        adj = adjustmentRepository.save(adj);

        return enrichAdjustmentDto(adj);
    }

    @Override
    public StockAdjustmentDto voidAdjustment(Long id, String reason) {
        Long companyId = CurrentUser.getCompanyId();
        StockAdjustment adj = adjustmentRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("ADJUSTMENT_NOT_FOUND", "Stock adjustment not found with id: " + id));

        if (adj.getStatus() != AdjustmentStatus.POSTED) {
            throw new BusinessException("INVALID_STATUS", "Only posted adjustments can be voided. Current status: " + adj.getStatus());
        }

        // Reverse stock movements
        for (StockAdjustmentLine line : adj.getLines()) {
            BigDecimal diff = line.getDifferenceQty();
            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                // Was surplus: issue back
                stockService.issueStock(
                        companyId,
                        line.getItemId(),
                        adj.getWarehouseId(),
                        diff,
                        MovementType.ADJUSTMENT_OUT,
                        StockSourceType.ADJUSTMENT,
                        adj.getId(),
                        line.getId(),
                        null
                );
            } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                // Was shortage: receive back
                stockService.receiveStock(
                        companyId,
                        line.getItemId(),
                        adj.getWarehouseId(),
                        diff.abs(),
                        line.getUnitCost(),
                        MovementType.ADJUSTMENT_IN,
                        StockSourceType.ADJUSTMENT,
                        adj.getId(),
                        line.getId(),
                        null
                );
            }
        }

        // Reverse journal entry
        if (adj.getJournalEntryId() != null) {
            postingService.reverseJournalEntry(adj.getJournalEntryId(), reason);
        }

        adj.setStatus(AdjustmentStatus.VOID);
        adj = adjustmentRepository.save(adj);

        return enrichAdjustmentDto(adj);
    }

    private StockAdjustmentDto enrichAdjustmentDto(StockAdjustment adj) {
        String whName = warehouseRepository.findById(adj.getWarehouseId()).map(Warehouse::getName).orElse(null);
        String accName = accountRepository.findById(adj.getAdjustmentAccountId()).map(Account::getName).orElse(null);

        Map<Long, Item> itemsById = itemRepository.findAllById(
                adj.getLines().stream().map(StockAdjustmentLine::getItemId).toList()
        ).stream().collect(Collectors.toMap(Item::getId, i -> i));

        List<StockAdjustmentLineDto> lineDtos = adj.getLines().stream().map(l -> {
            Item item = itemsById.get(l.getItemId());
            String sku = item != null ? item.getSku() : "";
            String name = item != null ? item.getName() : "";
            return new StockAdjustmentLineDto(
                    l.getId(),
                    l.getItemId(),
                    sku,
                    name,
                    l.getSystemQty(),
                    l.getCountedQty(),
                    l.getDifferenceQty(),
                    l.getUnitCost(),
                    l.getValueImpact()
            );
        }).toList();

        return new StockAdjustmentDto(
                adj.getId(),
                adj.getCompanyId(),
                adj.getAdjustmentNo(),
                adj.getAdjustmentDate(),
                adj.getWarehouseId(),
                whName,
                adj.getReason(),
                adj.getAdjustmentAccountId(),
                accName,
                adj.getNotes(),
                adj.getTotalValue(),
                adj.getStatus(),
                adj.getJournalEntryId(),
                null,
                lineDtos
        );
    }
}

