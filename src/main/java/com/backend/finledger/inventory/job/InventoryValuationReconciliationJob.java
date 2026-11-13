package com.backend.finledger.inventory.job;

import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.inventory.entity.Item;
import com.backend.finledger.inventory.entity.StockBalance;
import com.backend.finledger.inventory.repository.ItemRepository;
import com.backend.finledger.inventory.repository.StockBalanceRepository;
import com.backend.finledger.ledger.entity.Account;
import com.backend.finledger.ledger.entity.AccountType;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.ledger.repository.JournalLineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Nightly reconciliation job comparing perpetual inventory stock valuation against
 * Inventory GL Asset balances per active company (Spec Section 5.4).
 */
@Component
public class InventoryValuationReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(InventoryValuationReconciliationJob.class);

    private final CompanyRepository companyRepository;
    private final ItemRepository itemRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final AccountRepository accountRepository;
    private final JournalLineRepository journalLineRepository;

    public InventoryValuationReconciliationJob(
            CompanyRepository companyRepository,
            ItemRepository itemRepository,
            StockBalanceRepository stockBalanceRepository,
            AccountRepository accountRepository,
            JournalLineRepository journalLineRepository) {
        this.companyRepository = companyRepository;
        this.itemRepository = itemRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.accountRepository = accountRepository;
        this.journalLineRepository = journalLineRepository;
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional(readOnly = true)
    public void verifyInventoryVsGlValuation() {
        List<Company> companies = companyRepository.findAll();
        LocalDate today = LocalDate.now();

        for (Company company : companies) {
            if (!Boolean.TRUE.equals(company.getIsActive())) {
                continue;
            }
            Long companyId = company.getId();
            List<Item> items = itemRepository.findByCompanyIdAndIsActiveTrue(companyId);
            List<Long> itemIds = items.stream().map(Item::getId).toList();
            BigDecimal subledgerValue = BigDecimal.ZERO;
            if (!itemIds.isEmpty()) {
                for (StockBalance sb : stockBalanceRepository.findByItemIdIn(itemIds)) {
                    BigDecimal val = sb.getStockValue() != null ? sb.getStockValue()
                            : (sb.getQtyOnHand() != null && sb.getAvgCost() != null
                            ? sb.getQtyOnHand().multiply(sb.getAvgCost()) : BigDecimal.ZERO);
                    subledgerValue = subledgerValue.add(val);
                }
            }
            subledgerValue = subledgerValue.setScale(2, RoundingMode.HALF_UP);

            Set<Long> inventoryAccountIds = accountRepository.findByCompanyIdOrderByCodeAsc(companyId).stream()
                    .filter(a -> a.getAccountType() == AccountType.ASSET && !Boolean.TRUE.equals(a.getIsGroup()))
                    .filter(a -> "INVENTORY".equalsIgnoreCase(a.getSubType()))
                    .map(Account::getId)
                    .collect(Collectors.toSet());

            BigDecimal glValue = BigDecimal.ZERO;
            if (!inventoryAccountIds.isEmpty()) {
                Map<Long, JournalLineRepository.AccountBalanceSummary> summaryMap =
                        journalLineRepository.getAccountBalanceSummaries(companyId, today).stream()
                                .collect(Collectors.toMap(JournalLineRepository.AccountBalanceSummary::getAccountId, s -> s));

                for (Account acc : accountRepository.findByCompanyIdOrderByCodeAsc(companyId)) {
                    if (inventoryAccountIds.contains(acc.getId())) {
                        BigDecimal opening = acc.getOpeningBalance() != null ? acc.getOpeningBalance() : BigDecimal.ZERO;
                        JournalLineRepository.AccountBalanceSummary s = summaryMap.get(acc.getId());
                        BigDecimal debits = s != null && s.getTotalDebit() != null ? s.getTotalDebit() : BigDecimal.ZERO;
                        BigDecimal credits = s != null && s.getTotalCredit() != null ? s.getTotalCredit() : BigDecimal.ZERO;
                        glValue = glValue.add(opening).add(debits).subtract(credits);
                    }
                }
            }
            glValue = glValue.setScale(2, RoundingMode.HALF_UP);

            BigDecimal diff = subledgerValue.subtract(glValue).abs();
            if (diff.compareTo(new BigDecimal("0.01")) > 0) {
                log.warn("Inventory vs GL valuation variance detected for company {} ({}): subledger={}, gl={}, diff={}",
                        companyId, company.getCode(), subledgerValue, glValue, diff);
            } else {
                log.debug("Inventory vs GL valuation reconciled cleanly for company {} ({}): {}",
                        companyId, company.getCode(), subledgerValue);
            }
        }
    }
}

