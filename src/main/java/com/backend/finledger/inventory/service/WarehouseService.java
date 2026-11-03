package com.backend.finledger.inventory.service;

import com.backend.finledger.inventory.dto.WarehouseDto;
import com.backend.finledger.inventory.dto.WarehouseRequest;

import java.util.List;

public interface WarehouseService {
    List<WarehouseDto> getAllWarehouses();
    WarehouseDto getWarehouseById(Long id);
    WarehouseDto getWarehouseByCode(String code);
    WarehouseDto getDefaultWarehouse();
    WarehouseDto createWarehouse(WarehouseRequest request);
    WarehouseDto updateWarehouse(Long id, WarehouseRequest request);
    void deleteWarehouse(Long id);
}

