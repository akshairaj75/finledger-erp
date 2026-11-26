package com.backend.finledger.report.pdf;

import com.backend.finledger.attachment.service.FileStorageService;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.pdf.FinLedgerPdfTheme;
import com.backend.finledger.common.pdf.PdfBrandingHeaderBuilder;
import com.backend.finledger.common.pdf.PdfCellRenderers;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.ledger.dto.GeneralLedgerLineDto;
import com.backend.finledger.ledger.dto.GeneralLedgerReportDto;
import com.backend.finledger.ledger.dto.TrialBalanceLineDto;
import com.backend.finledger.ledger.dto.TrialBalanceReportDto;
import com.backend.finledger.purchases.dto.ApAgeingReportDto;
import com.backend.finledger.purchases.dto.ApAgeingVendorDto;
import com.backend.finledger.report.dto.BalanceSheetReportDto;
import com.backend.finledger.report.dto.ExpenseAnalysisGroupDto;
import com.backend.finledger.report.dto.ExpenseAnalysisReportDto;
import com.backend.finledger.report.dto.PipelineForecastReportDto;
import com.backend.finledger.report.dto.PipelineStageSummaryDto;
import com.backend.finledger.report.dto.ProfitLossReportDto;
import com.backend.finledger.report.dto.ReportLineDto;
import com.backend.finledger.report.dto.StockMovementLineDto;
import com.backend.finledger.report.dto.StockMovementReportDto;
import com.backend.finledger.report.dto.StockValuationLineDto;
import com.backend.finledger.report.dto.StockValuationReportDto;
import com.backend.finledger.report.dto.VatReturnReportDto;
import com.backend.finledger.sales.dto.ArAgeingCustomerDto;
import com.backend.finledger.sales.dto.ArAgeingReportDto;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

/**
 * Renders structured, webpage-faithful OpenPDF documents for all 11 FinLedger financial and operational reports.
 */
public final class ReportPdfRenderer {

    private ReportPdfRenderer() {}

