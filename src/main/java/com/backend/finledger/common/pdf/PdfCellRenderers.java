package com.backend.finledger.common.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.time.LocalDate;

/**
 * Custom OpenPDF cell events and page footer helper matching FinLedger's web styling.
 */
public final class PdfCellRenderers {

    private PdfCellRenderers() {}

    /**
     * Draws a rounded rectangle card with optional fill and border (mirrors .form-card / .gl-card / .totals-card).
     */
    public static class RoundedBoxCellEvent implements PdfPCellEvent {
        private final Color fillColor;
        private final Color borderColor;
        private final float radius;
        private final float lineWidth;

        public RoundedBoxCellEvent(Color fillColor, Color borderColor, float radius, float lineWidth) {
            this.fillColor = fillColor;
            this.borderColor = borderColor;
            this.radius = radius;
            this.lineWidth = lineWidth;
        }

        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            PdfContentByte cb = canvases[PdfPTable.BACKGROUNDCANVAS];
            cb.saveState();
            if (fillColor != null) {
                cb.setColorFill(fillColor);
                cb.roundRectangle(position.getLeft(), position.getBottom(),
                        position.getWidth(), position.getHeight(), radius);
                cb.fill();
            }
            if (borderColor != null) {
                cb.setColorStroke(borderColor);
                cb.setLineWidth(lineWidth);
                cb.roundRectangle(position.getLeft(), position.getBottom(),
                        position.getWidth(), position.getHeight(), radius);
                cb.stroke();
            }
            cb.restoreState();
        }
    }

    /**
     * Draws a top dark border and a crisp double-line bottom border (mirrors .grandtotal-row td).
     */
    public static class DoubleBottomBorderCellEvent implements PdfPCellEvent {
        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            PdfContentByte cb = canvases[PdfPTable.LINECANVAS];
            cb.saveState();
            cb.setColorStroke(FinLedgerPdfTheme.TEXT_PRIMARY);

            // Top solid 1.5pt line
            cb.setLineWidth(1.5f);
            cb.moveTo(position.getLeft(), position.getTop());
            cb.lineTo(position.getRight(), position.getTop());
            cb.stroke();

            // Double bottom lines (1pt each separated by 2pt)
            cb.setLineWidth(1.0f);
            cb.moveTo(position.getLeft(), position.getBottom() + 2.5f);
            cb.lineTo(position.getRight(), position.getBottom() + 2.5f);
            cb.stroke();

            cb.moveTo(position.getLeft(), position.getBottom());
            cb.lineTo(position.getRight(), position.getBottom());
            cb.stroke();

            cb.restoreState();
        }
    }

    /**
     * Draws a pill-shaped badge cell (mirrors .status-pill / .type-pill).
     */
    public static PdfPCell createPillCell(String text, Color bg, Color fg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, fg)));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(3f);
        cell.setPaddingBottom(3.5f);
        cell.setPaddingLeft(8f);
        cell.setPaddingRight(8f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setCellEvent(new RoundedBoxCellEvent(bg, null, 7f, 0f));
        return cell;
    }

    /**
     * Consistent footer across all generated FinLedger PDF pages.
     */
    public static class FinLedgerPageFooterEvent extends PdfPageEventHelper {
        private final String companyName;
        private final String docLabel;

        public FinLedgerPageFooterEvent(String companyName, String docLabel) {
            this.companyName = companyName != null && !companyName.isBlank() ? companyName : "FinLedger ERP";
            this.docLabel = docLabel != null ? docLabel : "";
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float left = document.leftMargin();
            float right = document.getPageSize().getWidth() - document.rightMargin();
            float yLine = document.bottomMargin() - 10f;
            float yText = document.bottomMargin() - 21f;

            cb.saveState();
            cb.setColorStroke(FinLedgerPdfTheme.BORDER_LIGHT);
            cb.setLineWidth(0.75f);
            cb.moveTo(left, yLine);
            cb.lineTo(right, yLine);
            cb.stroke();
            cb.restoreState();

            String leftText = companyName + (docLabel.isBlank() ? "" : "  •  " + docLabel)
                    + "  •  Generated " + LocalDate.now();
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(leftText, FinLedgerPdfTheme.FONT_FOOTER), left, yText, 0);

            String rightText = "Page " + writer.getPageNumber();
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase(rightText, FinLedgerPdfTheme.FONT_FOOTER), right, yText, 0);
        }
    }
}
