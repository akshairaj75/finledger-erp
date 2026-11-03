package com.backend.finledger.inventory.service;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.inventory.dto.ItemDto;
import com.backend.finledger.inventory.dto.ItemRequest;
import com.backend.finledger.inventory.entity.ItemType;
import org.springframework.data.domain.Pageable;

public interface ItemService {
    PageResponse<ItemDto> searchItems(ItemType itemType, Long categoryId, Boolean active, String search, Pageable pageable);
    ItemDto getItemById(Long id);
    ItemDto getItemBySku(String sku);
    ItemDto createItem(ItemRequest request);
    ItemDto updateItem(Long id, ItemRequest request);
    void deleteItem(Long id);
}

