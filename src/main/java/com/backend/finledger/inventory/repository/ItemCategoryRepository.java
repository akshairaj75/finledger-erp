package com.backend.finledger.inventory.repository;

import com.backend.finledger.inventory.entity.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemCategoryRepository extends JpaRepository<ItemCategory, Long> {
    Optional<ItemCategory> findByCompanyIdAndId(Long companyId, Long id);
    List<ItemCategory> findByCompanyId(Long companyId);
    boolean existsByCompanyIdAndParentIdAndName(Long companyId, Long parentId, String name);
}

