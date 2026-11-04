package com.backend.finledger;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.DocType;
import com.backend.finledger.common.NumberSeries;
import com.backend.finledger.common.NumberSeriesRepository;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.crm.dto.*;
import com.backend.finledger.crm.entity.*;
import com.backend.finledger.crm.repository.*;
import com.backend.finledger.crm.service.*;
import com.backend.finledger.inventory.entity.*;
import com.backend.finledger.inventory.repository.ItemRepository;
import com.backend.finledger.inventory.repository.StockBalanceRepository;
import com.backend.finledger.inventory.repository.WarehouseRepository;
import com.backend.finledger.ledger.entity.Account;
import com.backend.finledger.ledger.entity.AccountType;
import com.backend.finledger.ledger.entity.NormalBalance;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.sales.entity.Customer;
import com.backend.finledger.sales.repository.CustomerRepository;
import com.backend.finledger.security.UserPrincipal;
import com.backend.finledger.tax.entity.TaxRate;
import com.backend.finledger.tax.entity.TaxType;
import com.backend.finledger.tax.repository.TaxRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CrmModuleTest {

    @Autowired
    private LeadService leadService;

    @Autowired
    private LeadConversionService leadConversionService;

    @Autowired
    private OpportunityService opportunityService;

    @Autowired
    private CrmPipelineStageService stageService;

    @Autowired
    private ContactService contactService;

    @Autowired
    private CrmActivityService activityService;

    @Autowired
    private QuotationService quotationService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private CrmPipelineStageRepository stageRepository;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TaxRateRepository taxRateRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private StockBalanceRepository stockBalanceRepository;

    @Autowired
    private NumberSeriesRepository numberSeriesRepository;

    private Long bsbCompanyId;
    private Long arAccountId;
    private Warehouse defaultWarehouse;
    private Item inventoryItem;
    private TaxRate vat5Rate;

    @BeforeEach
    void setUp() {
        Company bsb = companyRepository.findByCode("BSB").orElseThrow();
        bsbCompanyId = bsb.getId();

        authenticateAs(1L, bsbCompanyId, "admin@finledger.com", Set.of(
                "CUSTOMER_VIEW", "CUSTOMER_CREATE", "CUSTOMER_EDIT",
                "INVOICE_VIEW", "INVOICE_CREATE", "INVOICE_SEND",
                "CRM_VIEW", "CRM_EDIT", "LEAD_VIEW", "LEAD_EDIT",
                "OPPORTUNITY_VIEW", "OPPORTUNITY_EDIT", "QUOTATION_VIEW", "QUOTATION_EDIT",
                "REPORT_VIEW", "JOURNAL_VIEW", "JOURNAL_CREATE", "JOURNAL_POST",
                "SETTINGS_EDIT", "ACCOUNT_VIEW", "ACCOUNT_EDIT"
        ));

        // Number Series
        ensureNumberSeries(bsbCompanyId, DocType.LEAD, "LEAD-", 1);
        ensureNumberSeries(bsbCompanyId, DocType.OPPORTUNITY, "OPP-", 1);
        ensureNumberSeries(bsbCompanyId, DocType.QUOTATION, "QT-", 1);
        ensureNumberSeries(bsbCompanyId, DocType.INVOICE, "INV-", 1);

        // Accounts
        Account arAcc = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "1200").orElseGet(() -> {
            Account a = new Account(bsbCompanyId, "1200", "Accounts Receivable", AccountType.ASSET, "RECEIVABLE", null, false, NormalBalance.DEBIT);
            return accountRepository.save(a);
        });
        arAccountId = arAcc.getId();

        Account revAcc = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "4100").orElseGet(() -> {
            Account a = new Account(bsbCompanyId, "4100", "Product Sales", AccountType.INCOME, "REVENUE", null, false, NormalBalance.CREDIT);
            return accountRepository.save(a);
        });

        // Tax Rate
        vat5Rate = taxRateRepository.findByCompanyIdAndCode(bsbCompanyId, "VAT5").orElseGet(() -> {
            TaxRate tr = new TaxRate();
            tr.setCompanyId(bsbCompanyId);
            tr.setCode("VAT5");
            tr.setName("Standard UAE VAT 5%");
            tr.setRate(new BigDecimal("5.00"));
            tr.setTaxType(TaxType.STANDARD);
            tr.setIsActive(true);
            return taxRateRepository.save(tr);
        });

        // Warehouse
        defaultWarehouse = warehouseRepository.findFirstByCompanyIdAndIsDefaultTrue(bsbCompanyId).orElseGet(() -> {
            Warehouse w = new Warehouse();
            w.setCompanyId(bsbCompanyId);
            w.setCode("WH-MAIN");
            w.setName("Main Dubai Warehouse");
            w.setIsDefault(true);
            w.setIsActive(true);
            return warehouseRepository.save(w);
        });

        // Item
        inventoryItem = itemRepository.findByCompanyIdAndSku(bsbCompanyId, "CRM-LAPTOP-01").orElseGet(() -> {
            Item item = new Item();
            item.setCompanyId(bsbCompanyId);
            item.setSku("CRM-LAPTOP-01");
            item.setName("High Performance Laptop");
            item.setItemType(ItemType.INVENTORY);
            item.setUnit("PCS");
            item.setSalesPrice(new BigDecimal("4500.00"));
            item.setPurchasePrice(new BigDecimal("3500.00"));
            item.setIncomeAccountId(revAcc.getId());
            item.setTaxRateId(vat5Rate.getId());
            item.setIsActive(true);
            return itemRepository.save(item);
        });
    }

    private void ensureNumberSeries(Long companyId, DocType docType, String prefix, Integer nextVal) {
        if (numberSeriesRepository.findByCompanyIdAndDocType(companyId, docType).isEmpty()) {
            NumberSeries ns = new NumberSeries();
            ns.setCompanyId(companyId);
            ns.setDocType(docType);
            ns.setPrefix(prefix);
            ns.setNextNumber(nextVal);
            ns.setPadding((byte) 5);
            numberSeriesRepository.save(ns);
        }
    }

    private void authenticateAs(Long userId, Long companyId, String email, Set<String> perms) {
        UserPrincipal principal = new UserPrincipal(
                userId,
                companyId,
                email,
                "",
                "Test User",
                true,
                perms
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @Transactional
    @DisplayName("1. Lead CRUD and CSV Import")
    void testLeadCrudAndCsvImport() {
        LeadRequest req = new LeadRequest(
                "Acme Trading",
                "John Doe",
                "john@acmetrading.com",
                "+971501234567",
                LeadSource.WEBSITE,
                "Trading",
                "Dubai",
                "AE",
                null,
                "High priority prospect from trade show"
        );

        LeadDto lead = leadService.createLead(req);
        assertNotNull(lead.id());
        assertNotNull(lead.leadNo());
        assertEquals("Acme Trading", lead.companyName());
        assertEquals(LeadStatus.NEW, lead.status());

        // Import CSV
        String csvContent = "companyName,contactName,email,phone,source,industry,city,countryCode,notes\n" +
                "Alpha Corp,Alice Smith,alice@alphacorp.com,+971509998888,REFERRAL,Technology,Dubai,AE,Referral lead\n" +
                "Beta Logistics,Bob Miller,bob@betalog.com,+971507776666,EXHIBITION,Logistics,Sharjah,AE,Exhibition lead\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "leads.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        LeadImportResultDto importResult = leadService.importLeadsFromCsv(file);
        assertEquals(2, importResult.totalProcessed());
        assertEquals(2, importResult.importedCount());
        assertEquals(0, importResult.skippedCount());
    }

    @Test
    @Transactional
    @DisplayName("2. Lead Qualification into Opportunity")
    void testLeadQualification() {
        LeadRequest req = new LeadRequest(
                "Gulf Tech LLC",
                "Sami Khalid",
                "sami@gulftech.ae",
                "+971505554433",
                LeadSource.COLD_CALL,
                "IT Services",
                "Abu Dhabi",
                "AE",
                null,
                "Ready for proposal"
        );
        LeadDto lead = leadService.createLead(req);

        OpportunityDto opp = leadService.qualifyLead(lead.id());
        assertNotNull(opp);
        assertNotNull(opp.opportunityNo());
        assertEquals(lead.id(), opp.leadId());
        assertEquals("Gulf Tech LLC - Deal", opp.title());

        LeadDto updatedLead = leadService.getLeadById(lead.id());
        assertEquals(LeadStatus.QUALIFIED, updatedLead.status());
    }

    @Test
    @Transactional
    @DisplayName("3. Lead Conversion to Customer & Primary Contact with TRN/Email deduplication")
    void testLeadConversionService() {
        String testEmail = "procurement@apexholdings.ae";

        LeadRequest req = new LeadRequest(
                "Apex Holdings LLC",
                "Majid Al-Mansoor",
                testEmail,
                "+971521112233",
                LeadSource.EXHIBITION,
                "Real Estate",
                "Dubai",
                "AE",
                null,
                "Convert immediately"
        );
        LeadDto lead = leadService.createLead(req);

        // 1. Initial conversion
        LeadConvertResponse resp = leadConversionService.convertLead(lead.id(),
                new LeadConvertRequest(null, null, null, null, null, null));

        assertFalse(resp.matchedExistingCustomer());
        assertNotNull(resp.customerId());
        assertNotNull(resp.contactId());

        // Verify Customer entity
        Customer customer = customerRepository.findById(resp.customerId()).orElseThrow();
        assertEquals("Apex Holdings LLC", customer.getName());
        assertEquals(testEmail, customer.getEmail());
        assertNotNull(customer.getArAccountId());

        // Verify Contact entity
        Contact contact = contactRepository.findById(resp.contactId()).orElseThrow();
        assertEquals("Majid Al-Mansoor", contact.getFullName());
        assertTrue(contact.getIsPrimary());
        assertEquals(customer.getId(), contact.getCustomerId());

        // Verify Lead status
        Lead convertedLead = leadRepository.findById(lead.id()).orElseThrow();
        assertEquals(LeadStatus.CONVERTED, convertedLead.getStatus());

        // 2. Duplicate Check: Create another lead with the same email
        LeadRequest req2 = new LeadRequest(
                "Apex Holdings Branch",
                "Omar Al-Mansoor",
                testEmail,
                "+971521112244",
                LeadSource.REFERRAL,
                "Real Estate",
                "Dubai",
                "AE",
                null,
                "Duplicate email test"
        );
        LeadDto lead2 = leadService.createLead(req2);

        // Conversion should detect existing customer without creating a duplicate
        LeadConvertResponse resp2 = leadConversionService.convertLead(lead2.id(),
                new LeadConvertRequest(null, null, null, null, null, null));

        assertTrue(resp2.matchedExistingCustomer());
        assertEquals(customer.getId(), resp2.customerId());

        Lead convertedLead2 = leadRepository.findById(lead2.id()).orElseThrow();
        assertEquals(LeadStatus.CONVERTED, convertedLead2.getStatus());
    }

    @Test
    @Transactional
    @DisplayName("4. Opportunity Stage Rules (WON requires customer, LOST requires lostReason)")
    void testOpportunityStageRules() {
        stageService.initDefaultStagesIfEmpty(bsbCompanyId);
        List<CrmPipelineStageDto> stages = stageService.getStages();

        CrmPipelineStageDto wonStage = stages.stream()
                .filter(s -> s.stageType() == StageType.WON)
                .findFirst()
                .orElseThrow();

        CrmPipelineStageDto lostStage = stages.stream()
                .filter(s -> s.stageType() == StageType.LOST)
                .findFirst()
                .orElseThrow();

        CrmPipelineStageDto openStage = stages.stream()
                .filter(s -> s.stageType() == StageType.OPEN)
                .findFirst()
                .orElseThrow();

        // Create opportunity without customer
        OpportunityRequest oppReq = new OpportunityRequest(
                "Enterprise ERP Deal",
                null,
                null,
                null,
                openStage.id(),
                new BigDecimal("500000.00"),
                "AED",
                (byte) 50,
                LocalDate.now().plusMonths(2),
                null
        );
        OpportunityDto opp = opportunityService.createOpportunity(oppReq);
        assertNull(opp.closedAt());

        // 1. Moving to WON without customer MUST fail with CUSTOMER_REQUIRED_FOR_WON
        BusinessException wonEx = assertThrows(BusinessException.class, () ->
                opportunityService.updateOpportunityStage(opp.id(), new OpportunityStageRequest(wonStage.id(), null)));
        assertEquals("CUSTOMER_REQUIRED_FOR_WON", wonEx.getCode());

        // 2. Moving to LOST without lostReason MUST fail with LOST_REASON_REQUIRED
        BusinessException lostEx = assertThrows(BusinessException.class, () ->
                opportunityService.updateOpportunityStage(opp.id(), new OpportunityStageRequest(lostStage.id(), null)));
        assertEquals("LOST_REASON_REQUIRED", lostEx.getCode());

        // 3. Moving to LOST with lostReason MUST succeed and set closedAt
        OpportunityDto lostOpp = opportunityService.updateOpportunityStage(
                opp.id(), new OpportunityStageRequest(lostStage.id(), "Competitor pricing 20% lower"));
        assertEquals(wonStage.id().equals(lostOpp.stageId()), false);
        assertEquals(lostStage.id(), lostOpp.stageId());
        assertEquals("Competitor pricing 20% lower", lostOpp.lostReason());
        assertNotNull(lostOpp.closedAt());

        // 4. Moving back to OPEN clears closedAt
        OpportunityDto openOpp = opportunityService.updateOpportunityStage(
                opp.id(), new OpportunityStageRequest(openStage.id(), null));
        assertNull(openOpp.closedAt());

        // 5. Link customer and move to WON
        Customer cust = new Customer();
        cust.setCompanyId(bsbCompanyId);
        cust.setCode("CUST-OPP-01");
        cust.setName("Enterprise Client Ltd");
        cust.setCurrencyCode("AED");
        cust.setArAccountId(arAccountId);
        cust = customerRepository.save(cust);

        Opportunity oppEntity = opportunityRepository.findById(opp.id()).orElseThrow();
        oppEntity.setCustomerId(cust.getId());
        opportunityRepository.save(oppEntity);

        OpportunityDto wonOpp = opportunityService.updateOpportunityStage(
                opp.id(), new OpportunityStageRequest(wonStage.id(), null));
        assertEquals(wonStage.id(), wonOpp.stageId());
        assertNotNull(wonOpp.closedAt());
    }

    @Test
    @Transactional
    @DisplayName("5. Quotation Workflow, Inventory Stock Reservation, and Invoice Conversion")
    void testQuotationWorkflowAndStockReservation() {
        Customer cust = new Customer();
        cust.setCompanyId(bsbCompanyId);
        cust.setCode("CUST-QT-01");
        cust.setName("Emirates Corporate Supplies");
        cust.setCurrencyCode("AED");
        cust.setArAccountId(arAccountId);
        cust = customerRepository.save(cust);

        // Check initial stock reservation
        StockBalance initialBalance = stockBalanceRepository.findByItemIdAndWarehouseId(inventoryItem.getId(), defaultWarehouse.getId())
                .orElseGet(() -> new StockBalance(inventoryItem.getId(), defaultWarehouse.getId()));
        BigDecimal initialReserved = initialBalance.getQtyReserved() != null ? initialBalance.getQtyReserved() : BigDecimal.ZERO;

        // 1. Create Quotation
        QuotationLineRequest lineReq = new QuotationLineRequest(
                (short) 1,
                inventoryItem.getId(),
                inventoryItem.getName(),
                new BigDecimal("5.0000"),
                new BigDecimal("4500.00"),
                BigDecimal.ZERO,
                vat5Rate.getId()
        );

        QuotationRequest qtReq = new QuotationRequest(
                null,
                cust.getId(),
                null,
                LocalDate.now(),
                LocalDate.now().plusDays(15),
                "AED",
                List.of(lineReq)
        );

        QuotationDto quotation = quotationService.createQuotation(qtReq);
        assertNotNull(quotation.id());
        assertNotNull(quotation.quotationNo());
        assertEquals(QuotationStatus.DRAFT, quotation.status());
        assertEquals(new BigDecimal("22500.00"), quotation.subtotal());
        assertEquals(new BigDecimal("1125.00"), quotation.taxTotal());
        assertEquals(new BigDecimal("23625.00"), quotation.total());

        // 2. Send Quotation
        QuotationDto sentQt = quotationService.sendQuotation(quotation.id());
        assertEquals(QuotationStatus.SENT, sentQt.status());

        // 3. Accept Quotation and verify Stock Reservation
        QuotationDto acceptedQt = quotationService.acceptQuotation(quotation.id());
        assertEquals(QuotationStatus.ACCEPTED, acceptedQt.status());

        StockBalance updatedBalance = stockBalanceRepository.findByItemIdAndWarehouseId(inventoryItem.getId(), defaultWarehouse.getId())
                .orElseThrow();
        assertEquals(initialReserved.add(new BigDecimal("5.0000")), updatedBalance.getQtyReserved());

        // 4. Convert Quotation to Invoice
        ConvertToInvoiceResponse invResp = quotationService.convertToInvoice(quotation.id());
        assertNotNull(invResp.invoiceId());
        assertNotNull(invResp.invoiceNo());
        assertEquals(QuotationStatus.INVOICED, invResp.status());

        QuotationDto invoicedQt = quotationService.getQuotationById(quotation.id());
        assertEquals(QuotationStatus.INVOICED, invoicedQt.status());
        assertEquals(invResp.invoiceId(), invoicedQt.invoiceId());

        // 5. Generate PDF
        byte[] pdfBytes = quotationService.generateQuotationPdf(quotation.id());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "Quotation PDF should have non-trivial size");
    }

    @Test
    @Transactional
    @DisplayName("6. CRM Activities Management")
    void testCrmActivities() {
        LeadRequest lReq = new LeadRequest(
                "Activity Test Co",
                "Contact Person",
                "contact@acttest.com",
                null,
                LeadSource.WEBSITE,
                null,
                null,
                null,
                null,
                null
        );
        LeadDto lead = leadService.createLead(lReq);

        CrmActivityRequest actReq = new CrmActivityRequest(
                ActivityType.CALL,
                "Introductory Discovery Call",
                "Discuss enterprise requirements",
                ActivityRelatedType.LEAD,
                lead.id(),
                null,
                null
        );

        CrmActivityDto activity = activityService.createActivity(actReq);
        assertNotNull(activity.id());
        assertNull(activity.completedAt());
        assertEquals("Introductory Discovery Call", activity.subject());

        // Complete Activity
        CrmActivityDto completed = activityService.completeActivity(activity.id());
        assertNotNull(completed.completedAt());

        List<CrmActivityDto> list = activityService.getActivities(ActivityRelatedType.LEAD, lead.id(), null, null);
        assertEquals(1, list.size());
    }
}

