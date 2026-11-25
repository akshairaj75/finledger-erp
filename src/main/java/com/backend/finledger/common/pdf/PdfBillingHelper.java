package com.backend.finledger.common.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import java.util.List;

/**
 * Shared OpenPDF layout components for FinLedger billing documents (Tax Invoices, Credit Notes, Quotations).
 */
public final class PdfBillingHelper {

    private PdfBillingHelper() {}

    public record TotalsRow(String label, String value, boolean emphasized, boolean dividerAbove) {}

    public static void addTwoColumnMetaCards(
            Document document,
            String leftTitle,
            List<String[]> leftRows,
            String rightTitle,
            List<String[]> rightRows
    ) throws DocumentException {
        PdfPTable outer = new PdfPTable(2);
        outer.setWidthPercentage(100);
        outer.setWidths(new float[]{50f, 50f});
        outer.setSpacingAfter(14f);

        outer.addCell(buildMetaCardCell(leftTitle, leftRows, 0f, 6f));
        outer.addCell(buildMetaCardCell(rightTitle, rightRows, 6f, 0f));

        document.add(outer);
    }

    private static PdfPCell buildMetaCardCell(String title, List<String[]> rows, float padLeft, float padRight) {
        PdfPCell wrapper = new PdfPCell();
        wrapper.setBorder(Rectangle.NO_BORDER);
        wrapper.setPaddingTop(0f);
        wrapper.setPaddingBottom(0f);
        wrapper.setPaddingLeft(padLeft);
        wrapper.setPaddingRight(padRight);

        PdfPTable card = new PdfPTable(1);
        card.setWidthPercentage(100);
        PdfPCell box = new PdfPCell();
        box.setBorder(Rectangle.NO_BORDER);
        box.setPadding(10f);
        box.setCellEvent(new PdfCellRenderers.RoundedBoxCellEvent(
                FinLedgerPdfTheme.SURFACE_LIGHT, FinLedgerPdfTheme.BORDER_LIGHT, 5f, 0.85f));

        Paragraph hdr = new Paragraph(title, FinLedgerPdfTheme.FONT_CARD_LABEL);
        hdr.setSpacingAfter(5f);
        box.addElement(hdr);

        for (String[] pair : rows) {
            if (pair == null || pair.length == 0) continue;
            if (pair.length == 1) {
                Paragraph p = new Paragraph(pair[0], FinLedgerPdfTheme.FONT_BODY);
                p.setLeading(12f);
                box.addElement(p);
            } else {
                String label = pair[0];
                String value = pair[1];
                if (value == null || value.isBlank()) continue;
                Phrase phrase = new Phrase();
                if (label != null && !label.isBlank()) {
                    phrase.add(new com.lowagie.text.Chunk(label + ": ", FinLedgerPdfTheme.FONT_META));
                    phrase.add(new com.lowagie.text.Chunk(value, FinLedgerPdfTheme.FONT_BODY_BOLD));
                } else {
                    phrase.add(new com.lowagie.text.Chunk(value, FinLedgerPdfTheme.FONT_BODY_BOLD));
                }
                Paragraph p = new Paragraph(phrase);
                p.setLeading(12.5f);
                box.addElement(p);
            }
        }

        card.addCell(box);
        wrapper.addElement(card);
        return wrapper;
    }

    public static void addLineTableHeaders(PdfPTable table, String[] headers, int[] alignments) {
        for (int i = 0; i < headers.length; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(headers[i], FinLedgerPdfTheme.FONT_TH));
            cell.setBackgroundColor(FinLedgerPdfTheme.SURFACE_LIGHT);
            cell.setBorder(Rectangle.BOTTOM);
            cell.setBorderColorBottom(FinLedgerPdfTheme.BORDER_MEDIUM);
            cell.setBorderWidthBottom(1.5f);
            cell.setPaddingTop(7f);
            cell.setPaddingBottom(7f);
            cell.setPaddingLeft(6f);
            cell.setPaddingRight(6f);
            cell.setHorizontalAlignment(alignments[i]);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }
    }

    public static PdfPCell lineBodyCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", font));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColorBottom(FinLedgerPdfTheme.SURFACE_SHADED);
        cell.setBorderWidthBottom(0.75f);
        cell.setPaddingTop(6f);
        cell.setPaddingBottom(6f);
        cell.setPaddingLeft(6f);
        cell.setPaddingRight(6f);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    public static void addTotalsCard(Document document, List<TotalsRow> rows) throws DocumentException {
        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(44);
        outer.setHorizontalAlignment(Element.ALIGN_RIGHT);
        outer.setSpacingBefore(12f);

        PdfPCell cardCell = new PdfPCell();
        cardCell.setBorder(Rectangle.NO_BORDER);
        cardCell.setPadding(10f);
        cardCell.setCellEvent(new PdfCellRenderers.RoundedBoxCellEvent(
                FinLedgerPdfTheme.SURFACE_LIGHT, FinLedgerPdfTheme.BORDER_LIGHT, 5f, 0.85f));

        PdfPTable inner = new PdfPTable(2);
        inner.setWidthPercentage(100);
        inner.setWidths(new float[]{52f, 48f});

        for (TotalsRow row : rows) {
            Font font = row.emphasized() ? FinLedgerPdfTheme.FONT_MAJOR : FinLedgerPdfTheme.FONT_BODY;
            int border = row.dividerAbove() ? Rectangle.TOP : Rectangle.NO_BORDER;

            PdfPCell lbl = new PdfPCell(new Phrase(row.label(), font));
            lbl.setBorder(border);
            lbl.setBorderColorTop(FinLedgerPdfTheme.BORDER_MEDIUM);
            lbl.setBorderWidthTop(1f);
            lbl.setPaddingTop(row.dividerAbove() ? 6f : 3.5f);
            lbl.setPaddingBottom(3.5f);
            lbl.setPaddingLeft(2f);
            lbl.setPaddingRight(4f);
            inner.addCell(lbl);

            PdfPCell val = new PdfPCell(new Phrase(row.value(), font));
            val.setBorder(border);
            val.setBorderColorTop(FinLedgerPdfTheme.BORDER_MEDIUM);
            val.setBorderWidthTop(1f);
            val.setHorizontalAlignment(Element.ALIGN_RIGHT);
            val.setPaddingTop(row.dividerAbove() ? 6f : 3.5f);
            val.setPaddingBottom(3.5f);
            val.setPaddingLeft(4f);
            val.setPaddingRight(2f);
            inner.addCell(val);
        }

        cardCell.addElement(inner);
        outer.addCell(cardCell);
        document.add(outer);
    }

    public static void addNotesBox(Document document, String title, String notes) throws DocumentException {
        if (notes == null || notes.isBlank()) return;
        PdfPTable wrap = new PdfPTable(1);
        wrap.setWidthPercentage(100);
        wrap.setSpacingBefore(14f);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(10f);
        cell.setCellEvent(new PdfCellRenderers.RoundedBoxCellEvent(
                FinLedgerPdfTheme.SURFACE_LIGHT, FinLedgerPdfTheme.BORDER_LIGHT, 5f, 0.85f));

        Paragraph h = new Paragraph(title, FinLedgerPdfTheme.FONT_CARD_LABEL);
        h.setSpacingAfter(3f);
        cell.addElement(h);
        cell.addElement(new Paragraph(notes, FinLedgerPdfTheme.FONT_BODY));

        wrap.addCell(cell);
        document.add(wrap);
    }
}
