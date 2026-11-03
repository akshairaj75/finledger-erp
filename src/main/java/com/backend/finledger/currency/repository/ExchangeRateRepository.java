package com.backend.finledger.currency.repository;

import com.backend.finledger.currency.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    List<ExchangeRate> findByCompanyIdOrderByRateDateDesc(Long companyId);

    Optional<ExchangeRate> findByCompanyIdAndFromCurrencyAndToCurrencyAndRateDate(
            Long companyId, String fromCurrency, String toCurrency, LocalDate rateDate);
}

