package com.backend.finledger.config;

import com.backend.finledger.company.Company;
import com.backend.finledger.company.CompanyRepository;
import com.backend.finledger.currency.Currency;
import com.backend.finledger.currency.CurrencyRepository;
import com.backend.finledger.ledger.Account;
import com.backend.finledger.ledger.AccountRepository;
import com.backend.finledger.ledger.AccountType;
import com.backend.finledger.ledger.NormalBalance;
import com.backend.finledger.tax.TaxRate;
import com.backend.finledger.tax.TaxRateRepository;
import com.backend.finledger.tax.TaxType;
import com.backend.finledger.user.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final CurrencyRepository currencyRepository;
    private final CompanyRepository companyRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final TaxRateRepository taxRateRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            CurrencyRepository currencyRepository,
            CompanyRepository companyRepository,
            PermissionRepository permissionRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            TaxRateRepository taxRateRepository,
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder) {
        this.currencyRepository = currencyRepository;
        this.companyRepository = companyRepository;
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.taxRateRepository = taxRateRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Starting FinLedger master data and RBAC seeding...");
        seedCurrencies();
        Map<String, Permission> permissions = seedPermissions();
        Company defaultCompany = seedDefaultCompany();
        Map<String, Role> roles = seedDefaultRoles(defaultCompany, permissions);
        seedAdminUser(defaultCompany, roles.get("ADMIN"));
        seedTaxRates(defaultCompany);
        seedChartOfAccounts(defaultCompany);
        log.info("FinLedger master data and RBAC seeding completed.");
    }

    private void seedCurrencies() {
        List<Currency> currencies = List.of(
                new Currency("AED", "UAE Dirham", "AED", (byte) 2),
                new Currency("SAR", "Saudi Riyal", "SAR", (byte) 2),
                new Currency("USD", "US Dollar", "$", (byte) 2),
                new Currency("EUR", "Euro", "€", (byte) 2),
                new Currency("GBP", "British Pound", "£", (byte) 2)
        );

        for (Currency c : currencies) {
            if (!currencyRepository.existsById(c.getCode())) {
                currencyRepository.save(c);
            }
        }
    }

    private Map<String, Permission> seedPermissions() {
        Map<String, Permission> map = new HashMap<>();
        List<Permission> list = List.of(
                // Company / Settings
                new Permission("COMPANY_VIEW", "COMPANY", "View company details"),
                new Permission("COMPANY_EDIT", "COMPANY", "Edit company profile"),
                new Permission("SETTINGS_EDIT", "SETTINGS", "Manage global system settings"),

                // Users & Roles
                new Permission("USER_VIEW", "USERS", "View users"),
                new Permission("USER_CREATE", "USERS", "Create users"),
                new Permission("USER_EDIT", "USERS", "Edit users"),
                new Permission("USER_DELETE", "USERS", "Delete users"),
                new Permission("ROLE_VIEW", "ROLES", "View roles"),
                new Permission("ROLE_EDIT", "ROLES", "Edit roles"),

                // General Ledger & Accounts
                new Permission("ACCOUNT_VIEW", "ACCOUNTING", "View chart of accounts"),
                new Permission("ACCOUNT_CREATE", "ACCOUNTING", "Create account"),
                new Permission("ACCOUNT_EDIT", "ACCOUNTING", "Edit account"),
                new Permission("ACCOUNT_DELETE", "ACCOUNTING", "Delete account"),
                new Permission("JOURNAL_VIEW", "ACCOUNTING", "View journals"),
                new Permission("JOURNAL_CREATE", "ACCOUNTING", "Create journals"),
                new Permission("JOURNAL_POST", "ACCOUNTING", "Post journals"),
                new Permission("JOURNAL_VOID", "ACCOUNTING", "Void journals"),
                new Permission("LEDGER_VIEW", "ACCOUNTING", "General ledger enquiry"),

                // Sales & Invoices
                new Permission("CUSTOMER_VIEW", "SALES", "View customers"),
                new Permission("CUSTOMER_CREATE", "SALES", "Create customers"),
                new Permission("CUSTOMER_EDIT", "SALES", "Edit customers"),
                new Permission("INVOICE_VIEW", "SALES", "View sales invoices"),
                new Permission("INVOICE_CREATE", "SALES", "Create sales invoices"),
                new Permission("INVOICE_SEND", "SALES", "Send and post invoices"),
                new Permission("INVOICE_VOID", "SALES", "Void invoices"),
                new Permission("RECEIPT_VIEW", "SALES", "View receipts"),
                new Permission("RECEIPT_CREATE", "SALES", "Create and post receipts"),
                new Permission("RECEIPT_VOID", "SALES", "Void receipts"),

                // Purchases & Payables
                new Permission("VENDOR_VIEW", "PURCHASES", "View vendors"),
                new Permission("VENDOR_CREATE", "PURCHASES", "Create vendors"),
                new Permission("VENDOR_EDIT", "PURCHASES", "Edit vendors"),
                new Permission("BILL_VIEW", "PURCHASES", "View bills"),
                new Permission("BILL_CREATE", "PURCHASES", "Create bills"),
                new Permission("BILL_APPROVE", "PURCHASES", "Approve bills"),
                new Permission("BILL_VOID", "PURCHASES", "Void bills"),
                new Permission("PAYMENT_VIEW", "PURCHASES", "View payments"),
                new Permission("PAYMENT_CREATE", "PURCHASES", "Create payments"),
                new Permission("PAYMENT_VOID", "PURCHASES", "Void payments"),

                // Inventory
                new Permission("ITEM_VIEW", "INVENTORY", "View items and stock balances"),
                new Permission("ITEM_CREATE", "INVENTORY", "Create items"),
                new Permission("ITEM_EDIT", "INVENTORY", "Edit items"),
                new Permission("STOCK_ADJUST", "INVENTORY", "Stock count adjustments"),
                new Permission("STOCK_TRANSFER", "INVENTORY", "Warehouse transfers"),

                // Expenses
                new Permission("EXPENSE_VIEW", "EXPENSES", "View expenses"),
                new Permission("EXPENSE_CREATE", "EXPENSES", "Create expense claims"),
                new Permission("EXPENSE_APPROVE", "EXPENSES", "Approve expense claims"),
                new Permission("EXPENSE_REIMBURSE", "EXPENSES", "Reimburse approved expenses"),

                // CRM
                new Permission("LEAD_VIEW", "CRM", "View leads"),
                new Permission("LEAD_CREATE", "CRM", "Create leads"),
                new Permission("LEAD_EDIT", "CRM", "Edit leads"),
                new Permission("LEAD_CONVERT", "CRM", "Convert lead to customer"),
                new Permission("OPPORTUNITY_VIEW", "CRM", "View opportunities pipeline"),
                new Permission("OPPORTUNITY_CREATE", "CRM", "Create opportunity"),
                new Permission("OPPORTUNITY_EDIT", "CRM", "Edit opportunity"),
                new Permission("QUOTATION_VIEW", "CRM", "View quotations"),
                new Permission("QUOTATION_CREATE", "CRM", "Create quotations"),
                new Permission("QUOTATION_SEND", "CRM", "Send quotation"),
                new Permission("QUOTATION_CONVERT", "CRM", "Convert quotation to invoice"),
                new Permission("CRM_VIEW_ALL", "CRM", "View all CRM records"),

                // Banking & Reports
                new Permission("BANK_RECONCILE", "BANKING", "Perform bank reconciliation"),
                new Permission("REPORT_VIEW", "REPORTS", "View financial and operational reports"),
                new Permission("TAX_RATE_VIEW", "TAX", "View tax rates"),
                new Permission("TAX_RATE_EDIT", "TAX", "Edit tax rates"),
                new Permission("CURRENCY_VIEW", "CURRENCY", "View currencies"),
                new Permission("CURRENCY_EDIT", "CURRENCY", "Edit exchange rates")
        );

        for (Permission p : list) {
            Permission saved = permissionRepository.findByCode(p.getCode())
                    .orElseGet(() -> permissionRepository.save(p));
            map.put(saved.getCode(), saved);
        }
        return map;
    }

    private Company seedDefaultCompany() {
        return companyRepository.findByCode("BSB").orElseGet(() -> {
            Company company = new Company();
            company.setCode("BSB");
            company.setLegalName("BSB Trading LLC");
            company.setTrn("100234567800003");
            company.setCountryCode("AE");
            company.setBaseCurrency("AED");
            company.setFiscalYearStartMonth((byte) 1);
            company.setAddress("Office 1204, Business Bay, Dubai, UAE");
            company.setEmail("accounts@bsbtrading.ae");
            company.setPhone("+971 4 000 0000");
            company.setIsActive(true);
            return companyRepository.save(company);
        });
    }

    private Map<String, Role> seedDefaultRoles(Company company, Map<String, Permission> perms) {
        Map<String, Role> roleMap = new HashMap<>();

        // Role 1: ADMIN - Everything
        Role admin = getOrCreateRole(company.getId(), "ADMIN", "Owner / IT Administrator with all permissions", new HashSet<>(perms.values()));
        roleMap.put("ADMIN", admin);

        // Role 2: ACCOUNTANT - GL, journals, posting, banking, reports, approve expenses
        Set<Permission> accountantPerms = collectPerms(perms,
                "COMPANY_VIEW", "ACCOUNT_VIEW", "ACCOUNT_CREATE", "ACCOUNT_EDIT",
                "JOURNAL_VIEW", "JOURNAL_CREATE", "JOURNAL_POST", "JOURNAL_VOID", "LEDGER_VIEW",
                "INVOICE_VIEW", "RECEIPT_VIEW", "BILL_VIEW", "PAYMENT_VIEW",
                "EXPENSE_VIEW", "EXPENSE_APPROVE", "EXPENSE_REIMBURSE",
                "BANK_RECONCILE", "REPORT_VIEW", "TAX_RATE_VIEW", "CURRENCY_VIEW"
        );
        Role accountant = getOrCreateRole(company.getId(), "ACCOUNTANT", "Finance team: GL, posting, banking, and reports", accountantPerms);
        roleMap.put("ACCOUNTANT", accountant);

        // Role 3: SALES_REP - Leads, opportunities, activities, quotations, create draft invoices
        Set<Permission> salesRepPerms = collectPerms(perms,
                "LEAD_VIEW", "LEAD_CREATE", "LEAD_EDIT",
                "OPPORTUNITY_VIEW", "OPPORTUNITY_CREATE", "OPPORTUNITY_EDIT",
                "QUOTATION_VIEW", "QUOTATION_CREATE", "QUOTATION_SEND",
                "CUSTOMER_VIEW", "ITEM_VIEW", "INVOICE_CREATE"
        );
        Role salesRep = getOrCreateRole(company.getId(), "SALES_REP", "Sales staff: leads, pipeline, quotes and draft invoices", salesRepPerms);
        roleMap.put("SALES_REP", salesRep);

        // Role 4: SALES_MANAGER - All CRM, convert quotations, pipeline reports
        Set<Permission> salesManagerPerms = new HashSet<>(salesRepPerms);
        salesManagerPerms.addAll(collectPerms(perms, "CRM_VIEW_ALL", "LEAD_CONVERT", "QUOTATION_CONVERT", "CUSTOMER_CREATE", "CUSTOMER_EDIT", "REPORT_VIEW"));
        Role salesManager = getOrCreateRole(company.getId(), "SALES_MANAGER", "Sales head: all CRM records, pipeline and conversions", salesManagerPerms);
        roleMap.put("SALES_MANAGER", salesManager);

        // Role 5: AR_CLERK - Invoices and receipts
        Set<Permission> arClerkPerms = collectPerms(perms,
                "CUSTOMER_VIEW", "CUSTOMER_CREATE", "CUSTOMER_EDIT",
                "INVOICE_VIEW", "INVOICE_CREATE", "INVOICE_SEND",
                "RECEIPT_VIEW", "RECEIPT_CREATE",
                "ITEM_VIEW", "TAX_RATE_VIEW"
        );
        Role arClerk = getOrCreateRole(company.getId(), "AR_CLERK", "Finance assistant: invoices and receipts", arClerkPerms);
        roleMap.put("AR_CLERK", arClerk);

        // Role 6: AP_CLERK - Bills and payments
        Set<Permission> apClerkPerms = collectPerms(perms,
                "VENDOR_VIEW", "VENDOR_CREATE", "VENDOR_EDIT",
                "BILL_VIEW", "BILL_CREATE", "BILL_APPROVE",
                "PAYMENT_VIEW", "PAYMENT_CREATE",
                "ITEM_VIEW", "TAX_RATE_VIEW"
        );
        Role apClerk = getOrCreateRole(company.getId(), "AP_CLERK", "Finance assistant: bills and vendor payments", apClerkPerms);
        roleMap.put("AP_CLERK", apClerk);

        // Role 7: STOREKEEPER - Items, stock view, adjustments, transfers
        Set<Permission> storekeeperPerms = collectPerms(perms,
                "ITEM_VIEW", "ITEM_CREATE", "ITEM_EDIT", "STOCK_ADJUST", "STOCK_TRANSFER"
        );
        Role storekeeper = getOrCreateRole(company.getId(), "STOREKEEPER", "Warehouse staff: stock adjustments and transfers", storekeeperPerms);
        roleMap.put("STOREKEEPER", storekeeper);

        // Role 8: EMPLOYEE - Expense claims
        Set<Permission> employeePerms = collectPerms(perms,
                "EXPENSE_VIEW", "EXPENSE_CREATE"
        );
        Role employee = getOrCreateRole(company.getId(), "EMPLOYEE", "All staff: submit and view personal expense claims", employeePerms);
        roleMap.put("EMPLOYEE", employee);

        // Role 9: VIEWER - Read-only reports & documents
        Set<Permission> viewerPerms = collectPerms(perms,
                "REPORT_VIEW", "COMPANY_VIEW", "ACCOUNT_VIEW", "JOURNAL_VIEW",
                "INVOICE_VIEW", "BILL_VIEW", "ITEM_VIEW", "CUSTOMER_VIEW", "VENDOR_VIEW"
        );
        Role viewer = getOrCreateRole(company.getId(), "VIEWER", "Auditor / Management read-only access", viewerPerms);
        roleMap.put("VIEWER", viewer);

        return roleMap;
    }

    private Role getOrCreateRole(Long companyId, String name, String desc, Set<Permission> permissions) {
        return roleRepository.findByCompanyIdAndName(companyId, name).orElseGet(() -> {
            Role role = new Role(companyId, name, desc);
            role.setPermissions(permissions);
            return roleRepository.save(role);
        });
    }

    private Set<Permission> collectPerms(Map<String, Permission> perms, String... codes) {
        Set<Permission> result = new HashSet<>();
        for (String c : codes) {
            if (perms.containsKey(c)) {
                result.add(perms.get(c));
            }
        }
        return result;
    }

    private void seedAdminUser(Company company, Role adminRole) {
        if (!userRepository.existsByEmail("admin@finledger.com")) {
            User admin = new User();
            admin.setCompanyId(company.getId());
            admin.setEmail("admin@finledger.com");
            admin.setPasswordHash(passwordEncoder.encode("password"));
            admin.setFullName("System Administrator");
            admin.setStatus(UserStatus.ACTIVE);
            admin.setFailedAttempts((byte) 0);
            if (adminRole != null) {
                admin.setRoles(Set.of(adminRole));
            }
            userRepository.save(admin);
            log.info("Default admin user created: admin@finledger.com / password");
        }
    }

    private void seedTaxRates(Company company) {
        List<TaxRate> defaults = List.of(
                new TaxRate(company.getId(), "VAT5", "UAE Standard VAT 5%", new BigDecimal("5.0000"), TaxType.STANDARD),
                new TaxRate(company.getId(), "VAT15", "KSA Standard VAT 15%", new BigDecimal("15.0000"), TaxType.STANDARD),
                new TaxRate(company.getId(), "ZR", "Zero-Rated 0%", new BigDecimal("0.0000"), TaxType.ZERO),
                new TaxRate(company.getId(), "EX", "Exempt 0%", new BigDecimal("0.0000"), TaxType.EXEMPT)
        );

        for (TaxRate tr : defaults) {
            if (taxRateRepository.findByCompanyIdAndCode(company.getId(), tr.getCode()).isEmpty()) {
                taxRateRepository.save(tr);
            }
        }
    }

    private void seedChartOfAccounts(Company company) {
        if (!accountRepository.findByCompanyIdOrderByCodeAsc(company.getId()).isEmpty()) {
            return;
        }

        // Top level groups
        Account assets = saveAccount(company.getId(), "1000", "Assets", AccountType.ASSET, null, null, true, NormalBalance.DEBIT, true);
        Account cashBank = saveAccount(company.getId(), "1100", "Cash & Bank", AccountType.ASSET, "BANK", assets.getId(), true, NormalBalance.DEBIT, true);
        saveAccount(company.getId(), "1110", "Emirates NBD - Current", AccountType.ASSET, "BANK", cashBank.getId(), false, NormalBalance.DEBIT, false);
        saveAccount(company.getId(), "1120", "Petty Cash", AccountType.ASSET, "CASH", cashBank.getId(), false, NormalBalance.DEBIT, false);
        saveAccount(company.getId(), "1200", "Accounts Receivable", AccountType.ASSET, "RECEIVABLE", assets.getId(), false, NormalBalance.DEBIT, true);
        saveAccount(company.getId(), "1300", "Inventory", AccountType.ASSET, "STOCK", assets.getId(), false, NormalBalance.DEBIT, true);

        Account liabilities = saveAccount(company.getId(), "2000", "Liabilities", AccountType.LIABILITY, null, null, true, NormalBalance.CREDIT, true);
        saveAccount(company.getId(), "2100", "Accounts Payable", AccountType.LIABILITY, "PAYABLE", liabilities.getId(), false, NormalBalance.CREDIT, true);
        saveAccount(company.getId(), "2200", "VAT Payable (Output)", AccountType.LIABILITY, "TAX", liabilities.getId(), false, NormalBalance.CREDIT, true);
        saveAccount(company.getId(), "2300", "Accrued Expenses", AccountType.LIABILITY, "ACCRUAL", liabilities.getId(), false, NormalBalance.CREDIT, false);

        Account equity = saveAccount(company.getId(), "3000", "Equity", AccountType.EQUITY, null, null, true, NormalBalance.CREDIT, true);
        saveAccount(company.getId(), "3100", "Owner's Capital", AccountType.EQUITY, "EQUITY", equity.getId(), false, NormalBalance.CREDIT, false);
        saveAccount(company.getId(), "3200", "Retained Earnings", AccountType.EQUITY, "EQUITY", equity.getId(), false, NormalBalance.CREDIT, true);

        Account revenue = saveAccount(company.getId(), "4000", "Revenue", AccountType.INCOME, null, null, true, NormalBalance.CREDIT, true);
        saveAccount(company.getId(), "4100", "Product Sales", AccountType.INCOME, "SALES", revenue.getId(), false, NormalBalance.CREDIT, false);
        saveAccount(company.getId(), "4200", "Service Income", AccountType.INCOME, "SERVICE", revenue.getId(), false, NormalBalance.CREDIT, false);

        Account expenses = saveAccount(company.getId(), "5000", "Expenses", AccountType.EXPENSE, null, null, true, NormalBalance.DEBIT, true);
        saveAccount(company.getId(), "5100", "Cost of Goods Sold", AccountType.EXPENSE, "COGS", expenses.getId(), false, NormalBalance.DEBIT, true);
        saveAccount(company.getId(), "5150", "Inventory Adjustments", AccountType.EXPENSE, "ADJUSTMENT", expenses.getId(), false, NormalBalance.DEBIT, false);
        saveAccount(company.getId(), "5200", "Salaries & Wages", AccountType.EXPENSE, "PAYROLL", expenses.getId(), false, NormalBalance.DEBIT, false);
        saveAccount(company.getId(), "5210", "Rent Expense", AccountType.EXPENSE, "RENT", expenses.getId(), false, NormalBalance.DEBIT, false);
    }

    private Account saveAccount(Long companyId, String code, String name, AccountType type, String subType,
                                Long parentId, boolean isGroup, NormalBalance normalBalance, boolean isSystem) {
        Account account = new Account(companyId, code, name, type, subType, parentId, isGroup, normalBalance);
        account.setIsSystem(isSystem);
        account.setIsActive(true);
        return accountRepository.save(account);
    }
}

