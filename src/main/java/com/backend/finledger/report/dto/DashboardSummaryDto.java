package com.backend.finledger.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryDto(
        // Financial KPIs
        BigDecimal totalRevenueYtd,
        BigDecimal totalExpensesYtd,
        BigDecimal accountsReceivable,
        BigDecimal cashAndBank,
        BigDecimal accountsPayable,

        // Operational KPIs
        BigDecimal openPipelineValue,
        long openPipelineCount,
        BigDecimal totalInventoryValue,
        long lowStockItemCount,
        long expensesAwaitingApprovalCount,
        BigDecimal expensesAwaitingApprovalAmount,

        // Visualizations & summaries
        List<MonthlyFinancialDto> monthlyRevenueExpenses,
        AgeingSummaryDto receivablesAgeing,
        List<RecentTransactionDto> recentTransactions,
        long overdueActivitiesCount
) {
    public DashboardSummaryDto(
            BigDecimal totalRevenueYtd,
            BigDecimal totalExpensesYtd,
            BigDecimal accountsReceivable,
            BigDecimal cashAndBank,
            BigDecimal accountsPayable,
            BigDecimal openPipelineValue,
            long openPipelineCount,
            BigDecimal totalInventoryValue,
            long lowStockItemCount,
            long expensesAwaitingApprovalCount,
            BigDecimal expensesAwaitingApprovalAmount,
            List<MonthlyFinancialDto> monthlyRevenueExpenses,
            AgeingSummaryDto receivablesAgeing,
            List<RecentTransactionDto> recentTransactions) {
        this(totalRevenueYtd, totalExpensesYtd, accountsReceivable, cashAndBank, accountsPayable,
                openPipelineValue, openPipelineCount, totalInventoryValue, lowStockItemCount,
                expensesAwaitingApprovalCount, expensesAwaitingApprovalAmount,
                monthlyRevenueExpenses, receivablesAgeing, recentTransactions, 0L);
    }
}

