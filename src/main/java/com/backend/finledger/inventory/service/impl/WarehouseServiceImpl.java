package com.backend.finledger.inventory.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.inventory.dto.WarehouseDto;
import com.backend.finledger.inventory.dto.WarehouseRequest;
import com.backend.finledger.inventory.entity.Warehouse;
import com.backend.finledger.inventory.mapper.InventoryMapper;
import com.backend.finledger.inventory.repository.WarehouseRepository;
import com.backend.finledger.inventory.service.WarehouseService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final InventoryMapper mapper;

    public WarehouseServiceImpl(WarehouseRepository warehouseRepository, InventoryMapper mapper) {
        this.warehouseRepository = warehouseRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseDto> getAllWarehouses() {
        Long companyId = CurrentUser.getCompanyId();
        return warehouseRepository.findByCompanyId(companyId).stream()
                .map(mapper::toWarehouseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseDto getWarehouseById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Warehouse wh = warehouseRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("WAREHOUSE_NOT_FOUND", "Warehouse not found with id: " + id));
        return mapper.toWarehouseDto(wh);
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseDto getWarehouseByCode(String code) {
        Long companyId = CurrentUser.getCompanyId();
        Warehouse wh = warehouseRepository.findByCompanyIdAndCode(companyId, code.trim())
                .orElseThrow(() -> new BusinessException("WAREHOUSE_NOT_FOUND", "Warehouse not found with code: " + code));
        return mapper.toWarehouseDto(wh);
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseDto getDefaultWarehouse() {
        Long companyId = CurrentUser.getCompanyId();
        Warehouse wh = warehouseRepository.findFirstByCompanyIdAndIsDefaultTrue(companyId)
                .orElseGet(() -> {
                    List<Warehouse> all = warehouseRepository.findByCompanyId(companyId);
                    if (all.isEmpty()) {
                        throw new BusinessException("NO_WAREHOUSE", "No warehouses configured for this company");
                    }
                    return all.get(0);
                });
        return mapper.toWarehouseDto(wh);
    }

    @Override
    public WarehouseDto createWarehouse(WarehouseRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        String code = request.code().trim().toUpperCase();

        if (warehouseRepository.existsByCompanyIdAndCode(companyId, code)) {
            throw new BusinessException("DUPLICATE_WAREHOUSE_CODE", "Warehouse already exists with code: " + code);
        }

        Warehouse wh = mapper.toWarehouseEntity(request);
        wh.setCompanyId(companyId);
        wh.setCode(code);
        wh.setName(request.name().trim());

        if (Boolean.TRUE.equals(request.isDefault())) {
            unsetOtherDefaults(companyId, null);
            wh.setIsDefault(true);
        } else {
            wh.setIsDefault(false);
        }

        wh.setIsActive(request.isActive() != null ? request.isActive() : true);
        wh = warehouseRepository.save(wh);
        return mapper.toWarehouseDto(wh);
    }

    @Override
    public WarehouseDto updateWarehouse(Long id, WarehouseRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        Warehouse wh = warehouseRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("WAREHOUSE_NOT_FOUND", "Warehouse not found with id: " + id));

        String code = request.code().trim().toUpperCase();
        if (!wh.getCode().equalsIgnoreCase(code) && warehouseRepository.existsByCompanyIdAndCode(companyId, code)) {
            throw new BusinessException("DUPLICATE_WAREHOUSE_CODE", "Warehouse already exists with code: " + code);
        }

        wh.setCode(code);
        wh.setName(request.name().trim());
        wh.setAddress(request.address());
        wh.setCostCenterId(request.costCenterId());

        if (Boolean.TRUE.equals(request.isDefault())) {
            unsetOtherDefaults(companyId, id);
            wh.setIsDefault(true);
        } else if (Boolean.FALSE.equals(request.isDefault())) {
            wh.setIsDefault(false);
        }

        if (request.isActive() != null) {
            wh.setIsActive(request.isActive());
        }

        wh = warehouseRepository.save(wh);
        return mapper.toWarehouseDto(wh);
    }

    @Override
    public void deleteWarehouse(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Warehouse wh = warehouseRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("WAREHOUSE_NOT_FOUND", "Warehouse not found with id: " + id));
        warehouseRepository.delete(wh);
    }

    private void unsetOtherDefaults(Long companyId, Long currentId) {
        List<Warehouse> list = warehouseRepository.findByCompanyId(companyId);
        for (Warehouse w : list) {
            if (currentId == null || !w.getId().equals(currentId)) {
                if (Boolean.TRUE.equals(w.getIsDefault())) {
                    w.setIsDefault(false);
                    warehouseRepository.save(w);
                }
            }
        }
    }
}

