package com.backend.finledger.common.pdf;

import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Shared design tokens, typography, and value formatters matching FinLedger's web UI palette.
 */
public final class FinLedgerPdfTheme {

    private FinLedgerPdfTheme() {}

    // Core Slate & Brand Palette (mirrors Angular CSS)
    public static final Color TEXT_PRIMARY   = new Color(15, 23, 42);    // #0F172A
    public static final Color TEXT_HEADING   = new Color(30, 41, 59);    // #1E293B
    public static final Color TEXT_BODY      = new Color(51, 65, 85);    // #334155
    public static final Color TEXT_LABEL     = new Color(71, 85, 105);   // #475569
    public static final Color TEXT_MUTED     = new Color(100, 116, 139); // #64748B
    public static final Color TEXT_SUBTLE    = new Color(148, 163, 184); // #94A3B8

    public static final Color BRAND_BLUE     = new Color(37, 99, 235);   // #2563EB
    public static final Color SURFACE_LIGHT  = new Color(248, 250, 252); // #F8FAFC
    public static final Color SURFACE_SHADED = new Color(241, 245, 249); // #F1F5F9
    public static final Color BORDER_LIGHT   = new Color(226, 232, 240); // #E2E8F0
    public static final Color BORDER_MEDIUM  = new Color(203, 213, 225); // #CBD5E1
    public static final Color BORDER_DARK    = new Color(148, 163, 184); // #94A3B8

    // Status Badge Colors
    public static final Color SUCCESS_BG     = new Color(220, 252, 231); // #DCFCE7
    public static final Color SUCCESS_TEXT   = new Color(22, 101, 52);   // #166534
    public static final Color SUCCESS_BORDER = new Color(187, 247, 208); // #BBF7D0

    public static final Color DANGER_BG      = new Color(254, 226, 226); // #FEE2E2
    public static final Color DANGER_TEXT    = new Color(153, 27, 27);   // #991B1B
    public static final Color DANGER_BORDER  = new Color(254, 202, 202); // #FECACA

    public static final Color WARNING_BG     = new Color(254, 243, 199); // #FEF3C7
    public static final Color WARNING_TEXT   = new Color(146, 64, 14);   // #92400E

    public static final Color INFO_BG        = new Color(219, 234, 254); // #DBEAFE
    public static final Color INFO_TEXT      = new Color(30, 64, 175);   // #1E40AF

    public static final Color INDIGO_BG      = new Color(224, 231, 255); // #E0E7FF
    public static final Color INDIGO_TEXT    = new Color(55, 48, 163);   // #3730A3

    // Typography Presets
    public static final Font FONT_DOC_TITLE   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, TEXT_PRIMARY);
    public static final Font FONT_DOC_SUB     = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_MUTED);
    public static final Font FONT_COMPANY     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, TEXT_PRIMARY);
    public static final Font FONT_META        = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, TEXT_LABEL);
    public static final Font FONT_META_BOLD   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, TEXT_PRIMARY);
    public static final Font FONT_CARD_LABEL  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, TEXT_MUTED);
    public static final Font FONT_CARD_VAL    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, TEXT_PRIMARY);

    public static final Font FONT_TH          = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, TEXT_LABEL);
    public static final Font FONT_SECTION     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, TEXT_HEADING);
    public static final Font FONT_BODY        = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, TEXT_BODY);
    public static final Font FONT_BODY_BOLD   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, TEXT_PRIMARY);
    public static final Font FONT_BODY_ITALIC = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8.5f, TEXT_LABEL);
    public static final Font FONT_CODE_BLUE   = FontFactory.getFont(FontFactory.COURIER_BOLD, 8.5f, BRAND_BLUE);
    public static final Font FONT_CODE_DARK   = FontFactory.getFont(FontFactory.COURIER_BOLD, 8.5f, TEXT_HEADING);
    public static final Font FONT_SUBTOTAL    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, TEXT_PRIMARY);
    public static final Font FONT_MAJOR       = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, TEXT_PRIMARY);
    public static final Font FONT_GRAND_TOTAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, TEXT_PRIMARY);
    public static final Font FONT_FOOTER      = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, TEXT_MUTED);

    public static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

    public static String fmtMoney(BigDecimal val) {
        if (val == null) return "0.00";
        DecimalFormat df = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return df.format(val.setScale(2, RoundingMode.HALF_UP));
    }

    public static String fmtMoneyOrDash(BigDecimal val) {
        if (val == null || val.compareTo(BigDecimal.ZERO) == 0) return "-";
        return fmtMoney(val);
    }

    public static String fmtQty(BigDecimal val) {
        if (val == null) return "0";
        BigDecimal stripped = val.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0, RoundingMode.UNNECESSARY).toPlainString() : stripped.toPlainString();
    }

    public static String fmtPct(Double pct) {
        if (pct == null) return "-";
        DecimalFormat df = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.US));
        return df.format(pct) + "%";
    }

    public static String fmtDate(LocalDate date) {
        return date != null ? date.format(DISPLAY_DATE) : "-";
    }

    public static String safe(String raw) {
        return (raw == null || raw.isBlank()) ? "-" : raw;
    }

    /**
     * Resolves status badge background and foreground colors matching the Angular UI pills.
     */
    public static Color[] statusColors(String status) {
        if (status == null) return new Color[]{SURFACE_SHADED, TEXT_LABEL};
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "PAID", "ACCEPTED", "POSTED", "OPEN", "ACTIVE" -> new Color[]{SUCCESS_BG, SUCCESS_TEXT};
            case "VOID", "REJECTED", "OVERDUE", "EXPIRED", "CANCELLED" -> new Color[]{DANGER_BG, DANGER_TEXT};
            case "PARTIALLY_PAID", "PENDING", "CLOSED" -> new Color[]{WARNING_BG, WARNING_TEXT};
            case "SENT", "INVOICED" -> new Color[]{INFO_BG, INFO_TEXT};
            default -> new Color[]{SURFACE_SHADED, TEXT_LABEL};
        };
    }
}
