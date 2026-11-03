package com.backend.finledger.purchases.repository;

import com.backend.finledger.purchases.entity.BillLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillLineRepository extends JpaRepository<BillLine, Long> {

    List<BillLine> findByBillId(Long billId);
}

