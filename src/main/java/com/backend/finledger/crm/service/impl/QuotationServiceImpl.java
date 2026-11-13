package com.backend.finledger.crm.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.DocType;
import com.backend.finledger.common.NumberSeriesService;
import com.backend.finledger.common.PageResponse;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.crm.dto.*;
import com.backend.finledger.crm.entity.Lead;
import com.backend.finledger.crm.entity.Opportunity;
import com.backend.finledger.crm.entity.Quotation;
import com.backend.finledger.crm.entity.QuotationLine;
import com.backend.finledger.crm.entity.QuotationStatus;
import com.backend.finledger.crm.repository.LeadRepository;
import com.backend.finledger.crm.repository.OpportunityRepository;
import com.backend.finledger.crm.repository.QuotationLineRepository;
import com.backend.finledger.crm.repository.QuotationRepository;
import com.backend.finledger.crm.service.QuotationService;
import com.backend.finledger.inventory.entity.Item;
import com.backend.finledger.inventory.entity.ItemType;
import com.backend.finledger.inventory.entity.StockBalance;
import com.backend.finledger.inventory.entity.Warehouse;
import com.backend.finledger.inventory.repository.ItemRepository;
import com.backend.finledger.inventory.repository.StockBalanceRepository;
import com.backend.finledger.inventory.repository.WarehouseRepository;
import com.backend.finledger.ledger.entity.Account;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.sales.dto.InvoiceDto;
import com.backend.finledger.sales.dto.InvoiceLineRequest;
import com.backend.finledger.sales.dto.InvoiceRequest;
import com.backend.finledger.sales.entity.Customer;
import com.backend.finledger.sales.entity.Invoice;
import com.backend.finledger.sales.entity.InvoiceDocType;
import com.backend.finledger.sales.repository.CustomerRepository;
import com.backend.finledger.sales.repository.InvoiceRepository;
import com.backend.finledger.sales.service.InvoiceService;
import com.backend.finledger.security.CurrentUser;
import com.backend.finledger.tax.entity.TaxRate;
import com.backend.finledger.tax.repository.TaxRateRepository;
import com.backend.finledger.tax.service.TaxCalculator;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class QuotationServiceImpl implements QuotationService {

    private final QuotationRepository quotationRepository;
    private final QuotationLineRepository quotationLineRepository;
    private final OpportunityRepository opportunityRepository;
    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final TaxRateRepository taxRateRepository;
    private final AccountRepository accountRepository;
    private final ItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final CompanyRepository companyRepository;
    private final NumberSeriesService numberSeriesService;
    private final TaxCalculator taxCalculator;
    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;

    public QuotationServiceImpl(QuotationRepository quotationRepository,
                                QuotationLineRepository quotationLineRepository,
                                OpportunityRepository opportunityRepository,
                                CustomerRepository customerRepository,
                                LeadRepository leadRepository,
                                TaxRateRepository taxRateRepository,
                                AccountRepository accountRepository,
                                ItemRepository itemRepository,
                                WarehouseRepository warehouseRepository,
                                StockBalanceRepository stockBalanceRepository,
                                CompanyRepository companyRepository,
                                NumberSeriesService numberSeriesService,
                                TaxCalculator taxCalculator,
                                InvoiceService invoiceService,
                                InvoiceRepository invoiceRepository) {
        this.quotationRepository = quotationRepository;
        this.quotationLineRepository = quotationLineRepository;
        this.opportunityRepository = opportunityRepository;
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
        this.taxRateRepository = taxRateRepository;
        this.accountRepository = accountRepository;
        this.itemRepository = itemRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.companyRepository = companyRepository;
        this.numberSeriesService = numberSeriesService;
        this.taxCalculator = taxCalculator;
        this.invoiceService = invoiceService;
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('QUOTATION_VIEW') or hasAuthority('CRM_VIEW_ALL')")
    public PageResponse<QuotationDto> getQuotations(Long customerId, Long opportunityId, QuotationStatus status,
                                                    LocalDate fromDate, LocalDate toDate, String search, Pageable pageable) {
        Long companyId = CurrentUser.getCompanyId();
        Page<Quotation> page = quotationRepository.searchQuotations(companyId, customerId, opportunityId, status, fromDate, toDate, search, pageable);
        return PageResponse.from(page.map(this::mapToDto));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('QUOTATION_VIEW') or hasAuthority('CRM_VIEW_ALL')")
    public QuotationDto getQuotationById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Quotation quotation = quotationRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("QUOTATION_NOT_FOUND", "Quotation not found with id: " + id));
        return mapToDto(quotation);
    }

    @Override
    @PreAuthorize("hasAuthority('QUOTATION_CREATE')")
    public QuotationDto createQuotation(QuotationRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Company not found"));

        Quotation quotation = new Quotation();
        quotation.setCompanyId(companyId);
        String quotationNo = numberSeriesService.generateNextNumber(companyId, DocType.QUOTATION);
        quotation.setQuotationNo(quotationNo);

        quotation.setOpportunityId(request.opportunityId());
        quotation.setCustomerId(request.customerId());
        quotation.setLeadId(request.leadId());
        quotation.setQuoteDate(request.quoteDate());
        quotation.setValidUntil(request.validUntil());
        quotation.setCurrencyCode(request.currencyCode() != null ? request.currencyCode() : company.getBaseCurrency());
        quotation.setStatus(QuotationStatus.DRAFT);

        populateAndCalculateLines(quotation, request.lines(), companyId);

        Quotation saved = quotationRepository.save(quotation);
        return mapToDto(saved);
    }

    @Override
    @PreAuthorize("hasAuthority('QUOTATION_CREATE')")
    public QuotationDto updateQuotation(Long id, QuotationRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        Quotation quotation = quotationRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("QUOTATION_NOT_FOUND", "Quotation not found with id: " + id));

        if (quotation.getStatus() != QuotationStatus.DRAFT && quotation.getStatus() != QuotationStatus.SENT) {
            throw new BusinessException("QUOTATION_NOT_EDITABLE", "Cannot edit quotation with status: " + quotation.getStatus());
        }

        quotation.setOpportunityId(request.opportunityId());
        quotation.setCustomerId(request.customerId());
        quotation.setLeadId(request.leadId());
        quotation.setQuoteDate(request.quoteDate());
        quotation.setValidUntil(request.validUntil());
        if (request.currencyCode() != null) {
            quotation.setCurrencyCode(request.currencyCode());
        }

        populateAndCalculateLines(quotation, request.lines(), companyId);

        Quotation saved = quotationRepository.save(quotation);
        return mapToDto(saved);
    }

    @Override
    @PreAuthorize("hasAuthority('QUOTATION_SEND') or hasAuthority('QUOTATION_CREATE')")
    public QuotationDto sendQuotation(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Quotation quotation = quotationRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("QUOTATION_NOT_FOUND", "Quotation not found with id: " + id));

        if (quotation.getStatus() == QuotationStatus.DRAFT) {
            quotation.setStatus(QuotationStatus.SENT);
            quotation = quotationRepository.save(quotation);
        }
        return mapToDto(quotation);
    }

    @Override
    @PreAuthorize("hasAuthority('QUOTATION_SEND') or hasAuthority('QUOTATION_CREATE')")
    public QuotationDto acceptQuotation(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Quotation quotation = quotationRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("QUOTATION_NOT_FOUND", "Quotation not found with id: " + id));

        if (quotation.getStatus() == QuotationStatus.INVOICED) {
            throw new BusinessException("QUOTATION_ALREADY_INVOICED", "Quotation is already converted to an invoice");
        }

        if (quotation.getStatus() != QuotationStatus.ACCEPTED) {
            quotation.setStatus(QuotationStatus.ACCEPTED);

            // Reserve inventory stock for lines with INVENTORY items
            Warehouse warehouse = warehouseRepository.findFirstByCompanyIdAndIsDefaultTrue(companyId)
                    .orElseGet(() -> warehouseRepository.findByCompanyId(companyId).stream().findFirst().orElse(null));

            if (warehouse != null) {
                for (QuotationLine line : quotation.getLines()) {
                    if (line.getItemId() != null) {
                        Item item = itemRepository.findByCompanyIdAndId(companyId, line.getItemId()).orElse(null);
                        if (item != null && item.getItemType() == ItemType.INVENTORY) {
                            StockBalance balance = stockBalanceRepository.findByItemIdAndWarehouseIdWithLock(item.getId(), warehouse.getId())
                                    .orElseGet(() -> new StockBalance(item.getId(), warehouse.getId()));
                            BigDecimal currentReserved = balance.getQtyReserved() != null ? balance.getQtyReserved() : BigDecimal.ZERO;
                            balance.setQtyReserved(currentReserved.add(line.getQuantity()));
                            stockBalanceRepository.save(balance);
                        }
                    }
                }
            }

            quotation = quotationRepository.save(quotation);
        }

        return mapToDto(quotation);
    }

    @Override
    @PreAuthorize("hasAuthority('QUOTATION_CONVERT')")
    public ConvertToInvoiceResponse convertToInvoice(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Quotation quotation = quotationRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("QUOTATION_NOT_FOUND", "Quotation not found with id: " + id));

        if (quotation.getInvoiceId() != null) {
            Invoice existingInvoice = invoiceRepository.findByCompanyIdAndId(companyId, quotation.getInvoiceId()).orElse(null);
            return new ConvertToInvoiceResponse(
                    quotation.getId(),
                    quotation.getQuotationNo(),
                    quotation.getInvoiceId(),
                    existingInvoice != null ? existingInvoice.getInvoiceNo() : null,
                    quotation.getStatus()
            );
        }

        if (quotation.getCustomerId() == null) {
            throw new BusinessException("CUSTOMER_REQUIRED_FOR_INVOICE", "Quotation must be linked to a customer before converting to an invoice");
        }

        Warehouse warehouse = warehouseRepository.findFirstByCompanyIdAndIsDefaultTrue(companyId)
                .orElseGet(() -> warehouseRepository.findByCompanyId(companyId).stream().findFirst().orElse(null));

        Account defaultIncomeAccount = accountRepository.findByCompanyIdAndCode(companyId, "4100")
                .orElseGet(() -> accountRepository.findByCompanyIdOrderByCodeAsc(companyId).stream()
                        .filter(a -> "INCOME".equalsIgnoreCase(a.getAccountType().name()))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException("INCOME_ACCOUNT_NOT_FOUND", "No income revenue account configured for invoice lines")));

        List<InvoiceLineRequest> invoiceLines = new ArrayList<>();
        for (QuotationLine ql : quotation.getLines()) {
            Long accountId = defaultIncomeAccount.getId();
            if (ql.getItemId() != null) {
                Item item = itemRepository.findByCompanyIdAndId(companyId, ql.getItemId()).orElse(null);
                if (item != null && item.getIncomeAccountId() != null) {
                    accountId = item.getIncomeAccountId();
                }
            }

            invoiceLines.add(new InvoiceLineRequest(
                    ql.getLineNo(),
                    ql.getItemId(),
                    ql.getDescription(),
                    ql.getQuantity(),
                    ql.getUnitPrice(),
                    ql.getDiscountPct(),
                    ql.getTaxRateId(),
                    accountId,
                    warehouse != null ? warehouse.getId() : null,
                    null
            ));
        }

        InvoiceRequest invoiceRequest = new InvoiceRequest(
                InvoiceDocType.INVOICE,
                quotation.getCustomerId(),
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                quotation.getCurrencyCode(),
                BigDecimal.ONE,
                "Converted from Quotation " + quotation.getQuotationNo(),
                quotation.getId(),
                null,
                invoiceLines
        );

        boolean wasAccepted = quotation.getStatus() == QuotationStatus.ACCEPTED;
        InvoiceDto invoiceDto = invoiceService.createInvoice(invoiceRequest);

        if (wasAccepted && warehouse != null) {
            for (QuotationLine ql : quotation.getLines()) {
                if (ql.getItemId() != null) {
                    Item item = itemRepository.findByCompanyIdAndId(companyId, ql.getItemId()).orElse(null);
                    if (item != null && item.getItemType() == ItemType.INVENTORY) {
                        stockBalanceRepository.findByItemIdAndWarehouseIdWithLock(item.getId(), warehouse.getId())
                                .ifPresent(balance -> {
                                    BigDecimal currentReserved = balance.getQtyReserved() != null
                                            ? balance.getQtyReserved() : BigDecimal.ZERO;
                                    BigDecimal qtyToRelease = ql.getQuantity() != null
                                            ? ql.getQuantity() : BigDecimal.ZERO;
                                    BigDecimal updated = currentReserved.subtract(qtyToRelease);
                                    balance.setQtyReserved(updated.compareTo(BigDecimal.ZERO) > 0 ? updated : BigDecimal.ZERO);
                                    stockBalanceRepository.save(balance);
                                });
                    }
                }
            }
        }

        quotation.setStatus(QuotationStatus.INVOICED);
        quotation.setInvoiceId(invoiceDto.id());
        quotationRepository.save(quotation);

        return new ConvertToInvoiceResponse(
                quotation.getId(),
                quotation.getQuotationNo(),
                invoiceDto.id(),
                invoiceDto.invoiceNo(),
                quotation.getStatus()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateQuotationPdf(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Quotation quotation = quotationRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("QUOTATION_NOT_FOUND", "Quotation not found with id: " + id));

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Company not found"));

        String customerName = "N/A";
        String customerTrn = "";
        if (quotation.getCustomerId() != null) {
            Customer cust = customerRepository.findByCompanyIdAndId(companyId, quotation.getCustomerId()).orElse(null);
            if (cust != null) {
                customerName = cust.getName();
                customerTrn = cust.getTrn() != null ? "TRN: " + cust.getTrn() : "";
            }
        } else if (quotation.getLeadId() != null) {
            Lead lead = leadRepository.findByCompanyIdAndId(companyId, quotation.getLeadId()).orElse(null);
            if (lead != null) {
                customerName = lead.getCompanyName() != null ? lead.getCompanyName() : lead.getContactName();
            }
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 40, 40);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(15, 23, 42));
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(51, 65, 85));
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(51, 65, 85));
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(15, 23, 42));

            Paragraph title = new Paragraph("PRICE QUOTATION", titleFont);
            title.setAlignment(Element.ALIGN_RIGHT);
            document.add(title);

            Paragraph docNo = new Paragraph("# " + quotation.getQuotationNo(), subTitleFont);
            docNo.setAlignment(Element.ALIGN_RIGHT);
            document.add(docNo);

            document.add(new Paragraph(" "));

            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);

            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.addElement(new Paragraph("ISSUED BY:", boldFont));
            leftCell.addElement(new Paragraph(company.getLegalName(), boldFont));
            if (company.getTrn() != null && !company.getTrn().isBlank()) {
                leftCell.addElement(new Paragraph("TRN: " + company.getTrn(), regularFont));
            }
            if (company.getAddress() != null) {
                leftCell.addElement(new Paragraph(company.getAddress(), regularFont));
            }
            headerTable.addCell(leftCell);

            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.addElement(new Paragraph("QUOTED TO:", boldFont));
            rightCell.addElement(new Paragraph(customerName, boldFont));
            if (!customerTrn.isBlank()) {
                rightCell.addElement(new Paragraph(customerTrn, regularFont));
            }
            rightCell.addElement(new Paragraph("Quote Date: " + quotation.getQuoteDate().format(DateTimeFormatter.ISO_DATE), regularFont));
            rightCell.addElement(new Paragraph("Valid Until: " + quotation.getValidUntil().format(DateTimeFormatter.ISO_DATE), regularFont));
            rightCell.addElement(new Paragraph("Status: " + quotation.getStatus().name(), regularFont));
            headerTable.addCell(rightCell);

            document.add(headerTable);
            document.add(new Paragraph(" "));

            // Lines table
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{8f, 40f, 12f, 14f, 12f, 14f});

            addHeaderCell(table, "#", boldFont);
            addHeaderCell(table, "Description", boldFont);
            addHeaderCell(table, "Qty", boldFont);
            addHeaderCell(table, "Unit Price", boldFont);
            addHeaderCell(table, "Disc %", boldFont);
            addHeaderCell(table, "Total (" + quotation.getCurrencyCode() + ")", boldFont);

            for (QuotationLine line : quotation.getLines()) {
                table.addCell(createBodyCell(String.valueOf(line.getLineNo()), regularFont, Element.ALIGN_CENTER));
                table.addCell(createBodyCell(line.getDescription(), regularFont, Element.ALIGN_LEFT));
                table.addCell(createBodyCell(line.getQuantity().toPlainString(), regularFont, Element.ALIGN_RIGHT));
                table.addCell(createBodyCell(line.getUnitPrice().toPlainString(), regularFont, Element.ALIGN_RIGHT));
                table.addCell(createBodyCell(line.getDiscountPct() != null ? line.getDiscountPct().toPlainString() + "%" : "0%", regularFont, Element.ALIGN_RIGHT));
                table.addCell(createBodyCell(line.getLineTotal().toPlainString(), regularFont, Element.ALIGN_RIGHT));
            }

            document.add(table);
            document.add(new Paragraph(" "));

            // Totals
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(45);
            totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            addTotalRow(totalTable, "Subtotal:", quotation.getSubtotal() + " " + quotation.getCurrencyCode(), regularFont);
            addTotalRow(totalTable, "VAT Total:", quotation.getTaxTotal() + " " + quotation.getCurrencyCode(), regularFont);
            addTotalRow(totalTable, "Grand Total:", quotation.getTotal() + " " + quotation.getCurrencyCode(), boldFont);

            document.add(totalTable);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("PDF_GENERATION_FAILED", "Failed to generate quotation PDF: " + e.getMessage());
        }
    }

    private void populateAndCalculateLines(Quotation quotation, List<QuotationLineRequest> lineRequests, Long companyId) {
        quotation.getLines().clear();
        short lineNo = 1;
        List<TaxCalculator.CalculatedLine> calculatedLines = new ArrayList<>();

        for (QuotationLineRequest lr : lineRequests) {
            BigDecimal taxRateValue = BigDecimal.ZERO;
            if (lr.taxRateId() != null) {
                TaxRate taxRate = taxRateRepository.findByCompanyIdAndId(companyId, lr.taxRateId())
                        .orElseThrow(() -> new BusinessException("TAX_RATE_NOT_FOUND", "Tax rate not found for id: " + lr.taxRateId()));
                taxRateValue = taxRate.getRate();
            }

            TaxCalculator.CalculatedLine calc = taxCalculator.calculateLine(
                    lr.quantity(),
                    lr.unitPrice(),
                    lr.discountPct(),
                    taxRateValue
            );
            calculatedLines.add(calc);

            QuotationLine line = new QuotationLine();
            line.setLineNo(lineNo++);
            line.setItemId(lr.itemId());
            line.setDescription(lr.description());
            line.setQuantity(lr.quantity());
            line.setUnitPrice(lr.unitPrice());
            line.setDiscountPct(lr.discountPct() != null ? lr.discountPct() : BigDecimal.ZERO);
            line.setTaxRateId(lr.taxRateId());
            line.setLineTotal(calc.lineTotal());

            quotation.addLine(line);
        }

        TaxCalculator.CalculatedDocumentTotals totals = taxCalculator.calculateDocumentTotals(calculatedLines);
        quotation.setSubtotal(totals.subtotal());
        quotation.setTaxTotal(totals.taxTotal());
        quotation.setTotal(totals.grandTotal());
    }

    private QuotationDto mapToDto(Quotation quotation) {
        String oppTitle = null;
        if (quotation.getOpportunityId() != null) {
            oppTitle = opportunityRepository.findById(quotation.getOpportunityId())
                    .map(Opportunity::getTitle)
                    .orElse(null);
        }

        String customerName = null;
        if (quotation.getCustomerId() != null) {
            customerName = customerRepository.findById(quotation.getCustomerId())
                    .map(Customer::getName)
                    .orElse(null);
        }

        String leadNo = null;
        if (quotation.getLeadId() != null) {
            leadNo = leadRepository.findById(quotation.getLeadId())
                    .map(Lead::getLeadNo)
                    .orElse(null);
        }

        String invoiceNo = null;
        if (quotation.getInvoiceId() != null) {
            invoiceNo = invoiceRepository.findById(quotation.getInvoiceId())
                    .map(Invoice::getInvoiceNo)
                    .orElse(null);
        }

        List<QuotationLineDto> lines = quotation.getLines().stream()
                .map(l -> {
                    String itemSku = null;
                    String itemName = null;
                    if (l.getItemId() != null) {
                        Item item = itemRepository.findById(l.getItemId()).orElse(null);
                        if (item != null) {
                            itemSku = item.getSku();
                            itemName = item.getName();
                        }
                    }
                    BigDecimal taxRatePct = null;
                    if (l.getTaxRateId() != null) {
                        taxRatePct = taxRateRepository.findById(l.getTaxRateId())
                                .map(TaxRate::getRate)
                                .orElse(null);
                    }
                    return new QuotationLineDto(
                            l.getId(),
                            l.getLineNo(),
                            l.getItemId(),
                            itemSku,
                            itemName,
                            l.getDescription(),
                            l.getQuantity(),
                            l.getUnitPrice(),
                            l.getDiscountPct(),
                            l.getTaxRateId(),
                            taxRatePct,
                            l.getLineTotal()
                    );
                })
                .toList();

        return new QuotationDto(
                quotation.getId(),
                quotation.getCompanyId(),
                quotation.getQuotationNo(),
                quotation.getOpportunityId(),
                oppTitle,
                quotation.getCustomerId(),
                customerName,
                quotation.getLeadId(),
                leadNo,
                quotation.getQuoteDate(),
                quotation.getValidUntil(),
                quotation.getCurrencyCode(),
                quotation.getSubtotal(),
                quotation.getTaxTotal(),
                quotation.getTotal(),
                quotation.getStatus(),
                quotation.getInvoiceId(),
                invoiceNo,
                lines,
                quotation.getCreatedAt()
        );
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(241, 245, 249));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private PdfPCell createBodyCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(6);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell lblCell = new PdfPCell(new Phrase(label, font));
        lblCell.setBorder(Rectangle.NO_BORDER);
        lblCell.setPadding(4);
        table.addCell(lblCell);

        PdfPCell valCell = new PdfPCell(new Phrase(value, font));
        valCell.setBorder(Rectangle.NO_BORDER);
        valCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valCell.setPadding(4);
        table.addCell(valCell);
    }
}
