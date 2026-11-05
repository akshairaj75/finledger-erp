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
        List<RecentTransactionDto> recentTransactions
) {}

