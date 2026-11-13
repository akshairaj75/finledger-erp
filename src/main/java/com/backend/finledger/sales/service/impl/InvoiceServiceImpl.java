package com.backend.finledger.sales.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.DocType;
import com.backend.finledger.common.ForbiddenException;
import com.backend.finledger.common.NumberSeriesService;
import com.backend.finledger.common.PageResponse;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.ledger.entity.*;
import com.backend.finledger.ledger.repository.*;
import com.backend.finledger.ledger.service.PostingService;
import com.backend.finledger.inventory.entity.Item;
import com.backend.finledger.inventory.entity.ItemType;
import com.backend.finledger.inventory.entity.MovementType;
import com.backend.finledger.inventory.entity.StockMovement;
import com.backend.finledger.inventory.entity.StockSourceType;
import com.backend.finledger.inventory.entity.Warehouse;
import com.backend.finledger.inventory.repository.ItemRepository;
import com.backend.finledger.inventory.repository.WarehouseRepository;
import com.backend.finledger.inventory.service.StockService;
import com.backend.finledger.ledger.dto.JournalEntryDto;
import com.backend.finledger.ledger.dto.JournalLineRequest;
import com.backend.finledger.ledger.dto.SourceDocumentPostingRequest;
import com.backend.finledger.sales.dto.*;
import com.backend.finledger.sales.entity.Customer;
import com.backend.finledger.sales.entity.Invoice;
import com.backend.finledger.sales.entity.InvoiceDocType;
import com.backend.finledger.sales.entity.InvoiceLine;
import com.backend.finledger.sales.entity.InvoiceStatus;
import com.backend.finledger.sales.mapper.SalesMapper;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final TaxRateRepository taxRateRepository;
    private final AccountRepository accountRepository;
    private final CompanyRepository companyRepository;
    private final PostingService postingService;
    private final NumberSeriesService numberSeriesService;
    private final TaxCalculator taxCalculator;
    private final SalesMapper salesMapper;
    private final ItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockService stockService;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository,
                              CustomerRepository customerRepository,
                              TaxRateRepository taxRateRepository,
                              AccountRepository accountRepository,
                              CompanyRepository companyRepository,
                              PostingService postingService,
                              NumberSeriesService numberSeriesService,
                              TaxCalculator taxCalculator,
                              SalesMapper salesMapper,
                              ItemRepository itemRepository,
                              WarehouseRepository warehouseRepository,
                              StockService stockService) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.taxRateRepository = taxRateRepository;
        this.accountRepository = accountRepository;
        this.companyRepository = companyRepository;
        this.postingService = postingService;
        this.numberSeriesService = numberSeriesService;
        this.taxCalculator = taxCalculator;
        this.salesMapper = salesMapper;
        this.itemRepository = itemRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockService = stockService;
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('INVOICE_CREATE')")
    public InvoiceDto createInvoice(InvoiceRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        Customer customer = customerRepository.findByCompanyIdAndId(companyId, request.customerId())
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found with id: " + request.customerId()));

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Company not found"));

        Invoice invoice = new Invoice();
        invoice.setCompanyId(companyId);
        invoice.setInvoiceNo("DRAFT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        invoice.setDocType(request.docType() != null ? request.docType() : InvoiceDocType.INVOICE);
        invoice.setCustomerId(customer.getId());
        invoice.setInvoiceDate(request.invoiceDate());

        LocalDate dueDate = request.dueDate();
        if (dueDate == null) {
            short terms = customer.getPaymentTermsDays() != null ? customer.getPaymentTermsDays() : 30;
            dueDate = request.invoiceDate().plusDays(terms);
        }
        invoice.setDueDate(dueDate);

        String currency = request.currencyCode() != null ? request.currencyCode() : customer.getCurrencyCode();
        if (currency == null) {
            currency = company.getBaseCurrency();
        }
        invoice.setCurrencyCode(currency);
        invoice.setExchangeRate(request.exchangeRate() != null && request.exchangeRate().compareTo(BigDecimal.ZERO) > 0
                ? request.exchangeRate() : BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP));

        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setNotes(request.notes());
        invoice.setQuotationId(request.quotationId());
        invoice.setSalespersonUserId(request.salespersonUserId());

        populateAndCalculateLines(invoice, request.lines(), companyId);

        Invoice saved = invoiceRepository.save(invoice);
        if (Boolean.TRUE.equals(request.send())) {
            if (!CurrentUser.hasAuthority("INVOICE_SEND")) {
                throw new ForbiddenException("INVOICE_SEND_FORBIDDEN", "You do not have permission to send invoices.");
            }
            return sendInvoice(saved.getId());
        }
        return mapToDtoWithCustomer(saved, customer);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('INVOICE_CREATE')")
    public InvoiceDto updateInvoice(Long id, InvoiceRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        Invoice invoice = invoiceRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("INVOICE_NOT_FOUND", "Invoice not found with id: " + id));

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessException("INVOICE_NOT_EDITABLE", "Only draft invoices can be edited. Current status: " + invoice.getStatus());
        }

        Customer customer = customerRepository.findByCompanyIdAndId(companyId, request.customerId())
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found with id: " + request.customerId()));

        invoice.setDocType(request.docType() != null ? request.docType() : invoice.getDocType());
        invoice.setCustomerId(customer.getId());
        invoice.setInvoiceDate(request.invoiceDate());

        LocalDate dueDate = request.dueDate();
        if (dueDate == null) {
            short terms = customer.getPaymentTermsDays() != null ? customer.getPaymentTermsDays() : 30;
            dueDate = request.invoiceDate().plusDays(terms);
        }
        invoice.setDueDate(dueDate);

        if (request.currencyCode() != null) {
            invoice.setCurrencyCode(request.currencyCode());
        }
        if (request.exchangeRate() != null && request.exchangeRate().compareTo(BigDecimal.ZERO) > 0) {
            invoice.setExchangeRate(request.exchangeRate());
        }
        invoice.setNotes(request.notes());
        invoice.setQuotationId(request.quotationId());
        invoice.setSalespersonUserId(request.salespersonUserId());

        invoice.clearLines();
        populateAndCalculateLines(invoice, request.lines(), companyId);

        Invoice saved = invoiceRepository.save(invoice);
        if (Boolean.TRUE.equals(request.send())) {
            if (!CurrentUser.hasAuthority("INVOICE_SEND")) {
                throw new ForbiddenException("INVOICE_SEND_FORBIDDEN", "You do not have permission to send invoices.");
            }
            return sendInvoice(saved.getId());
        }
        return mapToDtoWithCustomer(saved, customer);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('INVOICE_VIEW')")
    public InvoiceDto getInvoiceById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Invoice invoice = invoiceRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("INVOICE_NOT_FOUND", "Invoice not found with id: " + id));

        Customer customer = customerRepository.findByCompanyIdAndId(companyId, invoice.getCustomerId()).orElse(null);
        return mapToDtoWithCustomer(invoice, customer);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('INVOICE_VIEW')")
    public PageResponse<InvoiceDto> getInvoices(Long customerId, Pageable pageable) {
        return getInvoices(customerId, null, null, null, null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('INVOICE_VIEW')")
    public PageResponse<InvoiceDto> getInvoices(
            Long customerId,
            InvoiceStatus status,
            InvoiceDocType docType,
            LocalDate from,
            LocalDate to,
            String q,
            Pageable pageable) {
        Long companyId = CurrentUser.getCompanyId();
        String searchTerm = (q != null && !q.isBlank()) ? q.trim() : null;
        Page<Invoice> page = invoiceRepository.searchInvoices(
                companyId, customerId, status, docType, from, to, searchTerm, pageable);

        return PageResponse.from(page.map(inv -> {
            Customer cust = customerRepository.findByCompanyIdAndId(companyId, inv.getCustomerId()).orElse(null);
            return mapToDtoWithCustomer(inv, cust);
        }));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('INVOICE_SEND')")
    public InvoiceDto sendInvoice(Long id) {
        Long companyId = CurrentUser.getCompanyId();

        Invoice invoice = invoiceRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("INVOICE_NOT_FOUND", "Invoice not found with id: " + id));

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessException("INVALID_STATUS", "Only draft invoices can be sent/posted. Current status: " + invoice.getStatus());
        }

        Customer customer = customerRepository.findByCompanyIdAndId(companyId, invoice.getCustomerId())
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found for id: " + invoice.getCustomerId()));

        DocType numberSeriesDocType = (invoice.getDocType() == InvoiceDocType.INVOICE) ? DocType.INVOICE : DocType.CREDIT_NOTE;
        String generatedNo = numberSeriesService.generateNextNumber(companyId, numberSeriesDocType);
        invoice.setInvoiceNo(generatedNo);
        invoice.setStatus(InvoiceStatus.SENT);

        List<JournalLineRequest> journalLines = new ArrayList<>();

        if (invoice.getDocType() == InvoiceDocType.INVOICE) {
            journalLines.add(new JournalLineRequest(
                    customer.getArAccountId(),
                    null,
                    "AR - Invoice #" + invoice.getInvoiceNo(),
                    invoice.getTotal(),
                    BigDecimal.ZERO,
                    PartyType.CUSTOMER,
                    customer.getId()
            ));

            for (InvoiceLine line : invoice.getLines()) {
                BigDecimal netAmount = line.getLineTotal().subtract(line.getTaxAmount());
                if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                    journalLines.add(new JournalLineRequest(
                            line.getAccountId(),
                            line.getCostCenterId(),
                            line.getDescription(),
                            BigDecimal.ZERO,
                            netAmount,
                            null,
                            null
                    ));
                }

                if (line.getTaxAmount().compareTo(BigDecimal.ZERO) > 0 && line.getTaxRateId() != null) {
                    TaxRate taxRate = taxRateRepository.findByCompanyIdAndId(companyId, line.getTaxRateId())
                            .orElseThrow(() -> new BusinessException("TAX_RATE_NOT_FOUND", "Tax rate not found for id: " + line.getTaxRateId()));

                    Long taxAccountId = taxRate.getSalesAccountId();
                    if (taxAccountId == null) {
                        throw new BusinessException("TAX_ACCOUNT_MISSING", "Tax rate " + taxRate.getCode() + " is missing a sales GL account");
                    }

                    journalLines.add(new JournalLineRequest(
                            taxAccountId,
                            line.getCostCenterId(),
                            "Output VAT (" + taxRate.getCode() + ") - " + invoice.getInvoiceNo(),
                            BigDecimal.ZERO,
                            line.getTaxAmount(),
                            null,
                            null
                    ));
                }

                // Inventory stock issue and COGS posting
                if (line.getItemId() != null) {
                    itemRepository.findByCompanyIdAndId(companyId, line.getItemId()).ifPresent(item -> {
                        if (item.getItemType() == ItemType.INVENTORY) {
                            Long whId = line.getWarehouseId();
                            if (whId == null) {
                                whId = warehouseRepository.findFirstByCompanyIdAndIsDefaultTrue(companyId)
                                        .map(Warehouse::getId)
                                        .orElse(1L);
                            }

                            StockMovement sm = stockService.issueStock(
                                    companyId,
                                    item.getId(),
                                    whId,
                                    line.getQuantity(),
                                    MovementType.SALE,
                                    StockSourceType.INVOICE,
                                    invoice.getId(),
                                    line.getId(),
                                    null
                            );

                            line.setUnitCost(sm.getUnitCost());
                            BigDecimal cogsVal = line.getQuantity().multiply(sm.getUnitCost()).setScale(2, RoundingMode.HALF_UP);

                            if (cogsVal.compareTo(BigDecimal.ZERO) > 0) {
                                Long cogsAcc = item.getCogsAccountId();
                                if (cogsAcc == null) {
                                    cogsAcc = accountRepository.findByCompanyIdAndCode(companyId, "5100")
                                            .or(() -> accountRepository.findByCompanyIdAndCode(companyId, "5000"))
                                            .map(Account::getId)
                                            .orElse(line.getAccountId());
                                }

                                Long invAcc = item.getInventoryAccountId();
                                if (invAcc == null) {
                                    invAcc = accountRepository.findByCompanyIdAndCode(companyId, "1300")
                                            .map(Account::getId)
                                            .orElse(line.getAccountId());
                                }

                                journalLines.add(new JournalLineRequest(
                                        cogsAcc,
                                        line.getCostCenterId(),
                                        "COGS (" + item.getSku() + ") - " + invoice.getInvoiceNo(),
                                        cogsVal,
                                        BigDecimal.ZERO,
                                        null,
                                        null
                                ));

                                journalLines.add(new JournalLineRequest(
                                        invAcc,
                                        line.getCostCenterId(),
                                        "Inventory Relief (" + item.getSku() + ") - " + invoice.getInvoiceNo(),
                                        BigDecimal.ZERO,
                                        cogsVal,
                                        null,
                                        null
                                ));
                            }
                        }
                    });
                }
            }
        } else {
            // CREDIT NOTE
            for (InvoiceLine line : invoice.getLines()) {
                BigDecimal netAmount = line.getLineTotal().subtract(line.getTaxAmount());
                if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                    journalLines.add(new JournalLineRequest(
                            line.getAccountId(),
                            line.getCostCenterId(),
                            "Credit: " + line.getDescription(),
                            netAmount,
                            BigDecimal.ZERO,
                            null,
                            null
                    ));
                }

                if (line.getTaxAmount().compareTo(BigDecimal.ZERO) > 0 && line.getTaxRateId() != null) {
                    TaxRate taxRate = taxRateRepository.findByCompanyIdAndId(companyId, line.getTaxRateId())
                            .orElseThrow(() -> new BusinessException("TAX_RATE_NOT_FOUND", "Tax rate not found for id: " + line.getTaxRateId()));

                    Long taxAccountId = taxRate.getSalesAccountId();
                    if (taxAccountId == null) {
                        throw new BusinessException("TAX_ACCOUNT_MISSING", "Tax rate " + taxRate.getCode() + " is missing a sales GL account");
                    }

                    journalLines.add(new JournalLineRequest(
                            taxAccountId,
                            line.getCostCenterId(),
                            "Output VAT Reversal (" + taxRate.getCode() + ") - " + invoice.getInvoiceNo(),
                            line.getTaxAmount(),
                            BigDecimal.ZERO,
                            null,
                            null
                    ));
                }
            }

            journalLines.add(new JournalLineRequest(
                    customer.getArAccountId(),
                    null,
                    "AR - Credit Note #" + invoice.getInvoiceNo(),
                    BigDecimal.ZERO,
                    invoice.getTotal(),
                    PartyType.CUSTOMER,
                    customer.getId()
            ));
        }

        JournalSourceType sourceType = (invoice.getDocType() == InvoiceDocType.INVOICE)
                ? JournalSourceType.INVOICE : JournalSourceType.CREDIT_NOTE;

        SourceDocumentPostingRequest postingRequest = new SourceDocumentPostingRequest(
                sourceType,
                invoice.getId(),
                invoice.getInvoiceDate(),
                invoice.getInvoiceNo(),
                (invoice.getDocType() == InvoiceDocType.INVOICE ? "Sales Invoice " : "Credit Note ") + invoice.getInvoiceNo() + " - " + customer.getName(),
                invoice.getCurrencyCode(),
                invoice.getExchangeRate(),
                journalLines
        );

        JournalEntryDto postedJournal = postingService.postDocument(postingRequest);
        invoice.setJournalEntryId(postedJournal.id());

        Invoice saved = invoiceRepository.save(invoice);
        return mapToDtoWithCustomer(saved, customer);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('INVOICE_VOID')")
    public InvoiceDto voidInvoice(Long id, String reason) {
        Long companyId = CurrentUser.getCompanyId();

        Invoice invoice = invoiceRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("INVOICE_NOT_FOUND", "Invoice not found with id: " + id));

        if (invoice.getStatus() == InvoiceStatus.VOID) {
            throw new BusinessException("ALREADY_VOID", "Invoice is already void");
        }

        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            invoice.setStatus(InvoiceStatus.VOID);
            Invoice saved = invoiceRepository.save(invoice);
            return mapToDtoWithCustomer(saved, null);
        }

        if (invoice.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("CANNOT_VOID_PAID_INVOICE",
                    "Cannot void invoice with applied payments (amount paid: " + invoice.getAmountPaid() + "). Void receipts first.");
        }

        if (invoice.getJournalEntryId() != null) {
            postingService.reverseJournalEntry(invoice.getJournalEntryId(),
                    "Voiding " + invoice.getDocType() + " " + invoice.getInvoiceNo() + (reason != null ? ": " + reason : ""));
        }

        // Reverse stock movements if inventory items were sold
        if (invoice.getDocType() == InvoiceDocType.INVOICE) {
            for (InvoiceLine line : invoice.getLines()) {
                if (line.getItemId() != null) {
                    itemRepository.findByCompanyIdAndId(companyId, line.getItemId()).ifPresent(item -> {
                        if (item.getItemType() == ItemType.INVENTORY) {
                            Long whId = line.getWarehouseId();
                            if (whId == null) {
                                whId = warehouseRepository.findFirstByCompanyIdAndIsDefaultTrue(companyId)
                                        .map(Warehouse::getId)
                                        .orElse(1L);
                            }
                            BigDecimal cost = line.getUnitCost() != null ? line.getUnitCost() : BigDecimal.ZERO;
                            stockService.receiveStock(
                                    companyId,
                                    item.getId(),
                                    whId,
                                    line.getQuantity(),
                                    cost,
                                    MovementType.SALE_RETURN,
                                    StockSourceType.INVOICE,
                                    invoice.getId(),
                                    line.getId(),
                                    null
                            );
                        }
                    });
                }
            }
        }

        invoice.setStatus(InvoiceStatus.VOID);
        invoice.setBalanceDue(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        Invoice saved = invoiceRepository.save(invoice);
        Customer customer = customerRepository.findByCompanyIdAndId(companyId, invoice.getCustomerId()).orElse(null);
        return mapToDtoWithCustomer(saved, customer);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('INVOICE_VIEW')")
    public byte[] generateInvoicePdf(Long id) {
        Long companyId = CurrentUser.getCompanyId();

        Invoice invoice = invoiceRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("INVOICE_NOT_FOUND", "Invoice not found with id: " + id));

        Customer customer = customerRepository.findByCompanyIdAndId(companyId, invoice.getCustomerId())
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found"));

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Company not found"));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 40, 40);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(15, 23, 42));
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(51, 65, 85));
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(51, 65, 85));
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(15, 23, 42));

            String docTitle = (invoice.getDocType() == InvoiceDocType.INVOICE) ? "TAX INVOICE" : "CREDIT NOTE";
            Paragraph title = new Paragraph(docTitle, titleFont);
            title.setAlignment(Element.ALIGN_RIGHT);
            document.add(title);

            Paragraph docNo = new Paragraph("# " + invoice.getInvoiceNo(), subTitleFont);
            docNo.setAlignment(Element.ALIGN_RIGHT);
            document.add(docNo);

            document.add(new Paragraph(" "));

            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{60, 40});

            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.addElement(new Paragraph(company.getLegalName(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK)));
            if (company.getTrn() != null) {
                leftCell.addElement(new Paragraph("TRN / Tax ID: " + company.getTrn(), regularFont));
            }
            if (company.getAddress() != null) {
                leftCell.addElement(new Paragraph(company.getAddress(), regularFont));
            }
            if (company.getEmail() != null) {
                leftCell.addElement(new Paragraph("Email: " + company.getEmail(), regularFont));
            }
            headerTable.addCell(leftCell);

            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.addElement(new Paragraph("Date: " + invoice.getInvoiceDate().format(DateTimeFormatter.ISO_DATE), regularFont));
            rightCell.addElement(new Paragraph("Due Date: " + invoice.getDueDate().format(DateTimeFormatter.ISO_DATE), regularFont));
            rightCell.addElement(new Paragraph("Currency: " + invoice.getCurrencyCode(), regularFont));
            rightCell.addElement(new Paragraph("Status: " + invoice.getStatus(), boldFont));
            headerTable.addCell(rightCell);

            document.add(headerTable);
            document.add(new Paragraph(" "));

            Paragraph billToHeader = new Paragraph("BILL TO:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(71, 85, 105)));
            document.add(billToHeader);
            document.add(new Paragraph(customer.getName() + " (" + customer.getCode() + ")", boldFont));
            if (customer.getTrn() != null && !customer.getTrn().isBlank()) {
                document.add(new Paragraph("TRN: " + customer.getTrn(), regularFont));
            }
            if (customer.getBillingAddress() != null && !customer.getBillingAddress().isBlank()) {
                document.add(new Paragraph(customer.getBillingAddress(), regularFont));
            }
            if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
                document.add(new Paragraph("Email: " + customer.getEmail(), regularFont));
            }

            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{8, 42, 12, 14, 10, 14});

            String[] headers = {"#", "Description", "Qty", "Unit Price", "Tax", "Total"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
                cell.setBackgroundColor(new Color(30, 41, 59));
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            for (InvoiceLine line : invoice.getLines()) {
                table.addCell(createTableCell(String.valueOf(line.getLineNo()), Element.ALIGN_CENTER, regularFont));
                table.addCell(createTableCell(line.getDescription(), Element.ALIGN_LEFT, regularFont));
                table.addCell(createTableCell(line.getQuantity().stripTrailingZeros().toPlainString(), Element.ALIGN_RIGHT, regularFont));
                table.addCell(createTableCell(line.getUnitPrice().toString(), Element.ALIGN_RIGHT, regularFont));
                table.addCell(createTableCell(line.getTaxAmount().toString(), Element.ALIGN_RIGHT, regularFont));
                table.addCell(createTableCell(line.getLineTotal().toString(), Element.ALIGN_RIGHT, regularFont));
            }

            document.add(table);
            document.add(new Paragraph(" "));

            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(40);
            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalsTable.setWidths(new float[]{50, 50});

            addTotalRow(totalsTable, "Subtotal:", invoice.getSubtotal().toString(), regularFont);
            if (invoice.getDiscountTotal().compareTo(BigDecimal.ZERO) > 0) {
                addTotalRow(totalsTable, "Discount:", "-" + invoice.getDiscountTotal().toString(), regularFont);
            }
            addTotalRow(totalsTable, "VAT Total:", invoice.getTaxTotal().toString(), regularFont);
            addTotalRow(totalsTable, "Grand Total:", invoice.getTotal().toString() + " " + invoice.getCurrencyCode(), boldFont);
            addTotalRow(totalsTable, "Amount Paid:", invoice.getAmountPaid().toString(), regularFont);
            addTotalRow(totalsTable, "Balance Due:", invoice.getBalanceDue().toString() + " " + invoice.getCurrencyCode(), boldFont);

            document.add(totalsTable);

            if (invoice.getNotes() != null && !invoice.getNotes().isBlank()) {
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Notes & Terms:", boldFont));
                document.add(new Paragraph(invoice.getNotes(), regularFont));
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception ex) {
            throw new BusinessException("PDF_GENERATION_FAILED", "Failed to generate invoice PDF: " + ex.getMessage());
        }
    }

    private PdfPCell createTableCell(String text, int alignment, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        cell.setBorderColor(new Color(226, 232, 240));
        return cell;
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, font));
        lCell.setBorder(Rectangle.NO_BORDER);
        lCell.setPadding(3);
        table.addCell(lCell);

        PdfPCell vCell = new PdfPCell(new Phrase(value, font));
        vCell.setBorder(Rectangle.NO_BORDER);
        vCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vCell.setPadding(3);
        table.addCell(vCell);
    }

    private void populateAndCalculateLines(Invoice invoice, List<InvoiceLineRequest> lineRequests, Long companyId) {
        short lineNo = 1;
        List<TaxCalculator.CalculatedLine> calculatedLines = new ArrayList<>();

        for (InvoiceLineRequest lr : lineRequests) {
            Account account = accountRepository.findByCompanyIdAndId(companyId, lr.accountId())
                    .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "Account not found for id: " + lr.accountId()));
            if (Boolean.FALSE.equals(account.getIsActive())) {
                throw new BusinessException("ACCOUNT_INACTIVE", "Cannot select inactive account: " + account.getCode());
            }
            if (Boolean.TRUE.equals(account.getIsGroup())) {
                throw new BusinessException("GROUP_ACCOUNT_NOT_ALLOWED", "Cannot post to group account: " + account.getCode());
            }

            BigDecimal taxRateValue = BigDecimal.ZERO;
            if (lr.taxRateId() != null) {
                TaxRate taxRate = taxRateRepository.findByCompanyIdAndId(companyId, lr.taxRateId())
                        .orElseThrow(() -> new BusinessException("TAX_RATE_NOT_FOUND", "Tax rate not found for id: " + lr.taxRateId()));
                if (Boolean.FALSE.equals(taxRate.getIsActive())) {
                    throw new BusinessException("TAX_RATE_INACTIVE", "Tax rate is inactive: " + taxRate.getCode());
                }
                taxRateValue = taxRate.getRate();
            }

            TaxCalculator.CalculatedLine calc = taxCalculator.calculateLine(
                    lr.quantity(),
                    lr.unitPrice(),
                    lr.discountPct(),
                    taxRateValue
            );
            calculatedLines.add(calc);

            InvoiceLine line = new InvoiceLine();
            line.setLineNo(lineNo++);
            line.setItemId(lr.itemId());
            line.setDescription(lr.description());
            line.setQuantity(lr.quantity());
            line.setUnitPrice(lr.unitPrice().setScale(2, RoundingMode.HALF_UP));
            line.setDiscountPct(lr.discountPct() != null ? lr.discountPct().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2));
            line.setTaxRateId(lr.taxRateId());
            line.setTaxAmount(calc.taxAmount());
            line.setLineTotal(calc.lineTotal());
            line.setAccountId(lr.accountId());
            line.setWarehouseId(lr.warehouseId());
            line.setCostCenterId(lr.costCenterId());

            invoice.addLine(line);
        }

        TaxCalculator.CalculatedDocumentTotals totals = taxCalculator.calculateDocumentTotals(calculatedLines);
        invoice.setSubtotal(totals.subtotal());
        invoice.setDiscountTotal(totals.discountTotal());
        invoice.setTaxTotal(totals.taxTotal());
        invoice.setTotal(totals.grandTotal());
        invoice.setAmountPaid(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        invoice.setBalanceDue(totals.grandTotal());
    }

    private InvoiceDto mapToDtoWithCustomer(Invoice invoice, Customer customer) {
        InvoiceDto dto = salesMapper.toInvoiceDto(invoice);
        String custName = customer != null ? customer.getName() : null;
        String custCode = customer != null ? customer.getCode() : null;
        List<InvoiceLineDto> lineDtos = salesMapper.toInvoiceLineDtoList(invoice.getLines());

        return new InvoiceDto(
                dto.id(),
                dto.companyId(),
                dto.invoiceNo(),
                dto.docType(),
                dto.customerId(),
                custName,
                custCode,
                dto.invoiceDate(),
                dto.dueDate(),
                dto.currencyCode(),
                dto.exchangeRate(),
                dto.subtotal(),
                dto.discountTotal(),
                dto.taxTotal(),
                dto.total(),
                dto.amountPaid(),
                dto.balanceDue(),
                dto.status(),
                dto.notes(),
                dto.quotationId(),
                dto.salespersonUserId(),
                dto.journalEntryId(),
                lineDtos
        );
    }
}