    public static byte[] renderProfitLoss(ProfitLossReportDto pl, Company company, FileStorageService storage) {
        String ccy = currency(company);
        boolean hasCompare = pl.comparePeriod() != null && !pl.comparePeriod().isBlank();
        String subtitle = "For the period " + FinLedgerPdfTheme.fmtDate(pl.fromDate()) + " to " + FinLedgerPdfTheme.fmtDate(pl.toDate()) + " (" + ccy + ")";
        return renderWithDocument(false, company, storage, "Statement of Profit or Loss", subtitle, doc -> {
            PdfPTable table = new PdfPTable(hasCompare ? 4 : 2);
            table.setWidthPercentage(100);
            table.setWidths(hasCompare ? new float[]{48f, 20f, 20f, 12f} : new float[]{72f, 28f});
            table.setHeaderRows(1);

            addTableHeaderCell(table, "Account", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Current Period", Element.ALIGN_RIGHT);
            if (hasCompare) {
                addTableHeaderCell(table, "Comparative Period", Element.ALIGN_RIGHT);
                addTableHeaderCell(table, "% Change", Element.ALIGN_RIGHT);
            }

            // Operating Revenue
            addSectionRow(table, "OPERATING REVENUE", hasCompare ? 4 : 2);
            for (ReportLineDto l : safeList(pl.revenueLines())) {
                addReportAccountRow(table, l, hasCompare);
            }
            addSubtotalRow(table, "Total Revenue", pl.totalRevenue(), pl.totalRevenueCompare(), hasCompare);

            // Cost of Goods Sold
            addSectionRow(table, "COST OF GOODS SOLD", hasCompare ? 4 : 2);
            for (ReportLineDto l : safeList(pl.cogsLines())) {
                addReportAccountRow(table, l, hasCompare);
            }
            addSubtotalRow(table, "Total Cost of Goods Sold", pl.totalCogs(), pl.totalCogsCompare(), hasCompare);

            // Gross Profit (Major Row)
            addMajorRow(table, "Gross Profit", pl.grossProfit(), pl.grossProfitCompare(), hasCompare);

            // Operating Expenses
            addSectionRow(table, "OPERATING EXPENSES", hasCompare ? 4 : 2);
            for (ReportLineDto l : safeList(pl.expenseLines())) {
                addReportAccountRow(table, l, hasCompare);
            }
            addSubtotalRow(table, "Total Operating Expenses", pl.totalExpenses(), pl.totalExpensesCompare(), hasCompare);

            // Net Profit / Loss (Grand Total Row)
            addGrandTotalComparisonRow(table, "NET PROFIT / (LOSS)", pl.netProfit(), pl.netProfitCompare(), hasCompare);

            doc.add(table);
        });
    }

    public static byte[] renderBalanceSheet(BalanceSheetReportDto bs, Company company, FileStorageService storage) {
        String ccy = currency(company);
        String subtitle = "As of " + FinLedgerPdfTheme.fmtDate(bs.asOfDate()) + " (" + ccy + ")";
        return renderWithDocument(false, company, storage, "Statement of Financial Position", subtitle, doc -> {
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{72f, 28f});
            table.setHeaderRows(1);

            addTableHeaderCell(table, "Classification & Account", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Balance (" + ccy + ")", Element.ALIGN_RIGHT);

            // Assets
            addSectionRow(table, "CURRENT ASSETS", 2);
            for (ReportLineDto l : safeList(bs.currentAssets())) {
                addReportAccountRow(table, l, false);
            }
            addSubtotalRow(table, "Total Current Assets", bs.totalCurrentAssets(), null, false);

            addSectionRow(table, "NON-CURRENT ASSETS", 2);
            for (ReportLineDto l : safeList(bs.nonCurrentAssets())) {
                addReportAccountRow(table, l, false);
            }
            addSubtotalRow(table, "Total Non-Current Assets", bs.totalNonCurrentAssets(), null, false);

            addMajorRow(table, "TOTAL ASSETS", bs.totalAssets(), null, false);

            // Liabilities
            addSectionRow(table, "CURRENT LIABILITIES", 2);
            for (ReportLineDto l : safeList(bs.currentLiabilities())) {
                addReportAccountRow(table, l, false);
            }
            addSubtotalRow(table, "Total Current Liabilities", bs.totalCurrentLiabilities(), null, false);

            addSectionRow(table, "NON-CURRENT LIABILITIES", 2);
            for (ReportLineDto l : safeList(bs.nonCurrentLiabilities())) {
                addReportAccountRow(table, l, false);
            }
            addSubtotalRow(table, "Total Non-Current Liabilities", bs.totalNonCurrentLiabilities(), null, false);

            // Equity
            addSectionRow(table, "EQUITY & RESERVES", 2);
            for (ReportLineDto l : safeList(bs.equityLines())) {
                addReportAccountRow(table, l, false);
            }
            if (bs.retainedEarnings() != null && bs.retainedEarnings().compareTo(BigDecimal.ZERO) != 0) {
                addIndentedLabelRow(table, "Retained Earnings", FinLedgerPdfTheme.fmtMoney(bs.retainedEarnings()));
            }
            addIndentedLabelRow(table, "Net Income Year-To-Date", FinLedgerPdfTheme.fmtMoney(bs.netIncomeYearToDate()));
            addSubtotalRow(table, "Total Equity", bs.totalEquity(), null, false);

            addGrandTotalPairRow(table, "TOTAL LIABILITIES AND EQUITY", FinLedgerPdfTheme.fmtMoney(bs.totalLiabilitiesAndEquity()));

            doc.add(table);

            addStatusBanner(doc, bs.balanced(),
                    "Statement of Financial Position is balanced (Assets = Liabilities + Equity)",
                    "Imbalance detected in Balance Sheet");
        });
    }

    public static byte[] renderTrialBalance(TrialBalanceReportDto tb, Company company, FileStorageService storage) {
        String ccy = currency(company);
        String subtitle = "As of " + FinLedgerPdfTheme.fmtDate(tb.asOfDate()) + " (" + ccy + ")";
        return renderWithDocument(false, company, storage, "Trial Balance", subtitle, doc -> {
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{14f, 40f, 14f, 16f, 16f});
            table.setHeaderRows(1);

            addTableHeaderCell(table, "Account Code", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Account Name", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Type", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Debit (" + ccy + ")", Element.ALIGN_RIGHT);
            addTableHeaderCell(table, "Credit (" + ccy + ")", Element.ALIGN_RIGHT);

            List<TrialBalanceLineDto> lines = safeList(tb.lines());
            if (lines.isEmpty()) {
                addEmptyRow(table, 5, "No posted journal entries found for this period.");
            } else {
                for (TrialBalanceLineDto line : lines) {
                    table.addCell(bodyCell(line.accountCode(), FinLedgerPdfTheme.FONT_CODE_BLUE, Element.ALIGN_LEFT));
                    table.addCell(bodyCell(line.accountName(), FinLedgerPdfTheme.FONT_BODY_BOLD, Element.ALIGN_LEFT));
                    table.addCell(bodyCell(line.accountType() != null ? line.accountType().name() : "-", FinLedgerPdfTheme.FONT_META_BOLD, Element.ALIGN_LEFT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoneyOrDash(line.debitBalance()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoneyOrDash(line.creditBalance()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                }
            }

            table.addCell(grandTotalCell("TOTAL", 3, Element.ALIGN_LEFT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(tb.totalDebit()), 1, Element.ALIGN_RIGHT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(tb.totalCredit()), 1, Element.ALIGN_RIGHT));

            doc.add(table);

            boolean isBalanced = Boolean.TRUE.equals(tb.isBalanced());
            BigDecimal diff = (tb.totalDebit() != null ? tb.totalDebit() : BigDecimal.ZERO)
                    .subtract(tb.totalCredit() != null ? tb.totalCredit() : BigDecimal.ZERO);
            addStatusBanner(doc, isBalanced,
                    "Balanced (Total Debits = Total Credits)",
                    "Out of Balance (Difference: " + FinLedgerPdfTheme.fmtMoney(diff) + " " + ccy + ")");
        });
    }

    public static byte[] renderGeneralLedger(GeneralLedgerReportDto gl, Company company, FileStorageService storage) {
        String ccy = currency(company);
        String subtitle = gl.accountCode() + " " + gl.accountName()
                + "  •  Period: " + FinLedgerPdfTheme.fmtDate(gl.fromDate()) + " to " + FinLedgerPdfTheme.fmtDate(gl.toDate())
                + "  •  Normal Balance: " + gl.normalBalance();
        return renderWithDocument(true, company, storage, "General Ledger Report", subtitle, doc -> {
            BigDecimal netMovement = gl.netMovement();
            addSummaryCards(doc, new String[][]{
                    {"OPENING BALANCE", FinLedgerPdfTheme.fmtMoney(gl.openingBalance()) + " " + ccy},
                    {"NET MOVEMENT", FinLedgerPdfTheme.fmtMoney(netMovement) + " " + ccy},
                    {"CLOSING BALANCE", FinLedgerPdfTheme.fmtMoney(gl.closingBalance()) + " " + ccy}
            });

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{11f, 13f, 13f, 27f, 12f, 12f, 12f});
            table.setHeaderRows(1);

            addTableHeaderCell(table, "Date", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Entry #", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Reference", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Description", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Debit (" + ccy + ")", Element.ALIGN_RIGHT);
            addTableHeaderCell(table, "Credit (" + ccy + ")", Element.ALIGN_RIGHT);
            addTableHeaderCell(table, "Running Balance", Element.ALIGN_RIGHT);

            // Opening balance row
            PdfPCell obLabel = subtotalCell("Opening Balance as of " + FinLedgerPdfTheme.fmtDate(gl.fromDate()), Element.ALIGN_LEFT);
            obLabel.setColspan(4);
            table.addCell(obLabel);
            table.addCell(subtotalCell("-", Element.ALIGN_RIGHT));
            table.addCell(subtotalCell("-", Element.ALIGN_RIGHT));
            table.addCell(subtotalCell(FinLedgerPdfTheme.fmtMoney(gl.openingBalance()), Element.ALIGN_RIGHT));

            List<GeneralLedgerLineDto> lines = safeList(gl.lines());
            if (lines.isEmpty()) {
                addEmptyRow(table, 7, "No transactions recorded in this period for this account.");
            } else {
                for (GeneralLedgerLineDto line : lines) {
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtDate(line.date()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_LEFT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.safe(line.entryNo()), FinLedgerPdfTheme.FONT_CODE_BLUE, Element.ALIGN_LEFT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.safe(line.reference()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_LEFT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.safe(line.description()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_LEFT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoneyOrDash(line.debit()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoneyOrDash(line.credit()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoney(line.runningBalance()), FinLedgerPdfTheme.FONT_BODY_BOLD, Element.ALIGN_RIGHT));
                }
            }

            table.addCell(grandTotalCell("TOTAL ACTIVITY & CLOSING BALANCE", 4, Element.ALIGN_LEFT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(gl.totalDebit()), 1, Element.ALIGN_RIGHT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(gl.totalCredit()), 1, Element.ALIGN_RIGHT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(gl.closingBalance()), 1, Element.ALIGN_RIGHT));

            doc.add(table);
        });
    }

    public static byte[] renderArAgeing(ArAgeingReportDto ar, Company company, FileStorageService storage) {
        String ccy = currency(company);
        String subtitle = "Outstanding customer receivables as of " + FinLedgerPdfTheme.fmtDate(ar.asOfDate()) + " (" + ccy + ")";
        return renderWithDocument(true, company, storage, "Accounts Receivable Ageing", subtitle, doc -> {
            BigDecimal overdue1To60 = zero(ar.total1To30()).add(zero(ar.total31To60()));
            BigDecimal overdueOver60 = zero(ar.total61To90()).add(zero(ar.totalOver90()));
            addSummaryCards(doc, new String[][]{
                    {"CURRENT (<= 0 DAYS)", FinLedgerPdfTheme.fmtMoney(ar.totalCurrent()) + " " + ccy},
                    {"OVERDUE 1-60 DAYS", FinLedgerPdfTheme.fmtMoney(overdue1To60) + " " + ccy},
                    {"OVERDUE > 60 DAYS", FinLedgerPdfTheme.fmtMoney(overdueOver60) + " " + ccy},
                    {"GRAND TOTAL AR", FinLedgerPdfTheme.fmtMoney(ar.grandTotal()) + " " + ccy}
            });

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{11f, 25f, 10f, 10f, 10f, 10f, 10f, 14f});
            table.setHeaderRows(1);

            addTableHeaderCell(table, "Code", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Customer Name", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Current", Element.ALIGN_RIGHT);
            addTableHeaderCell(table, "1-30 Days", Element.ALIGN_RIGHT);
            addTableHeaderCell(table, "31-60 Days", Element.ALIGN_RIGHT);
            addTableHeaderCell(table, "61-90 Days", Element.ALIGN_RIGHT);
            addTableHeaderCell(table, ">90 Days", Element.ALIGN_RIGHT);
            addTableHeaderCell(table, "Total Due", Element.ALIGN_RIGHT);

            List<ArAgeingCustomerDto> customers = safeList(ar.customers());
            if (customers.isEmpty()) {
                addEmptyRow(table, 8, "No outstanding customer invoices found as of " + FinLedgerPdfTheme.fmtDate(ar.asOfDate()) + ".");
            } else {
                for (ArAgeingCustomerDto c : customers) {
                    table.addCell(bodyCell(FinLedgerPdfTheme.safe(c.customerCode()), FinLedgerPdfTheme.FONT_CODE_BLUE, Element.ALIGN_LEFT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.safe(c.customerName()), FinLedgerPdfTheme.FONT_BODY_BOLD, Element.ALIGN_LEFT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoneyOrDash(c.current()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoneyOrDash(c.days1To30()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoneyOrDash(c.days31To60()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoneyOrDash(c.days61To90()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoneyOrDash(c.daysOver90()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoney(c.totalOutstanding()), FinLedgerPdfTheme.FONT_BODY_BOLD, Element.ALIGN_RIGHT));
                }
            }

            table.addCell(grandTotalCell("TOTAL RECEIVABLES", 2, Element.ALIGN_LEFT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(ar.totalCurrent()), 1, Element.ALIGN_RIGHT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(ar.total1To30()), 1, Element.ALIGN_RIGHT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(ar.total31To60()), 1, Element.ALIGN_RIGHT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(ar.total61To90()), 1, Element.ALIGN_RIGHT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(ar.totalOver90()), 1, Element.ALIGN_RIGHT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(ar.grandTotal()), 1, Element.ALIGN_RIGHT));

            doc.add(table);
        });
    }

    public static byte[] renderApAgeing(ApAgeingReportDto ap, Company company, FileStorageService storage) {
        String ccy = currency(company);
        String subtitle = "Outstanding vendor payables as of " + FinLedgerPdfTheme.fmtDate(ap.asOfDate()) + " (" + ccy + ")";
        return renderWithDocument(true, company, storage, "Accounts Payable Ageing", subtitle, doc -> {
            BigDecimal overdue1To60 = zero(ap.total1To30()).add(zero(ap.total31To60()));
            BigDecimal overdueOver60 = zero(ap.total61To90()).add(zero(ap.totalOver90()));
            addSummaryCards(doc, new String[][]{
                    {"CURRENT (<= 0 DAYS)", FinLedgerPdfTheme.fmtMoney(ap.totalCurrent()) + " " + ccy},
                    {"OVERDUE 1-60 DAYS", FinLedgerPdfTheme.fmtMoney(overdue1To60) + " " + ccy},
                    {"OVERDUE > 60 DAYS", FinLedgerPdfTheme.fmtMoney(overdueOver60) + " " + ccy},
                    {"GRAND TOTAL AP", FinLedgerPdfTheme.fmtMoney(ap.grandTotal()) + " " + ccy}
            });

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{11f, 25f, 10f, 10f, 10f, 10f, 10f, 14f});
            table.setHeaderRows(1);

            addTableHeaderCell(table, "Code", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Vendor / Supplier", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Current", Element.ALIGN_RIGHT);
            addTableHeaderCell(table, "1-30 Days", Element.ALIGN_RIGHT);
            addTableHeaderCell(table, "31-60 Days", Element.ALIGN_RIGHT);
            addTableHeaderCell(table, "61-90 Days", Element.ALIGN_RIGHT);
            addTableHeaderCell(table, ">90 Days", Element.ALIGN_RIGHT);
            addTableHeaderCell(table, "Total Due", Element.ALIGN_RIGHT);

            List<ApAgeingVendorDto> vendors = safeList(ap.vendors());
            if (vendors.isEmpty()) {
                addEmptyRow(table, 8, "No outstanding vendor bills found as of " + FinLedgerPdfTheme.fmtDate(ap.asOfDate()) + ".");
            } else {
                for (ApAgeingVendorDto v : vendors) {
                    table.addCell(bodyCell(FinLedgerPdfTheme.safe(v.vendorCode()), FinLedgerPdfTheme.FONT_CODE_BLUE, Element.ALIGN_LEFT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.safe(v.vendorName()), FinLedgerPdfTheme.FONT_BODY_BOLD, Element.ALIGN_LEFT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoneyOrDash(v.current()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoneyOrDash(v.days1To30()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoneyOrDash(v.days31To60()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoneyOrDash(v.days61To90()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoneyOrDash(v.daysOver90()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoney(v.totalOutstanding()), FinLedgerPdfTheme.FONT_BODY_BOLD, Element.ALIGN_RIGHT));
                }
            }

            table.addCell(grandTotalCell("TOTAL PAYABLES", 2, Element.ALIGN_LEFT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(ap.totalCurrent()), 1, Element.ALIGN_RIGHT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(ap.total1To30()), 1, Element.ALIGN_RIGHT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(ap.total31To60()), 1, Element.ALIGN_RIGHT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(ap.total61To90()), 1, Element.ALIGN_RIGHT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(ap.totalOver90()), 1, Element.ALIGN_RIGHT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(ap.grandTotal()), 1, Element.ALIGN_RIGHT));

            doc.add(table);
        });
    }

    public static byte[] renderVatReturn(VatReturnReportDto vat, Company company, FileStorageService storage) {
        String ccy = currency(company);
        String subtitle = "Period: " + FinLedgerPdfTheme.fmtDate(vat.fromDate()) + " to " + FinLedgerPdfTheme.fmtDate(vat.toDate()) + " (" + ccy + ")";
        return renderWithDocument(false, company, storage, "VAT 201 Return (FTA UAE)", subtitle, doc -> {
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{12f, 48f, 20f, 20f});
            table.setHeaderRows(1);

            addTableHeaderCell(table, "Box #", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Description", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Taxable Amount (" + ccy + ")", Element.ALIGN_RIGHT);
            addTableHeaderCell(table, "VAT Amount (" + ccy + ")", Element.ALIGN_RIGHT);

            // Outputs
            addSectionRow(table, "VAT ON SALES AND ALL OTHER OUTPUTS", 4);
            addVatBoxRow(table, "Box 1a", "Standard rated supplies in UAE (5%)",
                    FinLedgerPdfTheme.fmtMoney(vat.standardRatedSalesAmount()),
                    FinLedgerPdfTheme.fmtMoney(vat.standardRatedSalesVat()));
            addVatBoxRow(table, "Box 2", "Supplies subject to the reverse charge provisions", "0.00", "0.00");
            addVatBoxRow(table, "Box 3", "Zero-rated supplies", FinLedgerPdfTheme.fmtMoney(vat.zeroRatedSalesAmount()), "-");
            addVatBoxRow(table, "Box 4", "Exempt supplies", FinLedgerPdfTheme.fmtMoney(vat.exemptSalesAmount()), "-");

            PdfPCell outLbl = subtotalCell("Total Output VAT Due", Element.ALIGN_LEFT);
            outLbl.setColspan(3);
            table.addCell(outLbl);
            table.addCell(subtotalCell(FinLedgerPdfTheme.fmtMoney(vat.totalOutputVat()), Element.ALIGN_RIGHT));

            // Inputs
            addSectionRow(table, "VAT ON EXPENSES AND ALL OTHER INPUTS", 4);
            addVatBoxRow(table, "Box 1g", "Standard rated expenses and purchases (5%)",
                    FinLedgerPdfTheme.fmtMoney(vat.standardRatedPurchasesAmount()),
                    FinLedgerPdfTheme.fmtMoney(vat.standardRatedPurchasesVat()));

            PdfPCell inLbl = subtotalCell("Total Recoverable Input VAT", Element.ALIGN_LEFT);
            inLbl.setColspan(3);
            table.addCell(inLbl);
            table.addCell(subtotalCell(FinLedgerPdfTheme.fmtMoney(vat.totalInputVat()), Element.ALIGN_RIGHT));

            table.addCell(grandTotalCell("BOX 13: NET VAT PAYABLE / (RECOVERABLE)", 3, Element.ALIGN_LEFT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(vat.netVatDue()), 1, Element.ALIGN_RIGHT));

            doc.add(table);
        });
    }

    public static byte[] renderStockValuation(StockValuationReportDto sv, Company company, FileStorageService storage) {
        String ccy = currency(company);
        String subtitle = "Warehouse: " + FinLedgerPdfTheme.safe(sv.warehouseName())
                + "  |  As of: " + FinLedgerPdfTheme.fmtDate(sv.asOfDate()) + " (" + ccy + ")";
        return renderWithDocument(true, company, storage, "Inventory Stock Valuation", subtitle, doc -> {
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{12f, 24f, 15f, 17f, 10f, 10f, 12f});
            table.setHeaderRows(1);

            addTableHeaderCell(table, "SKU", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Item Name", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Category", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Warehouse", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Qty On Hand", Element.ALIGN_RIGHT);
            addTableHeaderCell(table, "Avg Cost", Element.ALIGN_RIGHT);
            addTableHeaderCell(table, "Total Valuation (" + ccy + ")", Element.ALIGN_RIGHT);

            List<StockValuationLineDto> lines = safeList(sv.lines());
            if (lines.isEmpty()) {
                addEmptyRow(table, 7, "No inventory balances found.");
            } else {
                for (StockValuationLineDto item : lines) {
                    table.addCell(bodyCell(FinLedgerPdfTheme.safe(item.sku()), FinLedgerPdfTheme.FONT_CODE_BLUE, Element.ALIGN_LEFT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.safe(item.itemName()), FinLedgerPdfTheme.FONT_BODY_BOLD, Element.ALIGN_LEFT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.safe(item.categoryName()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_LEFT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.safe(item.warehouseName()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_LEFT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoney(item.quantityOnHand()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoney(item.unitCost()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoney(item.totalValue()), FinLedgerPdfTheme.FONT_BODY_BOLD, Element.ALIGN_RIGHT));
                }
            }

            table.addCell(grandTotalCell("GRAND TOTAL INVENTORY VALUATION (" + sv.totalItems() + " items)", 6, Element.ALIGN_LEFT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(sv.totalValuation()), 1, Element.ALIGN_RIGHT));

            doc.add(table);
        });
    }

    public static byte[] renderStockMovement(StockMovementReportDto sm, Company company, FileStorageService storage) {
        String ccy = currency(company);
        String subtitle = sm.sku() + " " + sm.itemName()
                + "  •  Period: " + FinLedgerPdfTheme.fmtDate(sm.fromDate()) + " to " + FinLedgerPdfTheme.fmtDate(sm.toDate());
        return renderWithDocument(true, company, storage, "Stock Movement Ledger", subtitle, doc -> {
            addSummaryCards(doc, new String[][]{
                    {"OPENING QTY", FinLedgerPdfTheme.fmtMoney(sm.openingQty())},
                    {"CLOSING QTY", FinLedgerPdfTheme.fmtMoney(sm.closingQty())}
            });

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{11f, 15f, 14f, 18f, 10f, 10f, 11f, 11f});
            table.setHeaderRows(1);

            addTableHeaderCell(table, "Date", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Movement Type", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Reference", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Warehouse", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Qty In", Element.ALIGN_RIGHT);
            addTableHeaderCell(table, "Qty Out", Element.ALIGN_RIGHT);
            addTableHeaderCell(table, "Running Balance", Element.ALIGN_RIGHT);
            addTableHeaderCell(table, "Unit Cost (" + ccy + ")", Element.ALIGN_RIGHT);

            PdfPCell openLbl = subtotalCell("Opening Stock as of " + FinLedgerPdfTheme.fmtDate(sm.fromDate()), Element.ALIGN_LEFT);
            openLbl.setColspan(4);
            table.addCell(openLbl);
            table.addCell(subtotalCell("-", Element.ALIGN_RIGHT));
            table.addCell(subtotalCell("-", Element.ALIGN_RIGHT));
            table.addCell(subtotalCell(FinLedgerPdfTheme.fmtMoney(sm.openingQty()), Element.ALIGN_RIGHT));
            table.addCell(subtotalCell("-", Element.ALIGN_RIGHT));

            List<StockMovementLineDto> moves = safeList(sm.movements());
            if (moves.isEmpty()) {
                addEmptyRow(table, 8, "No stock movements recorded for this item in this period.");
            } else {
                for (StockMovementLineDto m : moves) {
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtDate(m.date()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_LEFT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.safe(m.movementType()), FinLedgerPdfTheme.FONT_META_BOLD, Element.ALIGN_LEFT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.safe(m.reference()), FinLedgerPdfTheme.FONT_CODE_BLUE, Element.ALIGN_LEFT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.safe(m.warehouseName()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_LEFT));
                    BigDecimal in = m.qtyIn() != null ? m.qtyIn().abs() : BigDecimal.ZERO;
                    BigDecimal out = m.qtyOut() != null ? m.qtyOut().abs() : BigDecimal.ZERO;
                    table.addCell(bodyCell(in.compareTo(BigDecimal.ZERO) > 0 ? "+" + FinLedgerPdfTheme.fmtMoney(in) : "-", FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                    table.addCell(bodyCell(out.compareTo(BigDecimal.ZERO) > 0 ? "-" + FinLedgerPdfTheme.fmtMoney(out) : "-", FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoney(m.runningBalance()), FinLedgerPdfTheme.FONT_BODY_BOLD, Element.ALIGN_RIGHT));
                    table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoneyOrDash(m.unitCost()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                }
            }

            table.addCell(grandTotalCell("CLOSING STOCK QUANTITY", 6, Element.ALIGN_LEFT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(sm.closingQty()), 1, Element.ALIGN_RIGHT));
            table.addCell(grandTotalCell("", 1, Element.ALIGN_RIGHT));

            doc.add(table);
        });
    }

    public static byte[] renderPipelineForecast(PipelineForecastReportDto pf, Company company, FileStorageService storage) {
        String ccy = currency(company);
        String subtitle = "Period: " + FinLedgerPdfTheme.fmtDate(pf.fromDate()) + " to " + FinLedgerPdfTheme.fmtDate(pf.toDate()) + " (" + ccy + ")";
        return renderWithDocument(false, company, storage, "CRM Sales Pipeline Forecast", subtitle, doc -> {
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{34f, 12f, 14f, 20f, 20f});
            table.setHeaderRows(1);

            addTableHeaderCell(table, "Stage", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Deals", Element.ALIGN_CENTER);
            addTableHeaderCell(table, "Probability", Element.ALIGN_CENTER);
            addTableHeaderCell(table, "Total Value (" + ccy + ")", Element.ALIGN_RIGHT);
            addTableHeaderCell(table, "Weighted Forecast (" + ccy + ")", Element.ALIGN_RIGHT);

            for (PipelineStageSummaryDto s : safeList(pf.stages())) {
                table.addCell(bodyCell(FinLedgerPdfTheme.safe(s.stageName()), FinLedgerPdfTheme.FONT_BODY_BOLD, Element.ALIGN_LEFT));
                table.addCell(bodyCell(String.valueOf(s.dealCount()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_CENTER));
                table.addCell(bodyCell(s.probability() != null ? s.probability() + "%" : "-", FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_CENTER));
                table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoney(s.totalValue()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoney(s.weightedValue()), FinLedgerPdfTheme.FONT_BODY_BOLD, Element.ALIGN_RIGHT));
            }

            table.addCell(grandTotalCell("TOTAL PIPELINE (" + pf.totalDeals() + " open opportunities)", 3, Element.ALIGN_LEFT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(pf.totalPipelineValue()), 1, Element.ALIGN_RIGHT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(pf.totalWeightedValue()), 1, Element.ALIGN_RIGHT));

            doc.add(table);
        });
    }

    public static byte[] renderExpenseAnalysis(ExpenseAnalysisReportDto ea, Company company, FileStorageService storage) {
        String ccy = currency(company);
        String groupLabel = ea.groupBy() != null ? ea.groupBy().toUpperCase() : "CATEGORY";
        String subtitle = "Grouped by: " + groupLabel + "  •  Period: " + FinLedgerPdfTheme.fmtDate(ea.fromDate()) + " to " + FinLedgerPdfTheme.fmtDate(ea.toDate()) + " (" + ccy + ")";
        return renderWithDocument(false, company, storage, "Operational Expense Analysis", subtitle, doc -> {
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{34f, 12f, 20f, 20f, 14f});
            table.setHeaderRows(1);

            addTableHeaderCell(table, "Category / Entity", Element.ALIGN_LEFT);
            addTableHeaderCell(table, "Claims", Element.ALIGN_CENTER);
            addTableHeaderCell(table, "Reimbursable (" + ccy + ")", Element.ALIGN_RIGHT);
            addTableHeaderCell(table, "Total Amount (" + ccy + ")", Element.ALIGN_RIGHT);
            addTableHeaderCell(table, "% of Total", Element.ALIGN_RIGHT);

            for (ExpenseAnalysisGroupDto g : safeList(ea.groups())) {
                table.addCell(bodyCell(FinLedgerPdfTheme.safe(g.groupName()), FinLedgerPdfTheme.FONT_BODY_BOLD, Element.ALIGN_LEFT));
                table.addCell(bodyCell(String.valueOf(g.claimCount()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_CENTER));
                table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoney(g.reimbursableAmount()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoney(g.totalAmount()), FinLedgerPdfTheme.FONT_BODY_BOLD, Element.ALIGN_RIGHT));
                table.addCell(bodyCell(FinLedgerPdfTheme.fmtPct(g.percentageOfTotal()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
            }

            int totalClaims = 0;
            BigDecimal totalReimbursable = BigDecimal.ZERO;
            for (ExpenseAnalysisGroupDto g : safeList(ea.groups())) {
                totalClaims += g.claimCount();
                if (g.reimbursableAmount() != null) {
                    totalReimbursable = totalReimbursable.add(g.reimbursableAmount());
                }
            }

            table.addCell(grandTotalCell("GRAND TOTAL OPERATING EXPENSES", 1, Element.ALIGN_LEFT));
            table.addCell(grandTotalCell(String.valueOf(totalClaims), 1, Element.ALIGN_CENTER));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(totalReimbursable), 1, Element.ALIGN_RIGHT));
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(ea.grandTotal()), 1, Element.ALIGN_RIGHT));
            table.addCell(grandTotalCell("100.0%", 1, Element.ALIGN_RIGHT));

            doc.add(table);
        });
    }

    // ─── Core Document Wrapper & Shared Row Builders ─────────────────────────

    @FunctionalInterface
    private interface DocumentBodyWriter {
        void write(Document doc) throws Exception;
    }

    private static byte[] renderWithDocument(
            boolean landscape,
            Company company,
            FileStorageService storage,
            String title,
            String subtitle,
            DocumentBodyWriter bodyWriter
    ) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Rectangle pageSize = landscape ? PageSize.A4.rotate() : PageSize.A4;
            Document document = new Document(pageSize, 36, 36, 36, 46);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            String compName = company != null ? company.getLegalName() : "FinLedger ERP";
            writer.setPageEvent(new PdfCellRenderers.FinLedgerPageFooterEvent(compName, title));
            document.open();

            PdfBrandingHeaderBuilder.addDocumentHeader(document, company, storage, title, subtitle, null);
            bodyWriter.write(document);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("EXPORT_ERROR", "Failed to generate PDF report: " + e.getMessage());
        }
    }

    private static void addTableHeaderCell(PdfPTable table, String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FinLedgerPdfTheme.FONT_TH));
        cell.setBackgroundColor(FinLedgerPdfTheme.SURFACE_LIGHT);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColorBottom(FinLedgerPdfTheme.BORDER_MEDIUM);
        cell.setBorderWidthBottom(1.5f);
        cell.setPaddingTop(7f);
        cell.setPaddingBottom(7f);
        cell.setPaddingLeft(6f);
        cell.setPaddingRight(6f);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private static void addSectionRow(PdfPTable table, String sectionTitle, int colspan) {
        PdfPCell cell = new PdfPCell(new Phrase(sectionTitle, FinLedgerPdfTheme.FONT_SECTION));
        cell.setColspan(colspan);
        cell.setBackgroundColor(FinLedgerPdfTheme.SURFACE_LIGHT);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColorBottom(FinLedgerPdfTheme.BORDER_MEDIUM);
        cell.setBorderWidthBottom(1f);
        cell.setPaddingTop(9f);
        cell.setPaddingBottom(5f);
        cell.setPaddingLeft(6f);
        cell.setPaddingRight(6f);
        table.addCell(cell);
    }

    private static void addReportAccountRow(PdfPTable table, ReportLineDto line, boolean hasCompare) {
        Phrase p = new Phrase();
        if (line.accountCode() != null && !line.accountCode().isBlank()) {
            p.add(new Chunk(line.accountCode() + "  ", FinLedgerPdfTheme.FONT_CODE_BLUE));
        }
        p.add(new Chunk(FinLedgerPdfTheme.safe(line.accountName()), FinLedgerPdfTheme.FONT_BODY));

        PdfPCell nameCell = new PdfPCell(p);
        nameCell.setBorder(Rectangle.BOTTOM);
        nameCell.setBorderColorBottom(FinLedgerPdfTheme.SURFACE_SHADED);
        nameCell.setBorderWidthBottom(0.75f);
        nameCell.setPaddingTop(5.5f);
        nameCell.setPaddingBottom(5.5f);
        nameCell.setPaddingLeft(18f); // indent-1 matching webpage
        nameCell.setPaddingRight(6f);
        table.addCell(nameCell);

        table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoney(line.amount()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
        if (hasCompare) {
            table.addCell(bodyCell(FinLedgerPdfTheme.fmtMoney(line.compareAmount()), FinLedgerPdfTheme.FONT_META, Element.ALIGN_RIGHT));
            table.addCell(bodyCell(FinLedgerPdfTheme.fmtPct(line.changePct()), FinLedgerPdfTheme.FONT_META_BOLD, Element.ALIGN_RIGHT));
        }
    }

    private static void addIndentedLabelRow(PdfPTable table, String label, String value) {
        PdfPCell nameCell = new PdfPCell(new Phrase(label, FinLedgerPdfTheme.FONT_BODY));
        nameCell.setBorder(Rectangle.BOTTOM);
        nameCell.setBorderColorBottom(FinLedgerPdfTheme.SURFACE_SHADED);
        nameCell.setBorderWidthBottom(0.75f);
        nameCell.setPaddingTop(5.5f);
        nameCell.setPaddingBottom(5.5f);
        nameCell.setPaddingLeft(18f);
        nameCell.setPaddingRight(6f);
        table.addCell(nameCell);
        table.addCell(bodyCell(value, FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
    }

    private static void addSubtotalRow(PdfPTable table, String label, BigDecimal amount, BigDecimal compareAmount, boolean hasCompare) {
        table.addCell(subtotalCell(label, Element.ALIGN_LEFT));
        table.addCell(subtotalCell(FinLedgerPdfTheme.fmtMoney(amount), Element.ALIGN_RIGHT));
        if (hasCompare) {
            table.addCell(subtotalCell(FinLedgerPdfTheme.fmtMoney(compareAmount), Element.ALIGN_RIGHT));
            table.addCell(subtotalCell("", Element.ALIGN_RIGHT));
        }
    }

    private static void addMajorRow(PdfPTable table, String label, BigDecimal amount, BigDecimal compareAmount, boolean hasCompare) {
        table.addCell(majorCell(label, Element.ALIGN_LEFT));
        table.addCell(majorCell(FinLedgerPdfTheme.fmtMoney(amount), Element.ALIGN_RIGHT));
        if (hasCompare) {
            table.addCell(majorCell(FinLedgerPdfTheme.fmtMoney(compareAmount), Element.ALIGN_RIGHT));
            table.addCell(majorCell("", Element.ALIGN_RIGHT));
        }
    }

    private static void addGrandTotalComparisonRow(PdfPTable table, String label, BigDecimal amount, BigDecimal compareAmount, boolean hasCompare) {
        table.addCell(grandTotalCell(label, 1, Element.ALIGN_LEFT));
        table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(amount), 1, Element.ALIGN_RIGHT));
        if (hasCompare) {
            table.addCell(grandTotalCell(FinLedgerPdfTheme.fmtMoney(compareAmount), 1, Element.ALIGN_RIGHT));
            table.addCell(grandTotalCell("", 1, Element.ALIGN_RIGHT));
        }
    }

    private static void addGrandTotalPairRow(PdfPTable table, String label, String value) {
        table.addCell(grandTotalCell(label, 1, Element.ALIGN_LEFT));
        table.addCell(grandTotalCell(value, 1, Element.ALIGN_RIGHT));
    }

    private static void addVatBoxRow(PdfPTable table, String boxCode, String desc, String taxable, String vatAmt) {
        table.addCell(bodyCell(boxCode, FinLedgerPdfTheme.FONT_BODY_BOLD, Element.ALIGN_LEFT));
        table.addCell(bodyCell(desc, FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_LEFT));
        table.addCell(bodyCell(taxable, FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
        table.addCell(bodyCell(vatAmt, FinLedgerPdfTheme.FONT_BODY_BOLD, Element.ALIGN_RIGHT));
    }

    private static PdfPCell bodyCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", font));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColorBottom(FinLedgerPdfTheme.SURFACE_SHADED);
        cell.setBorderWidthBottom(0.75f);
        cell.setPaddingTop(5.5f);
        cell.setPaddingBottom(5.5f);
        cell.setPaddingLeft(6f);
        cell.setPaddingRight(6f);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private static PdfPCell subtotalCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FinLedgerPdfTheme.FONT_SUBTOTAL));
        cell.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        cell.setBorderColor(FinLedgerPdfTheme.BORDER_MEDIUM);
        cell.setBorderWidth(1f);
        cell.setPaddingTop(6f);
        cell.setPaddingBottom(6f);
        cell.setPaddingLeft(6f);
        cell.setPaddingRight(6f);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private static PdfPCell majorCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FinLedgerPdfTheme.FONT_MAJOR));
        cell.setBackgroundColor(FinLedgerPdfTheme.SURFACE_SHADED);
        cell.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        cell.setBorderColor(FinLedgerPdfTheme.BORDER_DARK);
        cell.setBorderWidth(1.5f);
        cell.setPaddingTop(7f);
        cell.setPaddingBottom(7f);
        cell.setPaddingLeft(6f);
        cell.setPaddingRight(6f);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private static PdfPCell grandTotalCell(String text, int colspan, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FinLedgerPdfTheme.FONT_GRAND_TOTAL));
        cell.setColspan(colspan);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setCellEvent(new PdfCellRenderers.DoubleBottomBorderCellEvent());
        cell.setPaddingTop(8f);
        cell.setPaddingBottom(9f);
        cell.setPaddingLeft(6f);
        cell.setPaddingRight(6f);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private static void addEmptyRow(PdfPTable table, int colspan, String message) {
        PdfPCell cell = new PdfPCell(new Phrase(message, FinLedgerPdfTheme.FONT_DOC_SUB));
        cell.setColspan(colspan);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColorBottom(FinLedgerPdfTheme.SURFACE_SHADED);
        cell.setPadding(16f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private static void addSummaryCards(Document doc, String[][] cards) throws Exception {
        PdfPTable grid = new PdfPTable(cards.length);
        grid.setWidthPercentage(100);
        grid.setSpacingAfter(12f);

        for (String[] card : cards) {
            PdfPCell outer = new PdfPCell();
            outer.setBorder(Rectangle.NO_BORDER);
            outer.setPaddingLeft(3f);
            outer.setPaddingRight(3f);
            outer.setPaddingTop(0f);
            outer.setPaddingBottom(0f);

            PdfPTable inner = new PdfPTable(1);
            inner.setWidthPercentage(100);
            PdfPCell box = new PdfPCell();
            box.setBorder(Rectangle.NO_BORDER);
            box.setPadding(8f);
            box.setCellEvent(new PdfCellRenderers.RoundedBoxCellEvent(
                    FinLedgerPdfTheme.SURFACE_LIGHT, FinLedgerPdfTheme.BORDER_LIGHT, 5f, 0.85f));

            Paragraph lbl = new Paragraph(card[0], FinLedgerPdfTheme.FONT_CARD_LABEL);
            lbl.setSpacingAfter(2f);
            box.addElement(lbl);
            box.addElement(new Paragraph(card[1], FinLedgerPdfTheme.FONT_CARD_VAL));

            inner.addCell(box);
            outer.addElement(inner);
            grid.addCell(outer);
        }
        doc.add(grid);
    }

    private static void addStatusBanner(Document doc, boolean success, String okText, String errText) throws Exception {
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        banner.setSpacingBefore(12f);

        Color bg = success ? FinLedgerPdfTheme.SUCCESS_BG : FinLedgerPdfTheme.DANGER_BG;
        Color fg = success ? FinLedgerPdfTheme.SUCCESS_TEXT : FinLedgerPdfTheme.DANGER_TEXT;
        Color border = success ? FinLedgerPdfTheme.SUCCESS_BORDER : FinLedgerPdfTheme.DANGER_BORDER;
        String text = (success ? "✓ " : "! ") + (success ? okText : errText);

        Font font = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 9f, fg);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(8f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setCellEvent(new PdfCellRenderers.RoundedBoxCellEvent(bg, border, 5f, 0.85f));
        banner.addCell(cell);
        doc.add(banner);
    }

    private static String currency(Company company) {
        return (company != null && company.getBaseCurrency() != null && !company.getBaseCurrency().isBlank())
                ? company.getBaseCurrency() : "AED";
    }

    private static BigDecimal zero(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private static boolean hasPositive(BigDecimal val) {
        return val != null && val.compareTo(BigDecimal.ZERO) > 0;
    }

    private static <T> List<T> safeList(List<T> list) {
        return list != null ? list : List.of();
    }
}
