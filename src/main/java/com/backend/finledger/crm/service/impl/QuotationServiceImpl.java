package com.backend.finledger.crm.service.impl;

import com.backend.finledger.attachment.service.FileStorageService;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.DocType;
import com.backend.finledger.common.NumberSeriesService;
import com.backend.finledger.common.PageResponse;
import com.backend.finledger.common.pdf.FinLedgerPdfTheme;
import com.backend.finledger.common.pdf.PdfBillingHelper;
import com.backend.finledger.common.pdf.PdfBrandingHeaderBuilder;
import com.backend.finledger.common.pdf.PdfCellRenderers;
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
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.backend.finledger.common.mail.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class QuotationServiceImpl implements QuotationService {

    private static final Logger log = LoggerFactory.getLogger(QuotationServiceImpl.class);

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
    private final MailService mailService;
    private final FileStorageService fileStorageService;

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
                                InvoiceRepository invoiceRepository,
                                MailService mailService) {
        this(quotationRepository, quotationLineRepository, opportunityRepository,
                customerRepository, leadRepository, taxRateRepository, accountRepository,
                itemRepository, warehouseRepository, stockBalanceRepository, companyRepository,
                numberSeriesService, taxCalculator, invoiceService, invoiceRepository, mailService, null);
    }

    @Autowired
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
                                InvoiceRepository invoiceRepository,
                                MailService mailService,
                                FileStorageService fileStorageService) {
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
        this.mailService = mailService;
        this.fileStorageService = fileStorageService;
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

        String recipientEmail = null;
        String recipientName = "Customer";
        if (quotation.getCustomerId() != null) {
            Customer cust = customerRepository.findByCompanyIdAndId(companyId, quotation.getCustomerId()).orElse(null);
            if (cust != null) {
                recipientEmail = cust.getEmail();
                recipientName = cust.getName();
            }
        } else if (quotation.getLeadId() != null) {
            Lead lead = leadRepository.findByCompanyIdAndId(companyId, quotation.getLeadId()).orElse(null);
            if (lead != null) {
                recipientEmail = lead.getEmail();
                recipientName = lead.getCompanyName() != null ? lead.getCompanyName() : lead.getContactName();
            }
        }

        if (recipientEmail != null && !recipientEmail.isBlank()) {
            try {
                Company company = companyRepository.findById(companyId).orElse(null);
                String companyName = company != null ? company.getLegalName() : "FinLedger";
                byte[] pdfBytes = generateQuotationPdf(quotation.getId());
                String filename = quotation.getQuotationNo() + ".pdf";
                String subject = "Quotation #" + quotation.getQuotationNo() + " from " + companyName;
                String bodyHtml = "<p>Dear " + recipientName + ",</p>" +
                        "<p>Please find attached quotation <b>#" + quotation.getQuotationNo() + "</b> for <b>" +
                        quotation.getTotal() + " " + quotation.getCurrencyCode() + "</b>.</p>" +
                        "<p>Valid until: " + quotation.getValidUntil() + "</p>" +
                        "<p>Thank you.<br/>" + companyName + "</p>";
                mailService.sendEmailWithAttachment(recipientEmail, subject, bodyHtml, filename, pdfBytes);
            } catch (Exception e) {
                log.warn("Failed to dispatch quotation email for {}: {}", quotation.getQuotationNo(), e.getMessage());
            }
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
        String customerTrn = null;
        String customerEmail = null;
        String customerAddress = null;
        if (quotation.getCustomerId() != null) {
            Customer cust = customerRepository.findByCompanyIdAndId(companyId, quotation.getCustomerId()).orElse(null);
            if (cust != null) {
                customerName = cust.getName();
                customerTrn = cust.getTrn();
                customerEmail = cust.getEmail();
                customerAddress = cust.getBillingAddress();
            }
        } else if (quotation.getLeadId() != null) {
            Lead lead = leadRepository.findByCompanyIdAndId(companyId, quotation.getLeadId()).orElse(null);
            if (lead != null) {
                customerName = lead.getCompanyName() != null ? lead.getCompanyName() : lead.getContactName();
                customerEmail = lead.getEmail();
            }
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 46);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new PdfCellRenderers.FinLedgerPageFooterEvent(
                    company.getLegalName(), "PRICE QUOTATION #" + quotation.getQuotationNo()));
            document.open();

            PdfBrandingHeaderBuilder.addDocumentHeader(
                    document,
                    company,
                    fileStorageService,
                    "PRICE QUOTATION",
                    "# " + quotation.getQuotationNo(),
                    quotation.getStatus().name()
            );

            List<String[]> quotedToRows = new ArrayList<>();
            quotedToRows.add(new String[]{null, customerName});
            if (customerTrn != null && !customerTrn.isBlank()) {
                quotedToRows.add(new String[]{"TRN", customerTrn});
            }
            if (customerAddress != null && !customerAddress.isBlank()) {
                quotedToRows.add(new String[]{customerAddress});
            }
            if (customerEmail != null && !customerEmail.isBlank()) {
                quotedToRows.add(new String[]{"Email", customerEmail});
            }

            List<String[]> metaRows = List.of(
                    new String[]{"Quote Date", quotation.getQuoteDate().format(DateTimeFormatter.ISO_DATE)},
                    new String[]{"Valid Until", quotation.getValidUntil().format(DateTimeFormatter.ISO_DATE)},
                    new String[]{"Currency", quotation.getCurrencyCode()},
                    new String[]{"Status", quotation.getStatus().name()}
            );
            PdfBillingHelper.addTwoColumnMetaCards(document, "QUOTED TO", quotedToRows, "QUOTATION DETAILS", metaRows);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{7f, 41f, 11f, 14f, 11f, 16f});
            table.setHeaderRows(1);

            PdfBillingHelper.addLineTableHeaders(
                    table,
                    new String[]{"#", "Description", "Qty", "Unit Price", "Disc %", "Total (" + quotation.getCurrencyCode() + ")"},
                    new int[]{Element.ALIGN_CENTER, Element.ALIGN_LEFT, Element.ALIGN_RIGHT, Element.ALIGN_RIGHT, Element.ALIGN_RIGHT, Element.ALIGN_RIGHT}
            );

            for (QuotationLine line : quotation.getLines()) {
                table.addCell(PdfBillingHelper.lineBodyCell(String.valueOf(line.getLineNo()), FinLedgerPdfTheme.FONT_META, Element.ALIGN_CENTER));
                table.addCell(PdfBillingHelper.lineBodyCell(line.getDescription(), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_LEFT));
                table.addCell(PdfBillingHelper.lineBodyCell(FinLedgerPdfTheme.fmtQty(line.getQuantity()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                table.addCell(PdfBillingHelper.lineBodyCell(FinLedgerPdfTheme.fmtMoney(line.getUnitPrice()), FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                table.addCell(PdfBillingHelper.lineBodyCell(
                        line.getDiscountPct() != null && line.getDiscountPct().compareTo(BigDecimal.ZERO) > 0
                                ? FinLedgerPdfTheme.fmtQty(line.getDiscountPct()) + "%" : "-",
                        FinLedgerPdfTheme.FONT_BODY, Element.ALIGN_RIGHT));
                table.addCell(PdfBillingHelper.lineBodyCell(FinLedgerPdfTheme.fmtMoney(line.getLineTotal()), FinLedgerPdfTheme.FONT_BODY_BOLD, Element.ALIGN_RIGHT));
            }
            document.add(table);

            String ccy = quotation.getCurrencyCode();
            PdfBillingHelper.addTotalsCard(document, List.of(
                    new PdfBillingHelper.TotalsRow("Subtotal (Net)", FinLedgerPdfTheme.fmtMoney(quotation.getSubtotal()) + " " + ccy, false, false),
                    new PdfBillingHelper.TotalsRow("VAT Total", FinLedgerPdfTheme.fmtMoney(quotation.getTaxTotal()) + " " + ccy, false, false),
                    new PdfBillingHelper.TotalsRow("Grand Total", FinLedgerPdfTheme.fmtMoney(quotation.getTotal()) + " " + ccy, true, true)
            ));

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

}
