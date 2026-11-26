package com.backend.finledger.report.excel;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.ledger.dto.GeneralLedgerLineDto;
import com.backend.finledger.ledger.dto.GeneralLedgerReportDto;
import com.backend.finledger.ledger.dto.TrialBalanceLineDto;
import com.backend.finledger.ledger.dto.TrialBalanceReportDto;
import com.backend.finledger.purchases.dto.ApAgeingReportDto;
import com.backend.finledger.purchases.dto.ApAgeingVendorDto;
import com.backend.finledger.report.dto.*;
import com.backend.finledger.sales.dto.ArAgeingCustomerDto;
import com.backend.finledger.sales.dto.ArAgeingReportDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Professional Excel (.xlsx) renderer producing presentation-grade accounting workbooks
 * with hierarchy, live Excel formulas, real numeric cells, and accounting styling.
 */
public final class ReportExcelRenderer {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

    private ReportExcelRenderer() {}

    // ─── 1. Profit & Loss ───────────────────────────────────────────────────

    public static byte[] renderProfitLoss(ProfitLossReportDto pl, Company company) {
        String ccy = currency(company);
        boolean hasCompare = pl.comparePeriod() != null && !pl.comparePeriod().isBlank();
        String subtitle = "For the period " + fmtDate(pl.fromDate()) + " to " + fmtDate(pl.toDate()) + " (" + ccy + ")";

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("Profit & Loss");
            ExcelStyles st = new ExcelStyles(wb);

            renderTopHeader(sheet, st, company, "Statement of Profit or Loss", subtitle);

            int rowIdx = 4;
            String[] headers = hasCompare
                    ? new String[]{"Account Code", "Account Name", "Current Period (" + ccy + ")", "Comparative (" + ccy + ")", "% Change"}
                    : new String[]{"Account Code", "Account Name", "Amount (" + ccy + ")"};
            HorizontalAlignment[] aligns = hasCompare
                    ? new HorizontalAlignment[]{HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT}
                    : new HorizontalAlignment[]{HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, HorizontalAlignment.RIGHT};
            renderTableHeaders(sheet, rowIdx++, st, headers, aligns);

            int numCols = headers.length;

            // Operating Revenue
            renderSectionHeader(sheet, rowIdx++, st, "OPERATING REVENUE", numCols);
            int revStart = rowIdx;
            for (ReportLineDto l : safeList(pl.revenueLines())) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(safe(l.accountCode()));
                r.getCell(0).setCellStyle(st.code);
                r.createCell(1).setCellValue(safe(l.accountName()));
                r.getCell(1).setCellStyle(st.textIndent);
                Cell cAmt = r.createCell(2);
                cAmt.setCellValue(toDouble(l.amount()));
                cAmt.setCellStyle(st.number);
            }
            int revEnd = rowIdx - 1;
            Row rRevSub = sheet.createRow(rowIdx++);
            rRevSub.createCell(0).setCellValue("");
            rRevSub.createCell(1).setCellValue("Total Operating Revenue");
            rRevSub.getCell(1).setCellStyle(st.subtotalLabel);
            Cell cRevSub = rRevSub.createCell(2);
            cRevSub.setCellFormula(sumFormula(2, revStart, revEnd));
            cRevSub.setCellStyle(st.subtotalNumber);
            int revSubRow = rowIdx - 1;

