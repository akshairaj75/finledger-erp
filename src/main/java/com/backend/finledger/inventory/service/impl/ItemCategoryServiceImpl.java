package com.backend.finledger.inventory.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.inventory.dto.ItemCategoryDto;
import com.backend.finledger.inventory.dto.ItemCategoryRequest;
import com.backend.finledger.inventory.entity.ItemCategory;
import com.backend.finledger.inventory.mapper.InventoryMapper;
import com.backend.finledger.inventory.repository.ItemCategoryRepository;
import com.backend.finledger.inventory.service.ItemCategoryService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class ItemCategoryServiceImpl implements ItemCategoryService {

    private final ItemCategoryRepository categoryRepository;
    private final InventoryMapper mapper;

    public ItemCategoryServiceImpl(ItemCategoryRepository categoryRepository, InventoryMapper mapper) {
        this.categoryRepository = categoryRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemCategoryDto> getAllCategories() {
        Long companyId = CurrentUser.getCompanyId();
        List<ItemCategory> categories = categoryRepository.findByCompanyId(companyId);
        Map<Long, ItemCategory> byId = categories.stream()
                .collect(Collectors.toMap(ItemCategory::getId, Function.identity()));

        return categories.stream().map(c -> {
            String parentName = null;
            if (c.getParentId() != null && byId.containsKey(c.getParentId())) {
                parentName = byId.get(c.getParentId()).getName();
            }
            return new ItemCategoryDto(
                    c.getId(),
                    c.getCompanyId(),
                    c.getName(),
                    c.getParentId(),
                    parentName,
                    c.getIncomeAccountId(),
                    c.getInventoryAccountId(),
                    c.getCogsAccountId()
            );
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ItemCategoryDto getCategoryById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        ItemCategory category = categoryRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "Category not found with id: " + id));

        String parentName = null;
        if (category.getParentId() != null) {
            parentName = categoryRepository.findByCompanyIdAndId(companyId, category.getParentId())
                    .map(ItemCategory::getName)
                    .orElse(null);
        }

        return new ItemCategoryDto(
                category.getId(),
                category.getCompanyId(),
                category.getName(),
                category.getParentId(),
                parentName,
                category.getIncomeAccountId(),
                category.getInventoryAccountId(),
                category.getCogsAccountId()
        );
    }

    @Override
    public ItemCategoryDto createCategory(ItemCategoryRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        if (categoryRepository.existsByCompanyIdAndParentIdAndName(companyId, request.parentId(), request.name().trim())) {
            throw new BusinessException("DUPLICATE_CATEGORY", "Category already exists with name: " + request.name());
        }

        ItemCategory category = mapper.toCategoryEntity(request);
        category.setCompanyId(companyId);
        category.setName(request.name().trim());
        category = categoryRepository.save(category);

        return getCategoryById(category.getId());
    }

    @Override
    public ItemCategoryDto updateCategory(Long id, ItemCategoryRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        ItemCategory category = categoryRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "Category not found with id: " + id));

        if (!category.getName().equalsIgnoreCase(request.name().trim()) &&
            categoryRepository.existsByCompanyIdAndParentIdAndName(companyId, request.parentId(), request.name().trim())) {
            throw new BusinessException("DUPLICATE_CATEGORY", "Category already exists with name: " + request.name());
        }

        category.setName(request.name().trim());
        category.setParentId(request.parentId());
        category.setIncomeAccountId(request.incomeAccountId());
        category.setInventoryAccountId(request.inventoryAccountId());
        category.setCogsAccountId(request.cogsAccountId());

        category = categoryRepository.save(category);
        return getCategoryById(category.getId());
    }

    @Override
    public void deleteCategory(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        ItemCategory category = categoryRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "Category not found with id: " + id));
        categoryRepository.delete(category);
    }
}

