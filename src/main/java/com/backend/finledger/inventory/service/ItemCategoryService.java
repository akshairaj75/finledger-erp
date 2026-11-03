package com.backend.finledger.inventory.service;

import com.backend.finledger.inventory.dto.ItemCategoryDto;
import com.backend.finledger.inventory.dto.ItemCategoryRequest;

import java.util.List;

public interface ItemCategoryService {
    List<ItemCategoryDto> getAllCategories();
    ItemCategoryDto getCategoryById(Long id);
    ItemCategoryDto createCategory(ItemCategoryRequest request);
    ItemCategoryDto updateCategory(Long id, ItemCategoryRequest request);
    void deleteCategory(Long id);
}