            // Cost of Goods Sold
            renderSectionHeader(sheet, rowIdx++, st, "COST OF GOODS SOLD", numCols);
            int cogsStart = rowIdx;
            for (ReportLineDto l : safeList(pl.cogsLines())) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(safe(l.accountCode()));
                r.getCell(0).setCellStyle(st.code);
                r.createCell(1).setCellValue(safe(l.accountName()));
                r.getCell(1).setCellStyle(st.textIndent);
                Cell cAmt = r.createCell(2);
                cAmt.setCellValue(toDouble(l.amount()));
                cAmt.setCellStyle(st.number);
            }
            int cogsEnd = rowIdx - 1;
            Row rCogsSub = sheet.createRow(rowIdx++);
            rCogsSub.createCell(0).setCellValue("");
            rCogsSub.createCell(1).setCellValue("Total Cost of Goods Sold");
            rCogsSub.getCell(1).setCellStyle(st.subtotalLabel);
            Cell cCogsSub = rCogsSub.createCell(2);
            cCogsSub.setCellFormula(sumFormula(2, cogsStart, cogsEnd));
            cCogsSub.setCellStyle(st.subtotalNumber);
            int cogsSubRow = rowIdx - 1;

            // Gross Profit
            Row rGp = sheet.createRow(rowIdx++);
            rGp.createCell(0).setCellValue("");
            rGp.createCell(1).setCellValue("Gross Profit");
            rGp.getCell(1).setCellStyle(st.majorLabel);
            Cell cGp = rGp.createCell(2);
            cGp.setCellFormula(cellRef(2, revSubRow) + "-" + cellRef(2, cogsSubRow));
            cGp.setCellStyle(st.majorNumber);
            int gpRow = rowIdx - 1;

            // Operating Expenses
            renderSectionHeader(sheet, rowIdx++, st, "OPERATING EXPENSES", numCols);
            int expStart = rowIdx;
            for (ReportLineDto l : safeList(pl.expenseLines())) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(safe(l.accountCode()));
                r.getCell(0).setCellStyle(st.code);
                r.createCell(1).setCellValue(safe(l.accountName()));
                r.getCell(1).setCellStyle(st.textIndent);
                Cell cAmt = r.createCell(2);
                cAmt.setCellValue(toDouble(l.amount()));
                cAmt.setCellStyle(st.number);
            }
            int expEnd = rowIdx - 1;
            Row rExpSub = sheet.createRow(rowIdx++);
            rExpSub.createCell(0).setCellValue("");
            rExpSub.createCell(1).setCellValue("Total Operating Expenses");
            rExpSub.getCell(1).setCellStyle(st.subtotalLabel);
            Cell cExpSub = rExpSub.createCell(2);
            cExpSub.setCellFormula(sumFormula(2, expStart, expEnd));
            cExpSub.setCellStyle(st.subtotalNumber);
            int expSubRow = rowIdx - 1;

            // Net Profit / (Loss)
            Row rNp = sheet.createRow(rowIdx++);
            rNp.createCell(0).setCellValue("");
            rNp.createCell(1).setCellValue("NET PROFIT / (LOSS)");
            rNp.getCell(1).setCellStyle(st.grandTotalLabel);
            Cell cNp = rNp.createCell(2);
            cNp.setCellFormula(cellRef(2, gpRow) + "-" + cellRef(2, expSubRow));
            cNp.setCellStyle(st.grandTotalNumber);

            finalizeSheet(sheet, numCols);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("EXPORT_ERROR", "Failed to render Profit & Loss Excel report: " + e.getMessage());
        }
    }

    // ─── 2. Balance Sheet ───────────────────────────────────────────────────

    public static byte[] renderBalanceSheet(BalanceSheetReportDto bs, Company company) {
        String ccy = currency(company);
        String subtitle = "As of " + fmtDate(bs.asOfDate()) + " (" + ccy + ")";

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("Balance Sheet");
            ExcelStyles st = new ExcelStyles(wb);

            renderTopHeader(sheet, st, company, "Statement of Financial Position (Balance Sheet)", subtitle);

            int rowIdx = 4;
            String[] headers = new String[]{"Account Code", "Account Name", "Balance (" + ccy + ")"};
            HorizontalAlignment[] aligns = new HorizontalAlignment[]{HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, HorizontalAlignment.RIGHT};
            renderTableHeaders(sheet, rowIdx++, st, headers, aligns);

            int numCols = 3;

            // Current Assets
            renderSectionHeader(sheet, rowIdx++, st, "CURRENT ASSETS", numCols);
            int caStart = rowIdx;
            for (ReportLineDto l : safeList(bs.currentAssets())) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(safe(l.accountCode()));
                r.getCell(0).setCellStyle(st.code);
                r.createCell(1).setCellValue(safe(l.accountName()));
                r.getCell(1).setCellStyle(st.textIndent);
                Cell c = r.createCell(2);
                c.setCellValue(toDouble(l.amount()));
                c.setCellStyle(st.number);
            }
            int caEnd = rowIdx - 1;
            Row rCaSub = sheet.createRow(rowIdx++);
            rCaSub.createCell(0).setCellValue("");
            rCaSub.createCell(1).setCellValue("Total Current Assets");
            rCaSub.getCell(1).setCellStyle(st.subtotalLabel);
            Cell cCaSub = rCaSub.createCell(2);
            cCaSub.setCellFormula(sumFormula(2, caStart, caEnd));
            cCaSub.setCellStyle(st.subtotalNumber);
            int caSubRow = rowIdx - 1;

            // Non-Current Assets
            renderSectionHeader(sheet, rowIdx++, st, "NON-CURRENT ASSETS", numCols);
            int ncaStart = rowIdx;
            for (ReportLineDto l : safeList(bs.nonCurrentAssets())) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(safe(l.accountCode()));
                r.getCell(0).setCellStyle(st.code);
                r.createCell(1).setCellValue(safe(l.accountName()));
                r.getCell(1).setCellStyle(st.textIndent);
                Cell c = r.createCell(2);
                c.setCellValue(toDouble(l.amount()));
                c.setCellStyle(st.number);
            }
            int ncaEnd = rowIdx - 1;
            Row rNcaSub = sheet.createRow(rowIdx++);
            rNcaSub.createCell(0).setCellValue("");
            rNcaSub.createCell(1).setCellValue("Total Non-Current Assets");
            rNcaSub.getCell(1).setCellStyle(st.subtotalLabel);
            Cell cNcaSub = rNcaSub.createCell(2);
            cNcaSub.setCellFormula(sumFormula(2, ncaStart, ncaEnd));
            cNcaSub.setCellStyle(st.subtotalNumber);
            int ncaSubRow = rowIdx - 1;

            // Total Assets
            Row rTa = sheet.createRow(rowIdx++);
            rTa.createCell(0).setCellValue("");
            rTa.createCell(1).setCellValue("TOTAL ASSETS");
            rTa.getCell(1).setCellStyle(st.majorLabel);
            Cell cTa = rTa.createCell(2);
            cTa.setCellFormula(cellRef(2, caSubRow) + "+" + cellRef(2, ncaSubRow));
            cTa.setCellStyle(st.majorNumber);

            // Current Liabilities
            renderSectionHeader(sheet, rowIdx++, st, "CURRENT LIABILITIES", numCols);
            int clStart = rowIdx;
            for (ReportLineDto l : safeList(bs.currentLiabilities())) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(safe(l.accountCode()));
                r.getCell(0).setCellStyle(st.code);
                r.createCell(1).setCellValue(safe(l.accountName()));
                r.getCell(1).setCellStyle(st.textIndent);
                Cell c = r.createCell(2);
                c.setCellValue(toDouble(l.amount()));
                c.setCellStyle(st.number);
            }
            int clEnd = rowIdx - 1;
            Row rClSub = sheet.createRow(rowIdx++);
            rClSub.createCell(0).setCellValue("");
            rClSub.createCell(1).setCellValue("Total Current Liabilities");
            rClSub.getCell(1).setCellStyle(st.subtotalLabel);
            Cell cClSub = rClSub.createCell(2);
            cClSub.setCellFormula(sumFormula(2, clStart, clEnd));
            cClSub.setCellStyle(st.subtotalNumber);
            int clSubRow = rowIdx - 1;

            // Non-Current Liabilities
            renderSectionHeader(sheet, rowIdx++, st, "NON-CURRENT LIABILITIES", numCols);
            int nclStart = rowIdx;
            for (ReportLineDto l : safeList(bs.nonCurrentLiabilities())) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(safe(l.accountCode()));
                r.getCell(0).setCellStyle(st.code);
                r.createCell(1).setCellValue(safe(l.accountName()));
                r.getCell(1).setCellStyle(st.textIndent);
                Cell c = r.createCell(2);
                c.setCellValue(toDouble(l.amount()));
                c.setCellStyle(st.number);
            }
            int nclEnd = rowIdx - 1;
            Row rNclSub = sheet.createRow(rowIdx++);
            rNclSub.createCell(0).setCellValue("");
            rNclSub.createCell(1).setCellValue("Total Non-Current Liabilities");
            rNclSub.getCell(1).setCellStyle(st.subtotalLabel);
            Cell cNclSub = rNclSub.createCell(2);
            cNclSub.setCellFormula(sumFormula(2, nclStart, nclEnd));
            cNclSub.setCellStyle(st.subtotalNumber);
            int nclSubRow = rowIdx - 1;

            // Equity & Reserves
            renderSectionHeader(sheet, rowIdx++, st, "EQUITY & RESERVES", numCols);
            int eqStart = rowIdx;
            for (ReportLineDto l : safeList(bs.equityLines())) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(safe(l.accountCode()));
                r.getCell(0).setCellStyle(st.code);
                r.createCell(1).setCellValue(safe(l.accountName()));
                r.getCell(1).setCellStyle(st.textIndent);
                Cell c = r.createCell(2);
                c.setCellValue(toDouble(l.amount()));
                c.setCellStyle(st.number);
            }

            // Retained Earnings (only once if non-zero; no duplicate 0.00 row)
            if (bs.retainedEarnings() != null && bs.retainedEarnings().compareTo(BigDecimal.ZERO) != 0) {
                Row rRet = sheet.createRow(rowIdx++);
                rRet.createCell(0).setCellValue("3200");
                rRet.getCell(0).setCellStyle(st.code);
                rRet.createCell(1).setCellValue("Retained Earnings");
                rRet.getCell(1).setCellStyle(st.textIndent);
                Cell cRet = rRet.createCell(2);
                cRet.setCellValue(bs.retainedEarnings().doubleValue());
                cRet.setCellStyle(st.number);
            }

            // Net Income YTD
            Row rNet = sheet.createRow(rowIdx++);
            rNet.createCell(0).setCellValue("-");
            rNet.getCell(0).setCellStyle(st.text);
            rNet.createCell(1).setCellValue("Net Income Year-To-Date");
            rNet.getCell(1).setCellStyle(st.textIndent);
            Cell cNet = rNet.createCell(2);
            cNet.setCellValue(toDouble(bs.netIncomeYearToDate()));
            cNet.setCellStyle(st.number);

            int eqEnd = rowIdx - 1;
            Row rEqSub = sheet.createRow(rowIdx++);
            rEqSub.createCell(0).setCellValue("");
            rEqSub.createCell(1).setCellValue("Total Equity");
            rEqSub.getCell(1).setCellStyle(st.subtotalLabel);
            Cell cEqSub = rEqSub.createCell(2);
            cEqSub.setCellFormula(sumFormula(2, eqStart, eqEnd));
            cEqSub.setCellStyle(st.subtotalNumber);
            int eqSubRow = rowIdx - 1;

            // Total Liabilities & Equity
            Row rTle = sheet.createRow(rowIdx++);
            rTle.createCell(0).setCellValue("");
            rTle.createCell(1).setCellValue("TOTAL LIABILITIES AND EQUITY");
            rTle.getCell(1).setCellStyle(st.grandTotalLabel);
            Cell cTle = rTle.createCell(2);
            cTle.setCellFormula(cellRef(2, clSubRow) + "+" + cellRef(2, nclSubRow) + "+" + cellRef(2, eqSubRow));
            cTle.setCellStyle(st.grandTotalNumber);

            finalizeSheet(sheet, numCols);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("EXPORT_ERROR", "Failed to render Balance Sheet Excel report: " + e.getMessage());
        }
    }

    // ─── 3. Trial Balance ───────────────────────────────────────────────────

    public static byte[] renderTrialBalance(TrialBalanceReportDto tb, Company company) {
        String ccy = currency(company);
        String subtitle = "As of " + fmtDate(tb.asOfDate()) + " (" + ccy + ")";

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("Trial Balance");
            ExcelStyles st = new ExcelStyles(wb);

            renderTopHeader(sheet, st, company, "Trial Balance", subtitle);

            int rowIdx = 4;
            String[] headers = new String[]{"Account Code", "Account Name", "Type", "Debit (" + ccy + ")", "Credit (" + ccy + ")"};
            HorizontalAlignment[] aligns = new HorizontalAlignment[]{HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT};
            renderTableHeaders(sheet, rowIdx++, st, headers, aligns);

            int startRow = rowIdx;
            for (TrialBalanceLineDto l : safeList(tb.lines())) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(safe(l.accountCode()));
                r.getCell(0).setCellStyle(st.code);
                r.createCell(1).setCellValue(safe(l.accountName()));
                r.getCell(1).setCellStyle(st.text);
                r.createCell(2).setCellValue(l.accountType() != null ? l.accountType().name() : "-");
                r.getCell(2).setCellStyle(st.text);

                Cell cDr = r.createCell(3);
                cDr.setCellValue(toDouble(l.debitBalance()));
                cDr.setCellStyle(st.number);

                Cell cCr = r.createCell(4);
                cCr.setCellValue(toDouble(l.creditBalance()));
                cCr.setCellStyle(st.number);
            }
            int endRow = rowIdx - 1;

            Row rTot = sheet.createRow(rowIdx++);
            rTot.createCell(0).setCellValue("");
            rTot.createCell(1).setCellValue("TOTAL");
            rTot.getCell(1).setCellStyle(st.grandTotalLabel);
            rTot.createCell(2).setCellValue("");
            rTot.getCell(2).setCellStyle(st.grandTotalLabel);

            Cell cDrTot = rTot.createCell(3);
            cDrTot.setCellFormula(sumFormula(3, startRow, endRow));
            cDrTot.setCellStyle(st.grandTotalNumber);

            Cell cCrTot = rTot.createCell(4);
            cCrTot.setCellFormula(sumFormula(4, startRow, endRow));
            cCrTot.setCellStyle(st.grandTotalNumber);

            finalizeSheet(sheet, 5);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("EXPORT_ERROR", "Failed to render Trial Balance Excel report: " + e.getMessage());
        }
    }

    // ─── 4. General Ledger ──────────────────────────────────────────────────

    public static byte[] renderGeneralLedger(GeneralLedgerReportDto gl, Company company) {
        String ccy = currency(company);
        String subtitle = safe(gl.accountCode()) + " " + safe(gl.accountName())
                + "  •  Period: " + fmtDate(gl.fromDate()) + " to " + fmtDate(gl.toDate())
                + "  •  Normal Balance: " + gl.normalBalance() + " (" + ccy + ")";

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("General Ledger");
            ExcelStyles st = new ExcelStyles(wb);

            renderTopHeader(sheet, st, company, "General Ledger Report", subtitle);

            int rowIdx = 4;
            String[] headers = new String[]{"Date", "Entry #", "Reference", "Description", "Debit (" + ccy + ")", "Credit (" + ccy + ")", "Running Balance (" + ccy + ")"};
            HorizontalAlignment[] aligns = new HorizontalAlignment[]{HorizontalAlignment.CENTER, HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT};
            renderTableHeaders(sheet, rowIdx++, st, headers, aligns);

            // Opening Balance Row
            Row rOpen = sheet.createRow(rowIdx++);
            rOpen.createCell(0).setCellValue(fmtDate(gl.fromDate()));
            rOpen.getCell(0).setCellStyle(st.date);
            rOpen.createCell(1).setCellValue("-");
            rOpen.getCell(1).setCellStyle(st.text);
            rOpen.createCell(2).setCellValue("-");
            rOpen.getCell(2).setCellStyle(st.text);
            rOpen.createCell(3).setCellValue("Opening Balance");
            rOpen.getCell(3).setCellStyle(st.textBold);
            rOpen.createCell(4).setCellValue(0.0);
            rOpen.getCell(4).setCellStyle(st.number);
            rOpen.createCell(5).setCellValue(0.0);
            rOpen.getCell(5).setCellStyle(st.number);
            Cell cOpenBal = rOpen.createCell(6);
            cOpenBal.setCellValue(toDouble(gl.openingBalance()));
            cOpenBal.setCellStyle(st.numberBold);

            int startRow = rowIdx;
            for (GeneralLedgerLineDto l : safeList(gl.lines())) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(fmtDate(l.date()));
                r.getCell(0).setCellStyle(st.date);
                r.createCell(1).setCellValue(safe(l.entryNo()));
                r.getCell(1).setCellStyle(st.code);
                r.createCell(2).setCellValue(safe(l.reference()));
                r.getCell(2).setCellStyle(st.text);
                r.createCell(3).setCellValue(safe(l.description()));
                r.getCell(3).setCellStyle(st.text);

                Cell cDr = r.createCell(4);
                cDr.setCellValue(toDouble(l.debit()));
                cDr.setCellStyle(st.number);

                Cell cCr = r.createCell(5);
                cCr.setCellValue(toDouble(l.credit()));
                cCr.setCellStyle(st.number);

                Cell cBal = r.createCell(6);
                cBal.setCellValue(toDouble(l.runningBalance()));
                cBal.setCellStyle(st.numberBold);
            }
            int endRow = rowIdx - 1;

            Row rTot = sheet.createRow(rowIdx++);
            rTot.createCell(0).setCellValue("");
            rTot.createCell(1).setCellValue("");
            rTot.createCell(2).setCellValue("");
            rTot.createCell(3).setCellValue("TOTAL ACTIVITY & CLOSING BALANCE");
            rTot.getCell(3).setCellStyle(st.grandTotalLabel);

            Cell cDrTot = rTot.createCell(4);
            cDrTot.setCellFormula(sumFormula(4, startRow, endRow));
            cDrTot.setCellStyle(st.grandTotalNumber);

            Cell cCrTot = rTot.createCell(5);
            cCrTot.setCellFormula(sumFormula(5, startRow, endRow));
            cCrTot.setCellStyle(st.grandTotalNumber);

            Cell cCloseBal = rTot.createCell(6);
            cCloseBal.setCellValue(toDouble(gl.closingBalance()));
            cCloseBal.setCellStyle(st.grandTotalNumber);

            finalizeSheet(sheet, 7);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("EXPORT_ERROR", "Failed to render General Ledger Excel report: " + e.getMessage());
        }
    }

    // ─── 5. AR Ageing ───────────────────────────────────────────────────────

    public static byte[] renderArAgeing(ArAgeingReportDto ar, Company company) {
        String ccy = currency(company);
        String subtitle = "Outstanding customer receivables as of " + fmtDate(ar.asOfDate()) + " (" + ccy + ")";

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("AR Ageing");
            ExcelStyles st = new ExcelStyles(wb);

            renderTopHeader(sheet, st, company, "Accounts Receivable Ageing", subtitle);

            int rowIdx = 4;
            String[] headers = new String[]{"Customer Code", "Customer Name", "Current (" + ccy + ")", "1-30 Days (" + ccy + ")", "31-60 Days (" + ccy + ")", "61-90 Days (" + ccy + ")", ">90 Days (" + ccy + ")", "Total Due (" + ccy + ")"};
            HorizontalAlignment[] aligns = new HorizontalAlignment[]{HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT};
            renderTableHeaders(sheet, rowIdx++, st, headers, aligns);

            int startRow = rowIdx;
            for (ArAgeingCustomerDto c : safeList(ar.customers())) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(safe(c.customerCode()));
                r.getCell(0).setCellStyle(st.code);
                r.createCell(1).setCellValue(safe(c.customerName()));
                r.getCell(1).setCellStyle(st.textBold);

                r.createCell(2).setCellValue(toDouble(c.current()));
                r.getCell(2).setCellStyle(st.number);
                r.createCell(3).setCellValue(toDouble(c.days1To30()));
                r.getCell(3).setCellStyle(st.number);
                r.createCell(4).setCellValue(toDouble(c.days31To60()));
                r.getCell(4).setCellStyle(st.number);
                r.createCell(5).setCellValue(toDouble(c.days61To90()));
                r.getCell(5).setCellStyle(st.number);
                r.createCell(6).setCellValue(toDouble(c.daysOver90()));
                r.getCell(6).setCellStyle(st.number);
                r.createCell(7).setCellValue(toDouble(c.totalOutstanding()));
                r.getCell(7).setCellStyle(st.numberBold);
            }
            int endRow = rowIdx - 1;

            Row rTot = sheet.createRow(rowIdx++);
            rTot.createCell(0).setCellValue("");
            rTot.createCell(1).setCellValue("TOTAL RECEIVABLES");
            rTot.getCell(1).setCellStyle(st.grandTotalLabel);

            for (int col = 2; col <= 7; col++) {
                Cell c = rTot.createCell(col);
                c.setCellFormula(sumFormula(col, startRow, endRow));
                c.setCellStyle(st.grandTotalNumber);
            }

            finalizeSheet(sheet, 8);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("EXPORT_ERROR", "Failed to render AR Ageing Excel report: " + e.getMessage());
        }
    }

    // ─── 6. AP Ageing ───────────────────────────────────────────────────────

    public static byte[] renderApAgeing(ApAgeingReportDto ap, Company company) {
        String ccy = currency(company);
        String subtitle = "Outstanding vendor payables as of " + fmtDate(ap.asOfDate()) + " (" + ccy + ")";

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("AP Ageing");
            ExcelStyles st = new ExcelStyles(wb);

            renderTopHeader(sheet, st, company, "Accounts Payable Ageing", subtitle);

            int rowIdx = 4;
            String[] headers = new String[]{"Vendor Code", "Vendor Name", "Current (" + ccy + ")", "1-30 Days (" + ccy + ")", "31-60 Days (" + ccy + ")", "61-90 Days (" + ccy + ")", ">90 Days (" + ccy + ")", "Total Due (" + ccy + ")"};
            HorizontalAlignment[] aligns = new HorizontalAlignment[]{HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT};
            renderTableHeaders(sheet, rowIdx++, st, headers, aligns);

            int startRow = rowIdx;
            for (ApAgeingVendorDto v : safeList(ap.vendors())) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(safe(v.vendorCode()));
                r.getCell(0).setCellStyle(st.code);
                r.createCell(1).setCellValue(safe(v.vendorName()));
                r.getCell(1).setCellStyle(st.textBold);

                r.createCell(2).setCellValue(toDouble(v.current()));
                r.getCell(2).setCellStyle(st.number);
                r.createCell(3).setCellValue(toDouble(v.days1To30()));
                r.getCell(3).setCellStyle(st.number);
                r.createCell(4).setCellValue(toDouble(v.days31To60()));
                r.getCell(4).setCellStyle(st.number);
                r.createCell(5).setCellValue(toDouble(v.days61To90()));
                r.getCell(5).setCellStyle(st.number);
                r.createCell(6).setCellValue(toDouble(v.daysOver90()));
                r.getCell(6).setCellStyle(st.number);
                r.createCell(7).setCellValue(toDouble(v.totalOutstanding()));
                r.getCell(7).setCellStyle(st.numberBold);
            }
            int endRow = rowIdx - 1;

            Row rTot = sheet.createRow(rowIdx++);
            rTot.createCell(0).setCellValue("");
            rTot.createCell(1).setCellValue("TOTAL PAYABLES");
            rTot.getCell(1).setCellStyle(st.grandTotalLabel);

            for (int col = 2; col <= 7; col++) {
                Cell c = rTot.createCell(col);
                c.setCellFormula(sumFormula(col, startRow, endRow));
                c.setCellStyle(st.grandTotalNumber);
            }

            finalizeSheet(sheet, 8);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("EXPORT_ERROR", "Failed to render AP Ageing Excel report: " + e.getMessage());
        }
    }

    // ─── 7. VAT Return ──────────────────────────────────────────────────────

    public static byte[] renderVatReturn(VatReturnReportDto vat, Company company) {
        String ccy = currency(company);
        String subtitle = "Period: " + fmtDate(vat.fromDate()) + " to " + fmtDate(vat.toDate()) + " (" + ccy + ")";

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("VAT Return");
            ExcelStyles st = new ExcelStyles(wb);

            renderTopHeader(sheet, st, company, "VAT 201 Return (FTA UAE)", subtitle);

            int rowIdx = 4;
            String[] headers = new String[]{"Box #", "Description", "Taxable Amount (" + ccy + ")", "VAT Amount (" + ccy + ")"};
            HorizontalAlignment[] aligns = new HorizontalAlignment[]{HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT};
            renderTableHeaders(sheet, rowIdx++, st, headers, aligns);

            int numCols = 4;

            // Outputs
            renderSectionHeader(sheet, rowIdx++, st, "VAT ON SALES AND ALL OTHER OUTPUTS", numCols);
            int outStart = rowIdx;

            addVatLine(sheet, rowIdx++, st, "Box 1a", "Standard rated supplies in UAE (5%)", toDouble(vat.standardRatedSalesAmount()), toDouble(vat.standardRatedSalesVat()));
            addVatLine(sheet, rowIdx++, st, "Box 2", "Supplies subject to the reverse charge provisions", 0.0, 0.0);
            addVatLine(sheet, rowIdx++, st, "Box 3", "Zero-rated supplies", toDouble(vat.zeroRatedSalesAmount()), 0.0);
            addVatLine(sheet, rowIdx++, st, "Box 4", "Exempt supplies", toDouble(vat.exemptSalesAmount()), 0.0);

            int outEnd = rowIdx - 1;
            Row rOutSub = sheet.createRow(rowIdx++);
            rOutSub.createCell(0).setCellValue("");
            rOutSub.createCell(1).setCellValue("Total Output VAT Due");
            rOutSub.getCell(1).setCellStyle(st.subtotalLabel);
            rOutSub.createCell(2).setCellValue("");
            rOutSub.getCell(2).setCellStyle(st.subtotalLabel);
            Cell cOutSub = rOutSub.createCell(3);
            cOutSub.setCellFormula(sumFormula(3, outStart, outEnd));
            cOutSub.setCellStyle(st.subtotalNumber);
            int outSubRow = rowIdx - 1;

            // Inputs
            renderSectionHeader(sheet, rowIdx++, st, "VAT ON EXPENSES AND ALL OTHER INPUTS", numCols);
            int inStart = rowIdx;
            addVatLine(sheet, rowIdx++, st, "Box 1g", "Standard rated expenses and purchases (5%)", toDouble(vat.standardRatedPurchasesAmount()), toDouble(vat.standardRatedPurchasesVat()));
            int inEnd = rowIdx - 1;

            Row rInSub = sheet.createRow(rowIdx++);
            rInSub.createCell(0).setCellValue("");
            rInSub.createCell(1).setCellValue("Total Recoverable Input VAT");
            rInSub.getCell(1).setCellStyle(st.subtotalLabel);
            rInSub.createCell(2).setCellValue("");
            rInSub.getCell(2).setCellStyle(st.subtotalLabel);
            Cell cInSub = rInSub.createCell(3);
            cInSub.setCellFormula(sumFormula(3, inStart, inEnd));
            cInSub.setCellStyle(st.subtotalNumber);
            int inSubRow = rowIdx - 1;

            // Grand Total (Box 13)
            Row rNet = sheet.createRow(rowIdx++);
            rNet.createCell(0).setCellValue("Box 13");
            rNet.getCell(0).setCellStyle(st.grandTotalLabel);
            rNet.createCell(1).setCellValue("NET VAT PAYABLE / (RECOVERABLE)");
            rNet.getCell(1).setCellStyle(st.grandTotalLabel);
            rNet.createCell(2).setCellValue("");
            rNet.getCell(2).setCellStyle(st.grandTotalLabel);
            Cell cNet = rNet.createCell(3);
            cNet.setCellFormula(cellRef(3, outSubRow) + "-" + cellRef(3, inSubRow));
            cNet.setCellStyle(st.grandTotalNumber);

            finalizeSheet(sheet, numCols);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("EXPORT_ERROR", "Failed to render VAT Return Excel report: " + e.getMessage());
        }
    }

    private static void addVatLine(Sheet sheet, int rowIdx, ExcelStyles st, String box, String desc, double taxable, double vat) {
        Row r = sheet.createRow(rowIdx);
        r.createCell(0).setCellValue(box);
        r.getCell(0).setCellStyle(st.code);
        r.createCell(1).setCellValue(desc);
        r.getCell(1).setCellStyle(st.text);
        Cell cTax = r.createCell(2);
        cTax.setCellValue(taxable);
        cTax.setCellStyle(st.number);
        Cell cVat = r.createCell(3);
        cVat.setCellValue(vat);
        cVat.setCellStyle(st.number);
    }

    // ─── 8. Stock Valuation ─────────────────────────────────────────────────

    public static byte[] renderStockValuation(StockValuationReportDto sv, Company company) {
        String ccy = currency(company);
        String subtitle = "Warehouse: " + safe(sv.warehouseName()) + "  |  As of: " + fmtDate(sv.asOfDate()) + " (" + ccy + ")";

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("Stock Valuation");
            ExcelStyles st = new ExcelStyles(wb);

            renderTopHeader(sheet, st, company, "Inventory Stock Valuation", subtitle);

            int rowIdx = 4;
            String[] headers = new String[]{"SKU", "Item Name", "Category", "Warehouse", "Qty On Hand", "Avg Cost (" + ccy + ")", "Total Valuation (" + ccy + ")"};
            HorizontalAlignment[] aligns = new HorizontalAlignment[]{HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT};
            renderTableHeaders(sheet, rowIdx++, st, headers, aligns);

            int startRow = rowIdx;
            for (StockValuationLineDto l : safeList(sv.lines())) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(safe(l.sku()));
                r.getCell(0).setCellStyle(st.code);
                r.createCell(1).setCellValue(safe(l.itemName()));
                r.getCell(1).setCellStyle(st.textBold);
                r.createCell(2).setCellValue(safe(l.categoryName()));
                r.getCell(2).setCellStyle(st.text);
                r.createCell(3).setCellValue(safe(l.warehouseName()));
                r.getCell(3).setCellStyle(st.text);

                Cell cQty = r.createCell(4);
                cQty.setCellValue(toDouble(l.quantityOnHand()));
                cQty.setCellStyle(st.number);

                Cell cCost = r.createCell(5);
                cCost.setCellValue(toDouble(l.unitCost()));
                cCost.setCellStyle(st.number);

                Cell cVal = r.createCell(6);
                cVal.setCellValue(toDouble(l.totalValue()));
                cVal.setCellStyle(st.numberBold);
            }
            int endRow = rowIdx - 1;

            Row rTot = sheet.createRow(rowIdx++);
            rTot.createCell(0).setCellValue("");
            rTot.createCell(1).setCellValue("GRAND TOTAL INVENTORY VALUATION");
            rTot.getCell(1).setCellStyle(st.grandTotalLabel);
            for (int col = 2; col <= 5; col++) {
                rTot.createCell(col).setCellValue("");
                rTot.getCell(col).setCellStyle(st.grandTotalLabel);
            }
            Cell cTot = rTot.createCell(6);
            cTot.setCellFormula(sumFormula(6, startRow, endRow));
            cTot.setCellStyle(st.grandTotalNumber);

            finalizeSheet(sheet, 7);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("EXPORT_ERROR", "Failed to render Stock Valuation Excel report: " + e.getMessage());
        }
    }

    // ─── 9. Stock Movement ──────────────────────────────────────────────────

    public static byte[] renderStockMovement(StockMovementReportDto sm, Company company) {
        String ccy = currency(company);
        String subtitle = safe(sm.sku()) + " " + safe(sm.itemName())
                + "  •  Period: " + fmtDate(sm.fromDate()) + " to " + fmtDate(sm.toDate()) + " (" + ccy + ")";

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("Stock Movement");
            ExcelStyles st = new ExcelStyles(wb);

            renderTopHeader(sheet, st, company, "Stock Movement Ledger", subtitle);

            int rowIdx = 4;
            String[] headers = new String[]{"Date", "Movement Type", "Reference", "Warehouse", "Qty In", "Qty Out", "Running Balance", "Unit Cost (" + ccy + ")"};
            HorizontalAlignment[] aligns = new HorizontalAlignment[]{HorizontalAlignment.CENTER, HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT};
            renderTableHeaders(sheet, rowIdx++, st, headers, aligns);

            // Opening Stock Row
            Row rOpen = sheet.createRow(rowIdx++);
            rOpen.createCell(0).setCellValue(fmtDate(sm.fromDate()));
            rOpen.getCell(0).setCellStyle(st.date);
            rOpen.createCell(1).setCellValue("OPENING");
            rOpen.getCell(1).setCellStyle(st.textBold);
            rOpen.createCell(2).setCellValue("-");
            rOpen.getCell(2).setCellStyle(st.text);
            rOpen.createCell(3).setCellValue("Opening Stock");
            rOpen.getCell(3).setCellStyle(st.text);
            rOpen.createCell(4).setCellValue(0.0);
            rOpen.getCell(4).setCellStyle(st.number);
            rOpen.createCell(5).setCellValue(0.0);
            rOpen.getCell(5).setCellStyle(st.number);
            Cell cOpenQty = rOpen.createCell(6);
            cOpenQty.setCellValue(toDouble(sm.openingQty()));
            cOpenQty.setCellStyle(st.numberBold);
            rOpen.createCell(7).setCellValue(0.0);
            rOpen.getCell(7).setCellStyle(st.number);

            for (StockMovementLineDto m : safeList(sm.movements())) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(fmtDate(m.date()));
                r.getCell(0).setCellStyle(st.date);
                r.createCell(1).setCellValue(safe(m.movementType()));
                r.getCell(1).setCellStyle(st.textBold);
                r.createCell(2).setCellValue(safe(m.reference()));
                r.getCell(2).setCellStyle(st.code);
                r.createCell(3).setCellValue(safe(m.warehouseName()));
                r.getCell(3).setCellStyle(st.text);

                Cell cIn = r.createCell(4);
                cIn.setCellValue(toDouble(m.qtyIn() != null ? m.qtyIn().abs() : null));
                cIn.setCellStyle(st.number);

                Cell cOut = r.createCell(5);
                cOut.setCellValue(toDouble(m.qtyOut() != null ? m.qtyOut().abs() : null));
                cOut.setCellStyle(st.number);

                Cell cBal = r.createCell(6);
                cBal.setCellValue(toDouble(m.runningBalance()));
                cBal.setCellStyle(st.numberBold);

                Cell cCost = r.createCell(7);
                cCost.setCellValue(toDouble(m.unitCost()));
                cCost.setCellStyle(st.number);
            }

            Row rClose = sheet.createRow(rowIdx++);
            rClose.createCell(0).setCellValue("");
            rClose.createCell(1).setCellValue("CLOSING STOCK QUANTITY");
            rClose.getCell(1).setCellStyle(st.grandTotalLabel);
            for (int col = 2; col <= 5; col++) {
                rClose.createCell(col).setCellValue("");
                rClose.getCell(col).setCellStyle(st.grandTotalLabel);
            }
            Cell cClose = rClose.createCell(6);
            cClose.setCellValue(toDouble(sm.closingQty()));
            cClose.setCellStyle(st.grandTotalNumber);
            rClose.createCell(7).setCellValue("");
            rClose.getCell(7).setCellStyle(st.grandTotalLabel);

            finalizeSheet(sheet, 8);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("EXPORT_ERROR", "Failed to render Stock Movement Excel report: " + e.getMessage());
        }
    }

    // ─── 10. Pipeline Forecast ──────────────────────────────────────────────

    public static byte[] renderPipelineForecast(PipelineForecastReportDto pf, Company company) {
        String ccy = currency(company);
        String subtitle = "Period: " + fmtDate(pf.fromDate()) + " to " + fmtDate(pf.toDate()) + " (" + ccy + ")";

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("Pipeline Forecast");
            ExcelStyles st = new ExcelStyles(wb);

            renderTopHeader(sheet, st, company, "CRM Sales Pipeline Forecast", subtitle);

            int rowIdx = 4;
            String[] headers = new String[]{"Stage", "Deals", "Probability", "Total Value (" + ccy + ")", "Weighted Forecast (" + ccy + ")"};
            HorizontalAlignment[] aligns = new HorizontalAlignment[]{HorizontalAlignment.LEFT, HorizontalAlignment.CENTER, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT};
            renderTableHeaders(sheet, rowIdx++, st, headers, aligns);

            int startRow = rowIdx;
            for (PipelineStageSummaryDto s : safeList(pf.stages())) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(safe(s.stageName()));
                r.getCell(0).setCellStyle(st.textBold);

                Cell cDeals = r.createCell(1);
                cDeals.setCellValue(s.dealCount());
                cDeals.setCellStyle(st.integer);

                Cell cProb = r.createCell(2);
                cProb.setCellValue(s.probability() != null ? s.probability() / 100.0 : 0.0);
                cProb.setCellStyle(st.percent);

                Cell cVal = r.createCell(3);
                cVal.setCellValue(toDouble(s.totalValue()));
                cVal.setCellStyle(st.number);

                Cell cWVal = r.createCell(4);
                cWVal.setCellValue(toDouble(s.weightedValue()));
                cWVal.setCellStyle(st.numberBold);
            }
            int endRow = rowIdx - 1;

            Row rTot = sheet.createRow(rowIdx++);
            rTot.createCell(0).setCellValue("TOTAL PIPELINE");
            rTot.getCell(0).setCellStyle(st.grandTotalLabel);

            Cell cTotDeals = rTot.createCell(1);
            cTotDeals.setCellFormula(sumFormula(1, startRow, endRow));
            cTotDeals.setCellStyle(st.integerBold);

            rTot.createCell(2).setCellValue("");
            rTot.getCell(2).setCellStyle(st.grandTotalLabel);

            Cell cTotVal = rTot.createCell(3);
            cTotVal.setCellFormula(sumFormula(3, startRow, endRow));
            cTotVal.setCellStyle(st.grandTotalNumber);

            Cell cTotWVal = rTot.createCell(4);
            cTotWVal.setCellFormula(sumFormula(4, startRow, endRow));
            cTotWVal.setCellStyle(st.grandTotalNumber);

            finalizeSheet(sheet, 5);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("EXPORT_ERROR", "Failed to render Pipeline Forecast Excel report: " + e.getMessage());
        }
    }

    // ─── 11. Expense Analysis ───────────────────────────────────────────────

    public static byte[] renderExpenseAnalysis(ExpenseAnalysisReportDto ea, Company company) {
        String ccy = currency(company);
        String groupLabel = ea.groupBy() != null ? ea.groupBy().toUpperCase() : "CATEGORY";
        String subtitle = "Grouped by: " + groupLabel + "  •  Period: " + fmtDate(ea.fromDate()) + " to " + fmtDate(ea.toDate()) + " (" + ccy + ")";

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("Expense Analysis");
            ExcelStyles st = new ExcelStyles(wb);

            renderTopHeader(sheet, st, company, "Operational Expense Analysis", subtitle);

            int rowIdx = 4;
            String[] headers = new String[]{"Category / Entity", "Claims", "Reimbursable (" + ccy + ")", "Total Amount (" + ccy + ")", "% of Total"};
            HorizontalAlignment[] aligns = new HorizontalAlignment[]{HorizontalAlignment.LEFT, HorizontalAlignment.CENTER, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT};
            renderTableHeaders(sheet, rowIdx++, st, headers, aligns);

            int startRow = rowIdx;
            for (ExpenseAnalysisGroupDto g : safeList(ea.groups())) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(safe(g.groupName()));
                r.getCell(0).setCellStyle(st.textBold);

                Cell cClaims = r.createCell(1);
                cClaims.setCellValue(g.claimCount());
                cClaims.setCellStyle(st.integer);

                Cell cReimb = r.createCell(2);
                cReimb.setCellValue(toDouble(g.reimbursableAmount()));
                cReimb.setCellStyle(st.number);

                Cell cTot = r.createCell(3);
                cTot.setCellValue(toDouble(g.totalAmount()));
                cTot.setCellStyle(st.numberBold);

                Cell cPct = r.createCell(4);
                cPct.setCellValue(g.percentageOfTotal() != null ? g.percentageOfTotal() / 100.0 : 0.0);
                cPct.setCellStyle(st.percent);
            }
            int endRow = rowIdx - 1;

            Row rTot = sheet.createRow(rowIdx++);
            rTot.createCell(0).setCellValue("GRAND TOTAL OPERATING EXPENSES");
            rTot.getCell(0).setCellStyle(st.grandTotalLabel);

            Cell cTotClaims = rTot.createCell(1);
            cTotClaims.setCellFormula(sumFormula(1, startRow, endRow));
            cTotClaims.setCellStyle(st.integerBold);

            Cell cTotReimb = rTot.createCell(2);
            cTotReimb.setCellFormula(sumFormula(2, startRow, endRow));
            cTotReimb.setCellStyle(st.grandTotalNumber);

            Cell cTotAmt = rTot.createCell(3);
            cTotAmt.setCellFormula(sumFormula(3, startRow, endRow));
            cTotAmt.setCellStyle(st.grandTotalNumber);

            Cell cTotPct = rTot.createCell(4);
            cTotPct.setCellFormula(sumFormula(4, startRow, endRow));
            cTotPct.setCellStyle(st.percentBold);

            finalizeSheet(sheet, 5);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("EXPORT_ERROR", "Failed to render Expense Analysis Excel report: " + e.getMessage());
        }
    }

    // ─── Helpers & Style Factory ────────────────────────────────────────────

    private static void renderTopHeader(Sheet sheet, ExcelStyles st, Company company, String reportTitle, String subtitle) {
        String companyName = company != null && company.getLegalName() != null ? company.getLegalName() : "FinLedger ERP";
        Row r0 = sheet.createRow(0);
        Cell c0 = r0.createCell(0);
        c0.setCellValue(companyName);
        c0.setCellStyle(st.companyTitle);

        Row r1 = sheet.createRow(1);
        Cell c1 = r1.createCell(0);
        c1.setCellValue(reportTitle);
        c1.setCellStyle(st.reportTitle);

        Row r2 = sheet.createRow(2);
        Cell c2 = r2.createCell(0);
        c2.setCellValue(subtitle);
        c2.setCellStyle(st.subtitle);

        sheet.createRow(3); // Blank line 4
    }

    private static void renderTableHeaders(Sheet sheet, int rowIdx, ExcelStyles st, String[] headers, HorizontalAlignment[] aligns) {
        Row row = sheet.createRow(rowIdx);
        row.setHeightInPoints(24);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            HorizontalAlignment align = (aligns != null && i < aligns.length) ? aligns[i] : HorizontalAlignment.LEFT;
            if (align == HorizontalAlignment.RIGHT) {
                cell.setCellStyle(st.thRight);
            } else if (align == HorizontalAlignment.CENTER) {
                cell.setCellStyle(st.thCenter);
            } else {
                cell.setCellStyle(st.thLeft);
            }
        }
    }

    private static void renderSectionHeader(Sheet sheet, int rowIdx, ExcelStyles st, String title, int numCols) {
        Row r = sheet.createRow(rowIdx);
        r.setHeightInPoints(20);
        for (int i = 0; i < numCols; i++) {
            Cell cell = r.createCell(i);
            if (i == 0) {
                cell.setCellValue(title);
            }
            cell.setCellStyle(st.sectionHeader);
        }
    }

    private static void finalizeSheet(Sheet sheet, int numCols) {
        sheet.createFreezePane(0, 5); // Freeze top header block + table header row
        for (int i = 0; i < numCols; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(Math.max(width + 1400, 3200), 22000));
        }
    }

    private static String colLetter(int colIndex) {
        StringBuilder sb = new StringBuilder();
        int col = colIndex;
        while (col >= 0) {
            sb.insert(0, (char) ('A' + (col % 26)));
            col = (col / 26) - 1;
        }
        return sb.toString();
    }

    private static String cellRef(int colIndex, int rowIndex) {
        return colLetter(colIndex) + (rowIndex + 1);
    }

    private static String sumFormula(int colIndex, int startRow, int endRow) {
        if (startRow > endRow) return "0";
        return "SUM(" + cellRef(colIndex, startRow) + ":" + cellRef(colIndex, endRow) + ")";
    }

    private static String currency(Company company) {
        return company != null && company.getBaseCurrency() != null ? company.getBaseCurrency() : "AED";
    }

    private static String safe(String str) {
        return str != null ? str : "-";
    }

    private static String fmtDate(LocalDate date) {
        return date != null ? date.format(DATE_FMT) : "-";
    }

    private static double toDouble(BigDecimal val) {
        return val != null ? val.doubleValue() : 0.0;
    }

    private static <T> List<T> safeList(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }

    // ─── Style Definitions Class ───────────────────────────────────────────

    private static final class ExcelStyles {
        final CellStyle companyTitle;
        final CellStyle reportTitle;
        final CellStyle subtitle;
        final CellStyle thLeft;
        final CellStyle thRight;
        final CellStyle thCenter;
        final CellStyle sectionHeader;
        final CellStyle text;
        final CellStyle textIndent;
        final CellStyle textBold;
        final CellStyle code;
        final CellStyle number;
        final CellStyle numberBold;
        final CellStyle subtotalLabel;
        final CellStyle subtotalNumber;
        final CellStyle majorLabel;
        final CellStyle majorNumber;
        final CellStyle grandTotalLabel;
        final CellStyle grandTotalNumber;
        final CellStyle percent;
        final CellStyle percentBold;
        final CellStyle integer;
        final CellStyle integerBold;
        final CellStyle date;

        ExcelStyles(XSSFWorkbook wb) {
            DataFormat df = wb.createDataFormat();
            short numFmt = df.getFormat("#,##0.00;(#,##0.00);\"-\"");
            short pctFmt = df.getFormat("0.0%");
            short intFmt = df.getFormat("#,##0");

            DefaultIndexedColorMap colorMap = new DefaultIndexedColorMap();

            XSSFColor colTitle = new XSSFColor(new Color(15, 23, 42), colorMap);
            XSSFColor colSubtitle = new XSSFColor(new Color(100, 116, 139), colorMap);
            XSSFColor colThBg = new XSSFColor(new Color(248, 250, 252), colorMap);
            XSSFColor colSectionBg = new XSSFColor(new Color(241, 245, 249), colorMap);
            XSSFColor colBrandBlue = new XSSFColor(new Color(37, 99, 235), colorMap);

            XSSFFont fCompany = wb.createFont();
            fCompany.setFontName("Calibri");
            fCompany.setFontHeightInPoints((short) 14);
            fCompany.setBold(true);
            fCompany.setColor(colTitle);

            XSSFFont fReport = wb.createFont();
            fReport.setFontName("Calibri");
            fReport.setFontHeightInPoints((short) 12);
            fReport.setBold(true);
            fReport.setColor(colTitle);

            XSSFFont fSub = wb.createFont();
            fSub.setFontName("Calibri");
            fSub.setFontHeightInPoints((short) 10);
            fSub.setColor(colSubtitle);

            XSSFFont fTh = wb.createFont();
            fTh.setFontName("Calibri");
            fTh.setFontHeightInPoints((short) 10);
            fTh.setBold(true);
            fTh.setColor(colTitle);

            XSSFFont fBody = wb.createFont();
            fBody.setFontName("Calibri");
            fBody.setFontHeightInPoints((short) 10);

            XSSFFont fBodyBold = wb.createFont();
            fBodyBold.setFontName("Calibri");
            fBodyBold.setFontHeightInPoints((short) 10);
            fBodyBold.setBold(true);

            XSSFFont fCode = wb.createFont();
            fCode.setFontName("Consolas");
            fCode.setFontHeightInPoints((short) 10);
            fCode.setBold(true);
            fCode.setColor(colBrandBlue);

            companyTitle = wb.createCellStyle();
            companyTitle.setFont(fCompany);

            reportTitle = wb.createCellStyle();
            reportTitle.setFont(fReport);

            subtitle = wb.createCellStyle();
            subtitle.setFont(fSub);

            thLeft = wb.createCellStyle();
            thLeft.setFont(fTh);
            thLeft.setFillForegroundColor(colThBg);
            thLeft.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            thLeft.setBorderTop(BorderStyle.THIN);
            thLeft.setBorderBottom(BorderStyle.MEDIUM);
            thLeft.setAlignment(HorizontalAlignment.LEFT);
            thLeft.setVerticalAlignment(VerticalAlignment.CENTER);

            thRight = wb.createCellStyle();
            thRight.cloneStyleFrom(thLeft);
            thRight.setAlignment(HorizontalAlignment.RIGHT);

            thCenter = wb.createCellStyle();
            thCenter.cloneStyleFrom(thLeft);
            thCenter.setAlignment(HorizontalAlignment.CENTER);

            sectionHeader = wb.createCellStyle();
            sectionHeader.setFont(fBodyBold);
            sectionHeader.setFillForegroundColor(colSectionBg);
            sectionHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            sectionHeader.setBorderTop(BorderStyle.THIN);
            sectionHeader.setBorderBottom(BorderStyle.THIN);
            sectionHeader.setAlignment(HorizontalAlignment.LEFT);
            sectionHeader.setVerticalAlignment(VerticalAlignment.CENTER);

            text = wb.createCellStyle();
            text.setFont(fBody);
            text.setAlignment(HorizontalAlignment.LEFT);
            text.setVerticalAlignment(VerticalAlignment.CENTER);

            textIndent = wb.createCellStyle();
            textIndent.setFont(fBody);
            textIndent.setIndention((short) 1);
            textIndent.setAlignment(HorizontalAlignment.LEFT);
            textIndent.setVerticalAlignment(VerticalAlignment.CENTER);

            textBold = wb.createCellStyle();
            textBold.setFont(fBodyBold);
            textBold.setAlignment(HorizontalAlignment.LEFT);
            textBold.setVerticalAlignment(VerticalAlignment.CENTER);

            code = wb.createCellStyle();
            code.setFont(fCode);
            code.setAlignment(HorizontalAlignment.LEFT);
            code.setVerticalAlignment(VerticalAlignment.CENTER);

            date = wb.createCellStyle();
            date.setFont(fBody);
            date.setAlignment(HorizontalAlignment.CENTER);
            date.setVerticalAlignment(VerticalAlignment.CENTER);

            number = wb.createCellStyle();
            number.setFont(fBody);
            number.setDataFormat(numFmt);
            number.setAlignment(HorizontalAlignment.RIGHT);
            number.setVerticalAlignment(VerticalAlignment.CENTER);

            numberBold = wb.createCellStyle();
            numberBold.setFont(fBodyBold);
            numberBold.setDataFormat(numFmt);
            numberBold.setAlignment(HorizontalAlignment.RIGHT);
            numberBold.setVerticalAlignment(VerticalAlignment.CENTER);

            subtotalLabel = wb.createCellStyle();
            subtotalLabel.setFont(fBodyBold);
            subtotalLabel.setBorderTop(BorderStyle.THIN);
            subtotalLabel.setBorderBottom(BorderStyle.THIN);
            subtotalLabel.setAlignment(HorizontalAlignment.LEFT);
            subtotalLabel.setVerticalAlignment(VerticalAlignment.CENTER);

            subtotalNumber = wb.createCellStyle();
            subtotalNumber.setFont(fBodyBold);
            subtotalNumber.setDataFormat(numFmt);
            subtotalNumber.setBorderTop(BorderStyle.THIN);
            subtotalNumber.setBorderBottom(BorderStyle.THIN);
            subtotalNumber.setAlignment(HorizontalAlignment.RIGHT);
            subtotalNumber.setVerticalAlignment(VerticalAlignment.CENTER);

            majorLabel = wb.createCellStyle();
            majorLabel.setFont(fBodyBold);
            majorLabel.setFillForegroundColor(colSectionBg);
            majorLabel.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            majorLabel.setBorderTop(BorderStyle.THIN);
            majorLabel.setBorderBottom(BorderStyle.THIN);
            majorLabel.setAlignment(HorizontalAlignment.LEFT);
            majorLabel.setVerticalAlignment(VerticalAlignment.CENTER);

            majorNumber = wb.createCellStyle();
            majorNumber.setFont(fBodyBold);
            majorNumber.setFillForegroundColor(colSectionBg);
            majorNumber.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            majorNumber.setDataFormat(numFmt);
            majorNumber.setBorderTop(BorderStyle.THIN);
            majorNumber.setBorderBottom(BorderStyle.THIN);
            majorNumber.setAlignment(HorizontalAlignment.RIGHT);
            majorNumber.setVerticalAlignment(VerticalAlignment.CENTER);

            grandTotalLabel = wb.createCellStyle();
            grandTotalLabel.setFont(fBodyBold);
            grandTotalLabel.setBorderTop(BorderStyle.THIN);
            grandTotalLabel.setBorderBottom(BorderStyle.DOUBLE);
            grandTotalLabel.setAlignment(HorizontalAlignment.LEFT);
            grandTotalLabel.setVerticalAlignment(VerticalAlignment.CENTER);

            grandTotalNumber = wb.createCellStyle();
            grandTotalNumber.setFont(fBodyBold);
            grandTotalNumber.setDataFormat(numFmt);
            grandTotalNumber.setBorderTop(BorderStyle.THIN);
            grandTotalNumber.setBorderBottom(BorderStyle.DOUBLE);
            grandTotalNumber.setAlignment(HorizontalAlignment.RIGHT);
            grandTotalNumber.setVerticalAlignment(VerticalAlignment.CENTER);

            percent = wb.createCellStyle();
            percent.setFont(fBody);
            percent.setDataFormat(pctFmt);
            percent.setAlignment(HorizontalAlignment.RIGHT);
            percent.setVerticalAlignment(VerticalAlignment.CENTER);

            percentBold = wb.createCellStyle();
            percentBold.setFont(fBodyBold);
            percentBold.setDataFormat(pctFmt);
            percentBold.setAlignment(HorizontalAlignment.RIGHT);
            percentBold.setVerticalAlignment(VerticalAlignment.CENTER);

            integer = wb.createCellStyle();
            integer.setFont(fBody);
            integer.setDataFormat(intFmt);
            integer.setAlignment(HorizontalAlignment.CENTER);
            integer.setVerticalAlignment(VerticalAlignment.CENTER);

            integerBold = wb.createCellStyle();
            integerBold.setFont(fBodyBold);
            integerBold.setDataFormat(intFmt);
            integerBold.setAlignment(HorizontalAlignment.CENTER);
            integerBold.setVerticalAlignment(VerticalAlignment.CENTER);
        }
    }
}
