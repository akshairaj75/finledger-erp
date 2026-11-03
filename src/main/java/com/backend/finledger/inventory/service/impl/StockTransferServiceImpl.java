package com.backend.finledger.inventory.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.DocType;
import com.backend.finledger.common.NumberSeriesService;
import com.backend.finledger.common.PageResponse;
import com.backend.finledger.inventory.dto.*;
import com.backend.finledger.inventory.entity.*;
import com.backend.finledger.inventory.repository.*;
import com.backend.finledger.inventory.service.CostingService;
import com.backend.finledger.inventory.service.StockService;
import com.backend.finledger.inventory.service.StockTransferService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class StockTransferServiceImpl implements StockTransferService {

    private final StockTransferRepository transferRepository;
    private final StockTransferLineRepository lineRepository;
    private final WarehouseRepository warehouseRepository;
    private final ItemRepository itemRepository;
    private final NumberSeriesService numberSeriesService;
    private final StockService stockService;
    private final CostingService costingService;

    public StockTransferServiceImpl(
            StockTransferRepository transferRepository,
            StockTransferLineRepository lineRepository,
            WarehouseRepository warehouseRepository,
            ItemRepository itemRepository,
            NumberSeriesService numberSeriesService,
            StockService stockService,
            CostingService costingService) {
        this.transferRepository = transferRepository;
        this.lineRepository = lineRepository;
        this.warehouseRepository = warehouseRepository;
        this.itemRepository = itemRepository;
        this.numberSeriesService = numberSeriesService;
        this.stockService = stockService;
        this.costingService = costingService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockTransferDto> getTransfers(Pageable pageable) {
        Long companyId = CurrentUser.getCompanyId();
        Page<StockTransfer> page = transferRepository.findByCompanyId(companyId, pageable);
        List<StockTransferDto> dtos = page.getContent().stream()
                .map(this::enrichTransferDto)
                .toList();
        return new PageResponse<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public StockTransferDto getTransferById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        StockTransfer trf = transferRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("TRANSFER_NOT_FOUND", "Stock transfer not found with id: " + id));
        return enrichTransferDto(trf);
    }

    @Override
    public StockTransferDto createDraftTransfer(StockTransferRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        if (request.fromWarehouseId().equals(request.toWarehouseId())) {
            throw new BusinessException("SAME_WAREHOUSE", "Source and destination warehouses cannot be the same");
        }

        warehouseRepository.findByCompanyIdAndId(companyId, request.fromWarehouseId())
                .orElseThrow(() -> new BusinessException("WAREHOUSE_NOT_FOUND", "Source warehouse not found"));
        warehouseRepository.findByCompanyIdAndId(companyId, request.toWarehouseId())
                .orElseThrow(() -> new BusinessException("WAREHOUSE_NOT_FOUND", "Destination warehouse not found"));

        StockTransfer trf = new StockTransfer();
        trf.setCompanyId(companyId);
        trf.setTransferNo("TRF-DRAFT-" + System.currentTimeMillis());
        trf.setTransferDate(request.transferDate() != null ? request.transferDate() : LocalDate.now());
        trf.setFromWarehouseId(request.fromWarehouseId());
        trf.setToWarehouseId(request.toWarehouseId());
        trf.setStatus(TransferStatus.DRAFT);
        trf.setNotes(request.notes());

        for (StockTransferLineRequest lReq : request.lines()) {
            Item item = itemRepository.findByCompanyIdAndId(companyId, lReq.itemId())
                    .orElseThrow(() -> new BusinessException("ITEM_NOT_FOUND", "Item not found with id: " + lReq.itemId()));

            StockTransferLine line = new StockTransferLine();
            line.setItemId(item.getId());
            line.setQuantity(lReq.quantity());
            line.setUnitCost(BigDecimal.ZERO);
            trf.addLine(line);
        }

        trf = transferRepository.save(trf);
        return enrichTransferDto(trf);
    }

    @Override
    public StockTransferDto dispatchTransfer(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        StockTransfer trf = transferRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("TRANSFER_NOT_FOUND", "Stock transfer not found with id: " + id));

        if (trf.getStatus() != TransferStatus.DRAFT) {
            throw new BusinessException("INVALID_STATUS", "Only draft transfers can be dispatched. Current status: " + trf.getStatus());
        }

        String permanentNo = numberSeriesService.generateNextNumber(companyId, DocType.TRANSFER);
        trf.setTransferNo(permanentNo);

        // Issue stock from source warehouse
        for (StockTransferLine line : trf.getLines()) {
            BigDecimal currentCost = costingService.getCurrentAverageCost(line.getItemId(), trf.getFromWarehouseId());
            line.setUnitCost(currentCost);

            stockService.issueStock(
                    companyId,
                    line.getItemId(),
                    trf.getFromWarehouseId(),
                    line.getQuantity(),
                    MovementType.TRANSFER_OUT,
                    StockSourceType.TRANSFER,
                    trf.getId(),
                    line.getId(),
                    null
            );
        }

        trf.setStatus(TransferStatus.IN_TRANSIT);
        trf = transferRepository.save(trf);
        return enrichTransferDto(trf);
    }

    @Override
    public StockTransferDto receiveTransfer(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        StockTransfer trf = transferRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("TRANSFER_NOT_FOUND", "Stock transfer not found with id: " + id));

        if (trf.getStatus() != TransferStatus.IN_TRANSIT) {
            throw new BusinessException("INVALID_STATUS", "Only in-transit transfers can be received. Current status: " + trf.getStatus());
        }

        // Receive stock into destination warehouse using the captured unitCost from dispatch
        for (StockTransferLine line : trf.getLines()) {
            stockService.receiveStock(
                    companyId,
                    line.getItemId(),
                    trf.getToWarehouseId(),
                    line.getQuantity(),
                    line.getUnitCost(),
                    MovementType.TRANSFER_IN,
                    StockSourceType.TRANSFER,
                    trf.getId(),
                    line.getId(),
                    null
            );
        }

        trf.setStatus(TransferStatus.RECEIVED);
        trf = transferRepository.save(trf);
        return enrichTransferDto(trf);
    }

    @Override
    public StockTransferDto voidTransfer(Long id, String reason) {
        Long companyId = CurrentUser.getCompanyId();
        StockTransfer trf = transferRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("TRANSFER_NOT_FOUND", "Stock transfer not found with id: " + id));

        if (trf.getStatus() == TransferStatus.VOID) {
            throw new BusinessException("INVALID_STATUS", "Transfer is already voided");
        }

        if (trf.getStatus() == TransferStatus.IN_TRANSIT) {
            // Cancel dispatch by receiving back into source warehouse
            for (StockTransferLine line : trf.getLines()) {
                stockService.receiveStock(
                        companyId,
                        line.getItemId(),
                        trf.getFromWarehouseId(),
                        line.getQuantity(),
                        line.getUnitCost(),
                        MovementType.TRANSFER_IN,
                        StockSourceType.TRANSFER,
                        trf.getId(),
                        line.getId(),
                        null
                );
            }
        } else if (trf.getStatus() == TransferStatus.RECEIVED) {
            // Reverse receipt from destination warehouse and restore to source warehouse
            for (StockTransferLine line : trf.getLines()) {
                stockService.issueStock(
                        companyId,
                        line.getItemId(),
                        trf.getToWarehouseId(),
                        line.getQuantity(),
                        MovementType.TRANSFER_OUT,
                        StockSourceType.TRANSFER,
                        trf.getId(),
                        line.getId(),
                        null
                );
                stockService.receiveStock(
                        companyId,
                        line.getItemId(),
                        trf.getFromWarehouseId(),
                        line.getQuantity(),
                        line.getUnitCost(),
                        MovementType.TRANSFER_IN,
                        StockSourceType.TRANSFER,
                        trf.getId(),
                        line.getId(),
                        null
                );
            }
        }

        trf.setStatus(TransferStatus.VOID);
        trf = transferRepository.save(trf);
        return enrichTransferDto(trf);
    }

    private StockTransferDto enrichTransferDto(StockTransfer trf) {
        String fromWhName = warehouseRepository.findById(trf.getFromWarehouseId()).map(Warehouse::getName).orElse(null);
        String toWhName = warehouseRepository.findById(trf.getToWarehouseId()).map(Warehouse::getName).orElse(null);

        Map<Long, Item> itemsById = itemRepository.findAllById(
                trf.getLines().stream().map(StockTransferLine::getItemId).toList()
        ).stream().collect(Collectors.toMap(Item::getId, i -> i));

        List<StockTransferLineDto> lineDtos = trf.getLines().stream().map(l -> {
            Item item = itemsById.get(l.getItemId());
            String sku = item != null ? item.getSku() : "";
            String name = item != null ? item.getName() : "";
            return new StockTransferLineDto(
                    l.getId(),
                    l.getItemId(),
                    sku,
                    name,
                    l.getQuantity(),
                    l.getUnitCost()
            );
        }).toList();

        return new StockTransferDto(
                trf.getId(),
                trf.getCompanyId(),
                trf.getTransferNo(),
                trf.getTransferDate(),
                trf.getFromWarehouseId(),
                fromWhName,
                trf.getToWarehouseId(),
                toWhName,
                trf.getStatus(),
                trf.getNotes(),
                lineDtos
        );
    }
}

