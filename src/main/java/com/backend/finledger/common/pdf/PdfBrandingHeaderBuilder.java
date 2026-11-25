package com.backend.finledger.common.pdf;

import com.backend.finledger.attachment.service.FileStorageService;
import com.backend.finledger.company.entity.Company;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.awt.Color;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the unified FinLedger header band (Company logo + details on left, Document/Report title + subtitle/pill on right)
 * matching the clean webpage card header aesthetic.
 */
public final class PdfBrandingHeaderBuilder {

    private static final Logger log = LoggerFactory.getLogger(PdfBrandingHeaderBuilder.class);

    private PdfBrandingHeaderBuilder() {}

    public static void addDocumentHeader(
            Document document,
            Company company,
            FileStorageService fileStorageService,
            String documentTitle,
            String subtitle,
            String statusBadge
    ) {
        try {
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{55f, 45f});
            header.setSpacingAfter(12f);

            // Left: Company Logo + Legal Name + TRN / Contact
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.BOTTOM);
            leftCell.setBorderColor(FinLedgerPdfTheme.BORDER_LIGHT);
            leftCell.setBorderWidth(1.5f);
            leftCell.setPaddingBottom(10f);
            leftCell.setPaddingTop(0f);
            leftCell.setPaddingLeft(0f);
            leftCell.setPaddingRight(8f);
            leftCell.setVerticalAlignment(Element.ALIGN_TOP);

            Image logo = loadCompanyLogo(company, fileStorageService);
            if (logo != null) {
                logo.scaleToFit(95f, 36f);
                logo.setSpacingAfter(4f);
                leftCell.addElement(logo);
            }

            String companyName = (company != null && company.getLegalName() != null && !company.getLegalName().isBlank())
                    ? company.getLegalName() : "FinLedger ERP";
            Paragraph compPara = new Paragraph(companyName, FinLedgerPdfTheme.FONT_COMPANY);
            compPara.setSpacingAfter(2f);
            leftCell.addElement(compPara);

            if (company != null) {
                List<String> metaLines = new ArrayList<>();
                if (company.getTrn() != null && !company.getTrn().isBlank()) {
                    metaLines.add("TRN: " + company.getTrn());
                }
                if (company.getAddress() != null && !company.getAddress().isBlank()) {
                    metaLines.add(company.getAddress());
                }
                StringBuilder contact = new StringBuilder();
                if (company.getEmail() != null && !company.getEmail().isBlank()) {
                    contact.append(company.getEmail());
                }
                if (company.getPhone() != null && !company.getPhone().isBlank()) {
                    if (!contact.isEmpty()) contact.append("  •  ");
                    contact.append(company.getPhone());
                }
                if (!contact.isEmpty()) {
                    metaLines.add(contact.toString());
                }
                for (String ml : metaLines) {
                    Paragraph p = new Paragraph(ml, FinLedgerPdfTheme.FONT_META);
                    p.setLeading(11f);
                    leftCell.addElement(p);
                }
            }
            header.addCell(leftCell);

            // Right: Document Title, Subtitle, Optional Status Pill
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.BOTTOM);
            rightCell.setBorderColor(FinLedgerPdfTheme.BORDER_LIGHT);
            rightCell.setBorderWidth(1.5f);
            rightCell.setPaddingBottom(10f);
            rightCell.setPaddingTop(0f);
            rightCell.setPaddingRight(0f);
            rightCell.setPaddingLeft(8f);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            rightCell.setVerticalAlignment(Element.ALIGN_TOP);

            Paragraph titlePara = new Paragraph(documentTitle, FinLedgerPdfTheme.FONT_DOC_TITLE);
            titlePara.setAlignment(Element.ALIGN_RIGHT);
            titlePara.setSpacingAfter(3f);
            rightCell.addElement(titlePara);

            if (subtitle != null && !subtitle.isBlank()) {
                Paragraph subPara = new Paragraph(subtitle, FinLedgerPdfTheme.FONT_DOC_SUB);
                subPara.setAlignment(Element.ALIGN_RIGHT);
                subPara.setSpacingAfter(5f);
                rightCell.addElement(subPara);
            }

            if (statusBadge != null && !statusBadge.isBlank()) {
                Color[] colors = FinLedgerPdfTheme.statusColors(statusBadge);
                PdfPTable badgeWrap = new PdfPTable(1);
                badgeWrap.setWidthPercentage(35);
                badgeWrap.setHorizontalAlignment(Element.ALIGN_RIGHT);
                badgeWrap.addCell(PdfCellRenderers.createPillCell(statusBadge, colors[0], colors[1]));
                rightCell.addElement(badgeWrap);
            }

            header.addCell(rightCell);
            document.add(header);
        } catch (Exception e) {
            log.warn("Failed to render PDF branding header: {}", e.getMessage());
        }
    }

    private static Image loadCompanyLogo(Company company, FileStorageService fileStorageService) {
        if (company == null || fileStorageService == null) return null;
        String logoPath = company.getLogoPath();
        if (logoPath == null || logoPath.isBlank()) return null;

        try {
            Resource resource = fileStorageService.loadAsResource(logoPath);
            if (resource == null || !resource.exists() || !resource.isReadable()) {
                return null;
            }
            try (InputStream is = resource.getInputStream()) {
                byte[] bytes = is.readAllBytes();
                if (bytes.length == 0) return null;
                return Image.getInstance(bytes);
            }
        } catch (Exception e) {
            log.debug("Could not load company logo for PDF ({}): {}", logoPath, e.getMessage());
            return null;
        }
    }
}
