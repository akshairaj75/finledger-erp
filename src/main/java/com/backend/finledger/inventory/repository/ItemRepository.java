package com.backend.finledger.inventory.repository;

import com.backend.finledger.inventory.entity.Item;
import com.backend.finledger.inventory.entity.ItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    Optional<Item> findByCompanyIdAndId(Long companyId, Long id);
    Optional<Item> findByCompanyIdAndSku(Long companyId, String sku);
    boolean existsByCompanyIdAndSku(Long companyId, String sku);

    @Query("SELECT i FROM Item i WHERE i.companyId = :companyId " +
           "AND (:itemType IS NULL OR i.itemType = :itemType) " +
           "AND (:categoryId IS NULL OR i.categoryId = :categoryId) " +
           "AND (:active IS NULL OR i.isActive = :active) " +
           "AND (:search IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(i.sku) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Item> searchItems(
            @Param("companyId") Long companyId,
            @Param("itemType") ItemType itemType,
            @Param("categoryId") Long categoryId,
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable);

    List<Item> findByCompanyIdAndIsActiveTrue(Long companyId);
}

