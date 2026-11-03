package com.backend.finledger.ledger.repository;

import com.backend.finledger.ledger.entity.FiscalPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FiscalPeriodRepository extends JpaRepository<FiscalPeriod, Long> {

    Optional<FiscalPeriod> findByFiscalYearIdAndPeriodNo(Long fiscalYearId, Byte periodNo);

    List<FiscalPeriod> findByFiscalYearIdOrderByPeriodNoAsc(Long fiscalYearId);

    @Query("SELECT fp FROM FiscalPeriod fp JOIN fp.fiscalYear fy WHERE fy.companyId = :companyId AND :date BETWEEN fp.startDate AND fp.endDate")
    Optional<FiscalPeriod> findPeriodByDate(@Param("companyId") Long companyId, @Param("date") LocalDate date);

    @Query("SELECT fp FROM FiscalPeriod fp JOIN fp.fiscalYear fy WHERE fy.companyId = :companyId AND fp.id = :id")
    Optional<FiscalPeriod> findByCompanyIdAndId(@Param("companyId") Long companyId, @Param("id") Long id);
}

