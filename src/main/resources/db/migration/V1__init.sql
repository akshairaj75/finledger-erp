-- FinLedger schema - MySQL 8.0 - Flyway V1__init.sql
SET NAMES utf8mb4; 
SET FOREIGN_KEY_CHECKS = 0; 
  
-- ===== Organisation & Security ===== 
CREATE TABLE companies ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  code VARCHAR(20) NOT NULL, 
  legal_name VARCHAR(200) NOT NULL, 
  trn VARCHAR(20) DEFAULT NULL, 
  country_code CHAR(2) NOT NULL, 
  base_currency CHAR(3) NOT NULL, 
  fiscal_year_start_month TINYINT NOT NULL DEFAULT 1, 
  address VARCHAR(500) DEFAULT NULL, 
  email VARCHAR(150) DEFAULT NULL, 
  phone VARCHAR(30) DEFAULT NULL, 
  logo_path VARCHAR(255) DEFAULT NULL, 
  is_active TINYINT(1) NOT NULL DEFAULT 1, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_companies_code (code), 
  CONSTRAINT fk_companies_base_currency FOREIGN KEY (base_currency) REFERENCES currencies(code), 
  CONSTRAINT fk_companies_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_companies_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE users ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  email VARCHAR(150) NOT NULL, 
  password_hash VARCHAR(100) NOT NULL, 
  full_name VARCHAR(150) NOT NULL, 
  status ENUM('ACTIVE','LOCKED','DISABLED') NOT NULL DEFAULT 'ACTIVE', 
  failed_attempts TINYINT NOT NULL DEFAULT 0, 
  last_login_at DATETIME(3) DEFAULT NULL, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_users_email (email), 
  KEY ix_users_company (company_id), 
  CONSTRAINT fk_users_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_users_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_users_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE roles ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  name VARCHAR(50) NOT NULL, 
  description VARCHAR(255) DEFAULT NULL, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_roles (company_id, name), 
  CONSTRAINT fk_roles_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_roles_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_roles_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE permissions ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  code VARCHAR(80) NOT NULL, 
  module VARCHAR(40) NOT NULL, 
  description VARCHAR(255) DEFAULT NULL, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_permissions_code (code) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE role_permissions ( 
  role_id BIGINT UNSIGNED NOT NULL, 
  permission_id BIGINT UNSIGNED NOT NULL, 
  PRIMARY KEY (role_id, permission_id), 
  CONSTRAINT fk_role_permissions_role_id FOREIGN KEY (role_id) REFERENCES roles(id), 
  CONSTRAINT fk_role_permissions_permission_id FOREIGN KEY (permission_id) REFERENCES 
permissions(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE user_roles ( 
  user_id BIGINT UNSIGNED NOT NULL, 
  role_id BIGINT UNSIGNED NOT NULL, 
  PRIMARY KEY (user_id, role_id), 
  CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users(id), 
  CONSTRAINT fk_user_roles_role_id FOREIGN KEY (role_id) REFERENCES roles(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE refresh_tokens ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  user_id BIGINT UNSIGNED NOT NULL, 
  token_hash CHAR(64) NOT NULL, 
  expires_at DATETIME(3) NOT NULL, 
  revoked TINYINT(1) NOT NULL DEFAULT 0, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_refresh_token (token_hash), 
  KEY ix_refresh_user (user_id), 
  CONSTRAINT fk_refresh_tokens_user_id FOREIGN KEY (user_id) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE audit_logs ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  user_id BIGINT UNSIGNED DEFAULT NULL, 
  entity_name VARCHAR(60) NOT NULL, 
  entity_id BIGINT UNSIGNED NOT NULL, 
  action ENUM('CREATE','UPDATE','DELETE','POST','VOID','APPROVE','REJECT','CONVERT','LOGIN') NOT 
NULL, 
  old_values JSON DEFAULT NULL, 
  new_values JSON DEFAULT NULL, 
  ip_address VARCHAR(45) DEFAULT NULL, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  PRIMARY KEY (id), 
  KEY ix_audit_entity (entity_name, entity_id), 
  KEY ix_audit_time (company_id, created_at), 
  CONSTRAINT fk_audit_logs_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_audit_logs_user_id FOREIGN KEY (user_id) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
  
-- ===== Setup & Master Data ===== 
CREATE TABLE currencies ( 
  code CHAR(3) NOT NULL, 
  name VARCHAR(50) NOT NULL, 
  symbol VARCHAR(5) DEFAULT NULL, 
  decimal_places TINYINT NOT NULL DEFAULT 2, 
  PRIMARY KEY (code) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE exchange_rates ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  from_currency CHAR(3) NOT NULL, 
  to_currency CHAR(3) NOT NULL, 
  rate_date DATE NOT NULL, 
  rate DECIMAL(18,6) NOT NULL, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_fx (company_id, from_currency, to_currency, rate_date), 
  CONSTRAINT fk_exchange_rates_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_exchange_rates_from_currency FOREIGN KEY (from_currency) REFERENCES 
currencies(code), 
  CONSTRAINT fk_exchange_rates_to_currency FOREIGN KEY (to_currency) REFERENCES currencies(code), 
  CONSTRAINT fk_exchange_rates_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_exchange_rates_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE fiscal_years ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  name VARCHAR(20) NOT NULL, 
  start_date DATE NOT NULL, 
  end_date DATE NOT NULL, 
  status ENUM('OPEN','CLOSED') NOT NULL DEFAULT 'OPEN', 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_fy (company_id, name), 
  CONSTRAINT fk_fiscal_years_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_fiscal_years_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_fiscal_years_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE fiscal_periods ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  fiscal_year_id BIGINT UNSIGNED NOT NULL, 
  period_no TINYINT NOT NULL, 
  start_date DATE NOT NULL, 
  end_date DATE NOT NULL, 
  status ENUM('OPEN','CLOSED','LOCKED') NOT NULL DEFAULT 'OPEN', 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_period (fiscal_year_id, period_no), 
  CONSTRAINT fk_fiscal_periods_fiscal_year_id FOREIGN KEY (fiscal_year_id) REFERENCES 
fiscal_years(id), 
  CONSTRAINT fk_fiscal_periods_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_fiscal_periods_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE tax_rates ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  code VARCHAR(20) NOT NULL, 
  name VARCHAR(80) NOT NULL, 
  rate DECIMAL(7,4) NOT NULL, 
  tax_type ENUM('STANDARD','ZERO','EXEMPT','OUT_OF_SCOPE') NOT NULL DEFAULT 'STANDARD', 
  sales_account_id BIGINT UNSIGNED DEFAULT NULL, 
  purchase_account_id BIGINT UNSIGNED DEFAULT NULL, 
  is_active TINYINT(1) NOT NULL DEFAULT 1, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_tax (company_id, code), 
  CONSTRAINT fk_tax_rates_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_tax_rates_sales_account_id FOREIGN KEY (sales_account_id) REFERENCES accounts(id), 
  CONSTRAINT fk_tax_rates_purchase_account_id FOREIGN KEY (purchase_account_id) REFERENCES 
accounts(id), 
  CONSTRAINT fk_tax_rates_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_tax_rates_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE number_series ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  doc_type 
ENUM('JOURNAL','INVOICE','BILL','RECEIPT','PAYMENT','CREDIT_NOTE','QUOTATION','LEAD','OPPORTUNITY',
'EXPENSE','ADJUSTMENT','TRANSFER') NOT NULL, 
  prefix VARCHAR(20) NOT NULL, 
  next_number INT NOT NULL DEFAULT 1, 
  padding TINYINT NOT NULL DEFAULT 4, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_series (company_id, doc_type), 
  CONSTRAINT fk_number_series_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_number_series_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_number_series_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE cost_centers ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  code VARCHAR(20) NOT NULL, 
  name VARCHAR(100) NOT NULL, 
  parent_id BIGINT UNSIGNED DEFAULT NULL, 
  is_active TINYINT(1) NOT NULL DEFAULT 1, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_cc (company_id, code), 
  CONSTRAINT fk_cost_centers_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_cost_centers_parent_id FOREIGN KEY (parent_id) REFERENCES cost_centers(id), 
  CONSTRAINT fk_cost_centers_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_cost_centers_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
  
-- ===== General Ledger ===== 
CREATE TABLE accounts ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  code VARCHAR(20) NOT NULL, 
  name VARCHAR(150) NOT NULL, 
  account_type ENUM('ASSET','LIABILITY','EQUITY','INCOME','EXPENSE') NOT NULL, 
  sub_type VARCHAR(40) DEFAULT NULL, 
  parent_id BIGINT UNSIGNED DEFAULT NULL, 
  is_group TINYINT(1) NOT NULL DEFAULT 0, 
  normal_balance ENUM('DEBIT','CREDIT') NOT NULL, 
  currency_code CHAR(3) DEFAULT NULL, 
  opening_balance DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  is_system TINYINT(1) NOT NULL DEFAULT 0, 
  is_active TINYINT(1) NOT NULL DEFAULT 1, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_accounts (company_id, code), 
  KEY ix_accounts_parent (parent_id), 
  KEY ix_accounts_type (company_id, account_type), 
  CONSTRAINT fk_accounts_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_accounts_parent_id FOREIGN KEY (parent_id) REFERENCES accounts(id), 
  CONSTRAINT fk_accounts_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_accounts_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE journal_entries ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  entry_no VARCHAR(30) NOT NULL, 
  entry_date DATE NOT NULL, 
  fiscal_period_id BIGINT UNSIGNED NOT NULL, 
  reference VARCHAR(100) DEFAULT NULL, 
  narration VARCHAR(500) DEFAULT NULL, 
  source_type 
ENUM('MANUAL','INVOICE','BILL','RECEIPT','PAYMENT','CREDIT_NOTE','EXPENSE','REIMBURSEMENT','STOCK_A
DJUSTMENT','STOCK_TRANSFER','OPENING','CLOSING') NOT NULL DEFAULT 'MANUAL', 
  source_id BIGINT UNSIGNED DEFAULT NULL, 
  currency_code CHAR(3) NOT NULL, 
  exchange_rate DECIMAL(18,6) NOT NULL DEFAULT 1.000000, 
  total_debit DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  total_credit DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  status ENUM('DRAFT','POSTED','VOID') NOT NULL DEFAULT 'DRAFT', 
  posted_at DATETIME(3) DEFAULT NULL, 
  posted_by BIGINT UNSIGNED DEFAULT NULL, 
  reversal_of_id BIGINT UNSIGNED DEFAULT NULL, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_je_no (company_id, entry_no), 
  KEY ix_je_date (company_id, entry_date), 
  KEY ix_je_source (source_type, source_id), 
  CONSTRAINT fk_journal_entries_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_journal_entries_fiscal_period_id FOREIGN KEY (fiscal_period_id) REFERENCES 
fiscal_periods(id), 
  CONSTRAINT fk_journal_entries_posted_by FOREIGN KEY (posted_by) REFERENCES users(id), 
  CONSTRAINT fk_journal_entries_reversal_of_id FOREIGN KEY (reversal_of_id) REFERENCES 
journal_entries(id), 
  CONSTRAINT fk_journal_entries_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_journal_entries_updated_by FOREIGN KEY (updated_by) REFERENCES users(id), 
  CHECK (status <> 'POSTED' OR total_debit = total_credit) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE journal_lines ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  journal_entry_id BIGINT UNSIGNED NOT NULL, 
  line_no SMALLINT NOT NULL, 
  account_id BIGINT UNSIGNED NOT NULL, 
  cost_center_id BIGINT UNSIGNED DEFAULT NULL, 
  description VARCHAR(255) DEFAULT NULL, 
  debit DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  credit DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  base_debit DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  base_credit DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  party_type ENUM('CUSTOMER','VENDOR') DEFAULT NULL, 
  party_id BIGINT UNSIGNED DEFAULT NULL, 
  is_reconciled TINYINT(1) NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  KEY ix_jl_entry (journal_entry_id), 
  KEY ix_jl_account (account_id), 
  KEY ix_jl_party (party_type, party_id), 
  CONSTRAINT fk_journal_lines_journal_entry_id FOREIGN KEY (journal_entry_id) REFERENCES 
journal_entries(id) ON DELETE CASCADE, 
  CONSTRAINT fk_journal_lines_account_id FOREIGN KEY (account_id) REFERENCES accounts(id), 
  CONSTRAINT fk_journal_lines_cost_center_id FOREIGN KEY (cost_center_id) REFERENCES 
cost_centers(id), 
  CHECK (debit >= 0 AND credit >= 0 AND (debit = 0 OR credit = 0)) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
  
-- ===== CRM ===== 
CREATE TABLE crm_pipeline_stages ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  name VARCHAR(50) NOT NULL, 
  sort_order TINYINT NOT NULL, 
  probability_pct TINYINT NOT NULL DEFAULT 0, 
  stage_type ENUM('OPEN','WON','LOST') NOT NULL DEFAULT 'OPEN', 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_stage (company_id, name), 
  CONSTRAINT fk_crm_pipeline_stages_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_crm_pipeline_stages_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_crm_pipeline_stages_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE leads ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  lead_no VARCHAR(30) NOT NULL, 
  company_name VARCHAR(200) DEFAULT NULL, 
  contact_name VARCHAR(150) NOT NULL, 
  email VARCHAR(150) DEFAULT NULL, 
  phone VARCHAR(30) DEFAULT NULL, 
  source ENUM('WEBSITE','REFERRAL','EXHIBITION','SOCIAL','COLD_CALL','IMPORT','OTHER') NOT NULL 
DEFAULT 'OTHER', 
  industry VARCHAR(60) DEFAULT NULL, 
  city VARCHAR(60) DEFAULT NULL, 
  country_code CHAR(2) DEFAULT NULL, 
  status ENUM('NEW','CONTACTED','QUALIFIED','DISQUALIFIED','CONVERTED') NOT NULL DEFAULT 'NEW', 
  owner_user_id BIGINT UNSIGNED DEFAULT NULL, 
  converted_customer_id BIGINT UNSIGNED DEFAULT NULL, 
  converted_at DATETIME(3) DEFAULT NULL, 
  notes TEXT DEFAULT NULL, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_lead_no (company_id, lead_no), 
  KEY ix_lead_status (company_id, status), 
  KEY ix_lead_email (email), 
  CONSTRAINT fk_leads_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_leads_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users(id), 
  CONSTRAINT fk_leads_converted_customer_id FOREIGN KEY (converted_customer_id) REFERENCES 
customers(id), 
  CONSTRAINT fk_leads_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_leads_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE contacts ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  customer_id BIGINT UNSIGNED DEFAULT NULL, 
  lead_id BIGINT UNSIGNED DEFAULT NULL, 
  full_name VARCHAR(150) NOT NULL, 
  job_title VARCHAR(100) DEFAULT NULL, 
  email VARCHAR(150) DEFAULT NULL, 
  phone VARCHAR(30) DEFAULT NULL, 
  is_primary TINYINT(1) NOT NULL DEFAULT 0, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  KEY ix_contact_customer (customer_id), 
  CONSTRAINT fk_contacts_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_contacts_customer_id FOREIGN KEY (customer_id) REFERENCES customers(id), 
  CONSTRAINT fk_contacts_lead_id FOREIGN KEY (lead_id) REFERENCES leads(id), 
  CONSTRAINT fk_contacts_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_contacts_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE opportunities ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  opportunity_no VARCHAR(30) NOT NULL, 
  title VARCHAR(200) NOT NULL, 
  lead_id BIGINT UNSIGNED DEFAULT NULL, 
  customer_id BIGINT UNSIGNED DEFAULT NULL, 
  contact_id BIGINT UNSIGNED DEFAULT NULL, 
  stage_id BIGINT UNSIGNED NOT NULL, 
  expected_value DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  currency_code CHAR(3) NOT NULL, 
  probability_pct TINYINT NOT NULL DEFAULT 0, 
  expected_close_date DATE DEFAULT NULL, 
  owner_user_id BIGINT UNSIGNED DEFAULT NULL, 
  lost_reason VARCHAR(255) DEFAULT NULL, 
  closed_at DATETIME(3) DEFAULT NULL, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_opp_no (company_id, opportunity_no), 
  KEY ix_opp_stage (company_id, stage_id), 
  KEY ix_opp_owner (owner_user_id), 
  CONSTRAINT fk_opportunities_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_opportunities_lead_id FOREIGN KEY (lead_id) REFERENCES leads(id), 
  CONSTRAINT fk_opportunities_customer_id FOREIGN KEY (customer_id) REFERENCES customers(id), 
  CONSTRAINT fk_opportunities_contact_id FOREIGN KEY (contact_id) REFERENCES contacts(id), 
  CONSTRAINT fk_opportunities_stage_id FOREIGN KEY (stage_id) REFERENCES crm_pipeline_stages(id), 
  CONSTRAINT fk_opportunities_currency_code FOREIGN KEY (currency_code) REFERENCES 
currencies(code), 
  CONSTRAINT fk_opportunities_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users(id), 
  CONSTRAINT fk_opportunities_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_opportunities_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE crm_activities ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  activity_type ENUM('CALL','MEETING','EMAIL','WHATSAPP','NOTE','TASK') NOT NULL, 
  subject VARCHAR(200) NOT NULL, 
  details TEXT DEFAULT NULL, 
  related_type ENUM('LEAD','OPPORTUNITY','CUSTOMER') NOT NULL, 
  related_id BIGINT UNSIGNED NOT NULL, 
  due_at DATETIME(3) DEFAULT NULL, 
  completed_at DATETIME(3) DEFAULT NULL, 
  assigned_user_id BIGINT UNSIGNED DEFAULT NULL, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  KEY ix_act_related (related_type, related_id), 
  KEY ix_act_due (assigned_user_id, due_at), 
  CONSTRAINT fk_crm_activities_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_crm_activities_assigned_user_id FOREIGN KEY (assigned_user_id) REFERENCES 
users(id), 
  CONSTRAINT fk_crm_activities_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_crm_activities_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE quotations ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  quotation_no VARCHAR(30) NOT NULL, 
  opportunity_id BIGINT UNSIGNED DEFAULT NULL, 
  customer_id BIGINT UNSIGNED DEFAULT NULL, 
  lead_id BIGINT UNSIGNED DEFAULT NULL, 
  quote_date DATE NOT NULL, 
  valid_until DATE NOT NULL, 
  currency_code CHAR(3) NOT NULL, 
  subtotal DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  tax_total DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  total DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  status ENUM('DRAFT','SENT','ACCEPTED','REJECTED','EXPIRED','INVOICED') NOT NULL DEFAULT 'DRAFT', 
  invoice_id BIGINT UNSIGNED DEFAULT NULL, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_qt_no (company_id, quotation_no), 
  KEY ix_qt_opp (opportunity_id), 
  CONSTRAINT fk_quotations_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_quotations_opportunity_id FOREIGN KEY (opportunity_id) REFERENCES 
opportunities(id), 
  CONSTRAINT fk_quotations_customer_id FOREIGN KEY (customer_id) REFERENCES customers(id), 
  CONSTRAINT fk_quotations_lead_id FOREIGN KEY (lead_id) REFERENCES leads(id), 
  CONSTRAINT fk_quotations_currency_code FOREIGN KEY (currency_code) REFERENCES currencies(code), 
  CONSTRAINT fk_quotations_invoice_id FOREIGN KEY (invoice_id) REFERENCES invoices(id), 
  CONSTRAINT fk_quotations_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_quotations_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE quotation_lines ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  quotation_id BIGINT UNSIGNED NOT NULL, 
  line_no SMALLINT NOT NULL, 
  item_id BIGINT UNSIGNED DEFAULT NULL, 
  description VARCHAR(255) NOT NULL, 
  quantity DECIMAL(18,4) NOT NULL DEFAULT 1, 
  unit_price DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  discount_pct DECIMAL(5,2) NOT NULL DEFAULT 0.00, 
  tax_rate_id BIGINT UNSIGNED DEFAULT NULL, 
  line_total DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  PRIMARY KEY (id), 
  KEY ix_ql_quote (quotation_id), 
  CONSTRAINT fk_quotation_lines_quotation_id FOREIGN KEY (quotation_id) REFERENCES quotations(id) 
ON DELETE CASCADE, 
  CONSTRAINT fk_quotation_lines_item_id FOREIGN KEY (item_id) REFERENCES items(id), 
  CONSTRAINT fk_quotation_lines_tax_rate_id FOREIGN KEY (tax_rate_id) REFERENCES tax_rates(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
  
-- ===== Sales / Accounts Receivable ===== 
CREATE TABLE customers ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  code VARCHAR(20) NOT NULL, 
  name VARCHAR(200) NOT NULL, 
  trn VARCHAR(20) DEFAULT NULL, 
  email VARCHAR(150) DEFAULT NULL, 
  phone VARCHAR(30) DEFAULT NULL, 
  billing_address VARCHAR(500) DEFAULT NULL, 
  country_code CHAR(2) DEFAULT NULL, 
  currency_code CHAR(3) NOT NULL, 
  payment_terms_days SMALLINT NOT NULL DEFAULT 30, 
  credit_limit DECIMAL(18,2) DEFAULT NULL, 
  ar_account_id BIGINT UNSIGNED NOT NULL, 
  account_manager_id BIGINT UNSIGNED DEFAULT NULL, 
  source_lead_id BIGINT UNSIGNED DEFAULT NULL, 
  is_active TINYINT(1) NOT NULL DEFAULT 1, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_customers (company_id, code), 
  KEY ix_customers_name (company_id, name), 
  CONSTRAINT fk_customers_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_customers_ar_account_id FOREIGN KEY (ar_account_id) REFERENCES accounts(id), 
  CONSTRAINT fk_customers_account_manager_id FOREIGN KEY (account_manager_id) REFERENCES users(id), 
  CONSTRAINT fk_customers_source_lead_id FOREIGN KEY (source_lead_id) REFERENCES leads(id), 
  CONSTRAINT fk_customers_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_customers_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE invoices ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  invoice_no VARCHAR(30) NOT NULL, 
  doc_type ENUM('INVOICE','CREDIT_NOTE') NOT NULL DEFAULT 'INVOICE', 
  customer_id BIGINT UNSIGNED NOT NULL, 
  invoice_date DATE NOT NULL, 
  due_date DATE NOT NULL, 
  currency_code CHAR(3) NOT NULL, 
  exchange_rate DECIMAL(18,6) NOT NULL DEFAULT 1.000000, 
  subtotal DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  discount_total DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  tax_total DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  total DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  amount_paid DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  balance_due DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  status ENUM('DRAFT','SENT','PARTIAL','PAID','OVERDUE','VOID') NOT NULL DEFAULT 'DRAFT', 
  notes VARCHAR(1000) DEFAULT NULL, 
  quotation_id BIGINT UNSIGNED DEFAULT NULL, 
  salesperson_user_id BIGINT UNSIGNED DEFAULT NULL, 
  journal_entry_id BIGINT UNSIGNED DEFAULT NULL, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_inv_no (company_id, invoice_no), 
  KEY ix_inv_customer (customer_id), 
  KEY ix_inv_status_due (company_id, status, due_date), 
  CONSTRAINT fk_invoices_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_invoices_customer_id FOREIGN KEY (customer_id) REFERENCES customers(id), 
  CONSTRAINT fk_invoices_quotation_id FOREIGN KEY (quotation_id) REFERENCES quotations(id), 
  CONSTRAINT fk_invoices_salesperson_user_id FOREIGN KEY (salesperson_user_id) REFERENCES 
users(id), 
  CONSTRAINT fk_invoices_journal_entry_id FOREIGN KEY (journal_entry_id) REFERENCES 
journal_entries(id), 
  CONSTRAINT fk_invoices_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_invoices_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE invoice_lines ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  invoice_id BIGINT UNSIGNED NOT NULL, 
  line_no SMALLINT NOT NULL, 
  item_id BIGINT UNSIGNED DEFAULT NULL, 
  description VARCHAR(255) NOT NULL, 
  quantity DECIMAL(18,4) NOT NULL DEFAULT 1, 
  unit_price DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  discount_pct DECIMAL(5,2) NOT NULL DEFAULT 0.00, 
  tax_rate_id BIGINT UNSIGNED DEFAULT NULL, 
  tax_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  line_total DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  account_id BIGINT UNSIGNED NOT NULL, 
  warehouse_id BIGINT UNSIGNED DEFAULT NULL, 
  unit_cost DECIMAL(18,4) DEFAULT NULL, 
  cost_center_id BIGINT UNSIGNED DEFAULT NULL, 
  PRIMARY KEY (id), 
  KEY ix_invl_invoice (invoice_id), 
  CONSTRAINT fk_invoice_lines_invoice_id FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE 
CASCADE, 
  CONSTRAINT fk_invoice_lines_item_id FOREIGN KEY (item_id) REFERENCES items(id), 
  CONSTRAINT fk_invoice_lines_tax_rate_id FOREIGN KEY (tax_rate_id) REFERENCES tax_rates(id), 
  CONSTRAINT fk_invoice_lines_account_id FOREIGN KEY (account_id) REFERENCES accounts(id), 
  CONSTRAINT fk_invoice_lines_warehouse_id FOREIGN KEY (warehouse_id) REFERENCES warehouses(id), 
  CONSTRAINT fk_invoice_lines_cost_center_id FOREIGN KEY (cost_center_id) REFERENCES 
cost_centers(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE receipts ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  receipt_no VARCHAR(30) NOT NULL, 
  customer_id BIGINT UNSIGNED NOT NULL, 
  receipt_date DATE NOT NULL, 
  bank_account_id BIGINT UNSIGNED NOT NULL, 
  payment_method ENUM('CASH','CHEQUE','BANK_TRANSFER','CARD') NOT NULL, 
  reference VARCHAR(60) DEFAULT NULL, 
  currency_code CHAR(3) NOT NULL, 
  amount DECIMAL(18,2) NOT NULL, 
  unapplied_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  status ENUM('DRAFT','POSTED','VOID') NOT NULL DEFAULT 'DRAFT', 
  journal_entry_id BIGINT UNSIGNED DEFAULT NULL, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_rct_no (company_id, receipt_no), 
  KEY ix_rct_customer (customer_id), 
  CONSTRAINT fk_receipts_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_receipts_customer_id FOREIGN KEY (customer_id) REFERENCES customers(id), 
  CONSTRAINT fk_receipts_bank_account_id FOREIGN KEY (bank_account_id) REFERENCES 
bank_accounts(id), 
  CONSTRAINT fk_receipts_journal_entry_id FOREIGN KEY (journal_entry_id) REFERENCES 
journal_entries(id), 
  CONSTRAINT fk_receipts_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_receipts_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE receipt_allocations ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  receipt_id BIGINT UNSIGNED NOT NULL, 
  invoice_id BIGINT UNSIGNED NOT NULL, 
  amount DECIMAL(18,2) NOT NULL, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_ra (receipt_id, invoice_id), 
  CONSTRAINT fk_receipt_allocations_receipt_id FOREIGN KEY (receipt_id) REFERENCES receipts(id) ON 
DELETE CASCADE, 
  CONSTRAINT fk_receipt_allocations_invoice_id FOREIGN KEY (invoice_id) REFERENCES invoices(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
  
-- ===== Purchases / Accounts Payable ===== 
CREATE TABLE vendors ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  code VARCHAR(20) NOT NULL, 
  name VARCHAR(200) NOT NULL, 
  trn VARCHAR(20) DEFAULT NULL, 
  email VARCHAR(150) DEFAULT NULL, 
  phone VARCHAR(30) DEFAULT NULL, 
  address VARCHAR(500) DEFAULT NULL, 
  currency_code CHAR(3) NOT NULL, 
  payment_terms_days SMALLINT NOT NULL DEFAULT 30, 
  bank_iban VARCHAR(34) DEFAULT NULL, 
  ap_account_id BIGINT UNSIGNED NOT NULL, 
  is_active TINYINT(1) NOT NULL DEFAULT 1, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_vendors (company_id, code), 
  CONSTRAINT fk_vendors_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_vendors_ap_account_id FOREIGN KEY (ap_account_id) REFERENCES accounts(id), 
  CONSTRAINT fk_vendors_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_vendors_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE bills ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  bill_no VARCHAR(30) NOT NULL, 
  vendor_invoice_no VARCHAR(50) DEFAULT NULL, 
  doc_type ENUM('BILL','DEBIT_NOTE') NOT NULL DEFAULT 'BILL', 
  vendor_id BIGINT UNSIGNED NOT NULL, 
  bill_date DATE NOT NULL, 
  due_date DATE NOT NULL, 
  currency_code CHAR(3) NOT NULL, 
  exchange_rate DECIMAL(18,6) NOT NULL DEFAULT 1.000000, 
  subtotal DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  tax_total DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  total DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  amount_paid DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  balance_due DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  status ENUM('DRAFT','APPROVED','PARTIAL','PAID','OVERDUE','VOID') NOT NULL DEFAULT 'DRAFT', 
  journal_entry_id BIGINT UNSIGNED DEFAULT NULL, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_bill_no (company_id, bill_no), 
  UNIQUE KEY uk_vendor_inv (vendor_id, vendor_invoice_no), 
  KEY ix_bill_status_due (company_id, status, due_date), 
  CONSTRAINT fk_bills_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_bills_vendor_id FOREIGN KEY (vendor_id) REFERENCES vendors(id), 
  CONSTRAINT fk_bills_journal_entry_id FOREIGN KEY (journal_entry_id) REFERENCES 
journal_entries(id), 
  CONSTRAINT fk_bills_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_bills_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE bill_lines ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  bill_id BIGINT UNSIGNED NOT NULL, 
  line_no SMALLINT NOT NULL, 
  item_id BIGINT UNSIGNED DEFAULT NULL, 
  account_id BIGINT UNSIGNED NOT NULL, 
  warehouse_id BIGINT UNSIGNED DEFAULT NULL, 
  description VARCHAR(255) NOT NULL, 
  quantity DECIMAL(18,4) NOT NULL DEFAULT 1, 
  unit_price DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  tax_rate_id BIGINT UNSIGNED DEFAULT NULL, 
  tax_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  line_total DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  cost_center_id BIGINT UNSIGNED DEFAULT NULL, 
  PRIMARY KEY (id), 
  KEY ix_bl_bill (bill_id), 
  CONSTRAINT fk_bill_lines_bill_id FOREIGN KEY (bill_id) REFERENCES bills(id) ON DELETE CASCADE, 
  CONSTRAINT fk_bill_lines_item_id FOREIGN KEY (item_id) REFERENCES items(id), 
  CONSTRAINT fk_bill_lines_account_id FOREIGN KEY (account_id) REFERENCES accounts(id), 
  CONSTRAINT fk_bill_lines_warehouse_id FOREIGN KEY (warehouse_id) REFERENCES warehouses(id), 
  CONSTRAINT fk_bill_lines_tax_rate_id FOREIGN KEY (tax_rate_id) REFERENCES tax_rates(id), 
  CONSTRAINT fk_bill_lines_cost_center_id FOREIGN KEY (cost_center_id) REFERENCES cost_centers(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE payments ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  payment_no VARCHAR(30) NOT NULL, 
  vendor_id BIGINT UNSIGNED NOT NULL, 
  payment_date DATE NOT NULL, 
  bank_account_id BIGINT UNSIGNED NOT NULL, 
  payment_method ENUM('CASH','CHEQUE','BANK_TRANSFER','CARD') NOT NULL, 
  reference VARCHAR(60) DEFAULT NULL, 
  currency_code CHAR(3) NOT NULL, 
  amount DECIMAL(18,2) NOT NULL, 
  status ENUM('DRAFT','POSTED','VOID') NOT NULL DEFAULT 'DRAFT', 
  journal_entry_id BIGINT UNSIGNED DEFAULT NULL, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_pay_no (company_id, payment_no), 
  KEY ix_pay_vendor (vendor_id), 
  CONSTRAINT fk_payments_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_payments_vendor_id FOREIGN KEY (vendor_id) REFERENCES vendors(id), 
  CONSTRAINT fk_payments_bank_account_id FOREIGN KEY (bank_account_id) REFERENCES 
bank_accounts(id), 
  CONSTRAINT fk_payments_journal_entry_id FOREIGN KEY (journal_entry_id) REFERENCES 
journal_entries(id), 
  CONSTRAINT fk_payments_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_payments_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE payment_allocations ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  payment_id BIGINT UNSIGNED NOT NULL, 
  bill_id BIGINT UNSIGNED NOT NULL, 
  amount DECIMAL(18,2) NOT NULL, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_pa (payment_id, bill_id), 
  CONSTRAINT fk_payment_allocations_payment_id FOREIGN KEY (payment_id) REFERENCES payments(id) ON 
DELETE CASCADE, 
  CONSTRAINT fk_payment_allocations_bill_id FOREIGN KEY (bill_id) REFERENCES bills(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
  
-- ===== Inventory ===== 
CREATE TABLE item_categories ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  name VARCHAR(100) NOT NULL, 
  parent_id BIGINT UNSIGNED DEFAULT NULL, 
  income_account_id BIGINT UNSIGNED DEFAULT NULL, 
  inventory_account_id BIGINT UNSIGNED DEFAULT NULL, 
  cogs_account_id BIGINT UNSIGNED DEFAULT NULL, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_itemcat (company_id, parent_id, name), 
  CONSTRAINT fk_item_categories_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_item_categories_parent_id FOREIGN KEY (parent_id) REFERENCES item_categories(id), 
  CONSTRAINT fk_item_categories_income_account_id FOREIGN KEY (income_account_id) REFERENCES 
accounts(id), 
  CONSTRAINT fk_item_categories_inventory_account_id FOREIGN KEY (inventory_account_id) REFERENCES 
accounts(id), 
  CONSTRAINT fk_item_categories_cogs_account_id FOREIGN KEY (cogs_account_id) REFERENCES 
accounts(id), 
  CONSTRAINT fk_item_categories_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_item_categories_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE items ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  sku VARCHAR(40) NOT NULL, 
  barcode VARCHAR(40) DEFAULT NULL, 
  name VARCHAR(150) NOT NULL, 
  category_id BIGINT UNSIGNED DEFAULT NULL, 
  item_type ENUM('INVENTORY','NON_INVENTORY','SERVICE') NOT NULL, 
  unit VARCHAR(20) NOT NULL DEFAULT 'PCS', 
  costing_method ENUM('WEIGHTED_AVERAGE','FIFO') NOT NULL DEFAULT 'WEIGHTED_AVERAGE', 
  sales_price DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  purchase_price DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  reorder_level DECIMAL(18,4) NOT NULL DEFAULT 0, 
  reorder_qty DECIMAL(18,4) NOT NULL DEFAULT 0, 
  income_account_id BIGINT UNSIGNED DEFAULT NULL, 
  cogs_account_id BIGINT UNSIGNED DEFAULT NULL, 
  inventory_account_id BIGINT UNSIGNED DEFAULT NULL, 
  tax_rate_id BIGINT UNSIGNED DEFAULT NULL, 
  image_path VARCHAR(255) DEFAULT NULL, 
  is_active TINYINT(1) NOT NULL DEFAULT 1, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_items (company_id, sku), 
  KEY ix_items_category (category_id), 
  KEY ix_items_barcode (barcode), 
  CONSTRAINT fk_items_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_items_category_id FOREIGN KEY (category_id) REFERENCES item_categories(id), 
  CONSTRAINT fk_items_income_account_id FOREIGN KEY (income_account_id) REFERENCES accounts(id), 
  CONSTRAINT fk_items_cogs_account_id FOREIGN KEY (cogs_account_id) REFERENCES accounts(id), 
  CONSTRAINT fk_items_inventory_account_id FOREIGN KEY (inventory_account_id) REFERENCES 
accounts(id), 
  CONSTRAINT fk_items_tax_rate_id FOREIGN KEY (tax_rate_id) REFERENCES tax_rates(id), 
  CONSTRAINT fk_items_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_items_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE warehouses ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  code VARCHAR(20) NOT NULL, 
  name VARCHAR(100) NOT NULL, 
  address VARCHAR(500) DEFAULT NULL, 
  cost_center_id BIGINT UNSIGNED DEFAULT NULL, 
  is_default TINYINT(1) NOT NULL DEFAULT 0, 
  is_active TINYINT(1) NOT NULL DEFAULT 1, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_wh (company_id, code), 
  CONSTRAINT fk_warehouses_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_warehouses_cost_center_id FOREIGN KEY (cost_center_id) REFERENCES cost_centers(id), 
  CONSTRAINT fk_warehouses_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_warehouses_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE stock_balances ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  item_id BIGINT UNSIGNED NOT NULL, 
  warehouse_id BIGINT UNSIGNED NOT NULL, 
  qty_on_hand DECIMAL(18,4) NOT NULL DEFAULT 0, 
  qty_reserved DECIMAL(18,4) NOT NULL DEFAULT 0, 
  avg_cost DECIMAL(18,4) NOT NULL DEFAULT 0, 
  stock_value DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_stock (item_id, warehouse_id), 
  KEY ix_stock_wh (warehouse_id), 
  CONSTRAINT fk_stock_balances_item_id FOREIGN KEY (item_id) REFERENCES items(id), 
  CONSTRAINT fk_stock_balances_warehouse_id FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE stock_movements ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  item_id BIGINT UNSIGNED NOT NULL, 
  warehouse_id BIGINT UNSIGNED NOT NULL, 
  movement_date DATE NOT NULL, 
  movement_type 
ENUM('OPENING','PURCHASE','SALE','SALE_RETURN','PURCHASE_RETURN','ADJUSTMENT_IN','ADJUSTMENT_OUT','
TRANSFER_IN','TRANSFER_OUT') NOT NULL, 
  quantity DECIMAL(18,4) NOT NULL, 
  unit_cost DECIMAL(18,4) NOT NULL, 
  total_cost DECIMAL(18,2) NOT NULL, 
  balance_qty DECIMAL(18,4) NOT NULL, 
  source_type ENUM('INVOICE','BILL','ADJUSTMENT','TRANSFER','OPENING') NOT NULL, 
  source_id BIGINT UNSIGNED NOT NULL, 
  source_line_id BIGINT UNSIGNED DEFAULT NULL, 
  journal_entry_id BIGINT UNSIGNED DEFAULT NULL, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  PRIMARY KEY (id), 
  KEY ix_sm_item_wh_date (item_id, warehouse_id, movement_date), 
  KEY ix_sm_source (source_type, source_id), 
  CONSTRAINT fk_stock_movements_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_stock_movements_item_id FOREIGN KEY (item_id) REFERENCES items(id), 
  CONSTRAINT fk_stock_movements_warehouse_id FOREIGN KEY (warehouse_id) REFERENCES warehouses(id), 
  CONSTRAINT fk_stock_movements_journal_entry_id FOREIGN KEY (journal_entry_id) REFERENCES 
journal_entries(id), 
  CONSTRAINT fk_stock_movements_created_by FOREIGN KEY (created_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE stock_adjustments ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  adjustment_no VARCHAR(30) NOT NULL, 
  adjustment_date DATE NOT NULL, 
  warehouse_id BIGINT UNSIGNED NOT NULL, 
  reason ENUM('STOCK_COUNT','DAMAGED','EXPIRED','THEFT','OPENING','OTHER') NOT NULL, 
  adjustment_account_id BIGINT UNSIGNED NOT NULL, 
  notes VARCHAR(500) DEFAULT NULL, 
  total_value DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  status ENUM('DRAFT','POSTED','VOID') NOT NULL DEFAULT 'DRAFT', 
  journal_entry_id BIGINT UNSIGNED DEFAULT NULL, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_adj_no (company_id, adjustment_no), 
  CONSTRAINT fk_stock_adjustments_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_stock_adjustments_warehouse_id FOREIGN KEY (warehouse_id) REFERENCES 
warehouses(id), 
  CONSTRAINT fk_stock_adjustments_adjustment_account_id FOREIGN KEY (adjustment_account_id) 
REFERENCES accounts(id), 
  CONSTRAINT fk_stock_adjustments_journal_entry_id FOREIGN KEY (journal_entry_id) REFERENCES 
journal_entries(id), 
  CONSTRAINT fk_stock_adjustments_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_stock_adjustments_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE stock_adjustment_lines ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  adjustment_id BIGINT UNSIGNED NOT NULL, 
  item_id BIGINT UNSIGNED NOT NULL, 
  system_qty DECIMAL(18,4) NOT NULL, 
  counted_qty DECIMAL(18,4) NOT NULL, 
  difference_qty DECIMAL(18,4) NOT NULL, 
  unit_cost DECIMAL(18,4) NOT NULL, 
  value_impact DECIMAL(18,2) NOT NULL, 
  PRIMARY KEY (id), 
  KEY ix_adjl_adj (adjustment_id), 
  CONSTRAINT fk_stock_adjustment_lines_adjustment_id FOREIGN KEY (adjustment_id) REFERENCES 
stock_adjustments(id) ON DELETE CASCADE, 
  CONSTRAINT fk_stock_adjustment_lines_item_id FOREIGN KEY (item_id) REFERENCES items(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE stock_transfers ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  transfer_no VARCHAR(30) NOT NULL, 
  transfer_date DATE NOT NULL, 
  from_warehouse_id BIGINT UNSIGNED NOT NULL, 
  to_warehouse_id BIGINT UNSIGNED NOT NULL, 
  status ENUM('DRAFT','IN_TRANSIT','RECEIVED','VOID') NOT NULL DEFAULT 'DRAFT', 
  notes VARCHAR(500) DEFAULT NULL, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_trf_no (company_id, transfer_no), 
  CONSTRAINT fk_stock_transfers_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_stock_transfers_from_warehouse_id FOREIGN KEY (from_warehouse_id) REFERENCES 
warehouses(id), 
  CONSTRAINT fk_stock_transfers_to_warehouse_id FOREIGN KEY (to_warehouse_id) REFERENCES 
warehouses(id), 
  CONSTRAINT fk_stock_transfers_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_stock_transfers_updated_by FOREIGN KEY (updated_by) REFERENCES users(id), 
  CHECK (from_warehouse_id <> to_warehouse_id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE stock_transfer_lines ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  transfer_id BIGINT UNSIGNED NOT NULL, 
  item_id BIGINT UNSIGNED NOT NULL, 
  quantity DECIMAL(18,4) NOT NULL, 
  unit_cost DECIMAL(18,4) NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  KEY ix_trfl_trf (transfer_id), 
  CONSTRAINT fk_stock_transfer_lines_transfer_id FOREIGN KEY (transfer_id) REFERENCES 
stock_transfers(id) ON DELETE CASCADE, 
  CONSTRAINT fk_stock_transfer_lines_item_id FOREIGN KEY (item_id) REFERENCES items(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
  
-- ===== Expenses ===== 
CREATE TABLE expense_categories ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  name VARCHAR(100) NOT NULL, 
  account_id BIGINT UNSIGNED NOT NULL, 
  default_tax_rate_id BIGINT UNSIGNED DEFAULT NULL, 
  limit_per_claim DECIMAL(18,2) DEFAULT NULL, 
  receipt_required TINYINT(1) NOT NULL DEFAULT 1, 
  is_active TINYINT(1) NOT NULL DEFAULT 1, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_expcat (company_id, name), 
  CONSTRAINT fk_expense_categories_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_expense_categories_account_id FOREIGN KEY (account_id) REFERENCES accounts(id), 
  CONSTRAINT fk_expense_categories_default_tax_rate_id FOREIGN KEY (default_tax_rate_id) REFERENCES 
tax_rates(id), 
  CONSTRAINT fk_expense_categories_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_expense_categories_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE expenses ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  expense_no VARCHAR(30) NOT NULL, 
  expense_date DATE NOT NULL, 
  paid_by ENUM('COMPANY','EMPLOYEE') NOT NULL, 
  employee_user_id BIGINT UNSIGNED DEFAULT NULL, 
  vendor_id BIGINT UNSIGNED DEFAULT NULL, 
  payee_name VARCHAR(150) DEFAULT NULL, 
  bank_account_id BIGINT UNSIGNED DEFAULT NULL, 
  currency_code CHAR(3) NOT NULL, 
  exchange_rate DECIMAL(18,6) NOT NULL DEFAULT 1.000000, 
  subtotal DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  tax_total DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  total DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  status ENUM('DRAFT','SUBMITTED','APPROVED','REJECTED','POSTED','REIMBURSED','VOID') NOT NULL 
DEFAULT 'DRAFT', 
  submitted_at DATETIME(3) DEFAULT NULL, 
  approved_by BIGINT UNSIGNED DEFAULT NULL, 
  approved_at DATETIME(3) DEFAULT NULL, 
  rejection_reason VARCHAR(255) DEFAULT NULL, 
  journal_entry_id BIGINT UNSIGNED DEFAULT NULL, 
  reimbursement_entry_id BIGINT UNSIGNED DEFAULT NULL, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_exp_no (company_id, expense_no), 
  KEY ix_exp_status (company_id, status), 
  KEY ix_exp_employee (employee_user_id), 
  CONSTRAINT fk_expenses_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_expenses_employee_user_id FOREIGN KEY (employee_user_id) REFERENCES users(id), 
  CONSTRAINT fk_expenses_vendor_id FOREIGN KEY (vendor_id) REFERENCES vendors(id), 
  CONSTRAINT fk_expenses_bank_account_id FOREIGN KEY (bank_account_id) REFERENCES 
bank_accounts(id), 
  CONSTRAINT fk_expenses_currency_code FOREIGN KEY (currency_code) REFERENCES currencies(code), 
  CONSTRAINT fk_expenses_approved_by FOREIGN KEY (approved_by) REFERENCES users(id), 
  CONSTRAINT fk_expenses_journal_entry_id FOREIGN KEY (journal_entry_id) REFERENCES 
journal_entries(id), 
  CONSTRAINT fk_expenses_reimbursement_entry_id FOREIGN KEY (reimbursement_entry_id) REFERENCES 
journal_entries(id), 
  CONSTRAINT fk_expenses_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_expenses_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE expense_lines ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  expense_id BIGINT UNSIGNED NOT NULL, 
  line_no SMALLINT NOT NULL, 
  category_id BIGINT UNSIGNED NOT NULL, 
  account_id BIGINT UNSIGNED NOT NULL, 
  description VARCHAR(255) NOT NULL, 
  amount DECIMAL(18,2) NOT NULL, 
  tax_rate_id BIGINT UNSIGNED DEFAULT NULL, 
  tax_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  cost_center_id BIGINT UNSIGNED DEFAULT NULL, 
  customer_id BIGINT UNSIGNED DEFAULT NULL, 
  is_billable TINYINT(1) NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  KEY ix_expl_exp (expense_id), 
  CONSTRAINT fk_expense_lines_expense_id FOREIGN KEY (expense_id) REFERENCES expenses(id) ON DELETE 
CASCADE, 
  CONSTRAINT fk_expense_lines_category_id FOREIGN KEY (category_id) REFERENCES 
expense_categories(id), 
  CONSTRAINT fk_expense_lines_account_id FOREIGN KEY (account_id) REFERENCES accounts(id), 
  CONSTRAINT fk_expense_lines_tax_rate_id FOREIGN KEY (tax_rate_id) REFERENCES tax_rates(id), 
  CONSTRAINT fk_expense_lines_cost_center_id FOREIGN KEY (cost_center_id) REFERENCES 
cost_centers(id), 
  CONSTRAINT fk_expense_lines_customer_id FOREIGN KEY (customer_id) REFERENCES customers(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE attachments ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  entity_type VARCHAR(40) NOT NULL, 
  entity_id BIGINT UNSIGNED NOT NULL, 
  file_name VARCHAR(255) NOT NULL, 
  content_type VARCHAR(100) NOT NULL, 
  size_bytes INT UNSIGNED NOT NULL, 
  storage_key VARCHAR(500) NOT NULL, 
  uploaded_by BIGINT UNSIGNED DEFAULT NULL, 
  uploaded_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  PRIMARY KEY (id), 
  KEY ix_att_entity (entity_type, entity_id), 
  CONSTRAINT fk_attachments_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_attachments_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
  
-- ===== Banking ===== 
CREATE TABLE bank_accounts ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  company_id BIGINT UNSIGNED NOT NULL, 
  name VARCHAR(100) NOT NULL, 
  bank_name VARCHAR(100) DEFAULT NULL, 
  account_number VARCHAR(40) DEFAULT NULL, 
  iban VARCHAR(34) DEFAULT NULL, 
  swift_code VARCHAR(11) DEFAULT NULL, 
  currency_code CHAR(3) NOT NULL, 
  gl_account_id BIGINT UNSIGNED NOT NULL, 
  account_kind ENUM('BANK','CASH','CARD') NOT NULL DEFAULT 'BANK', 
  is_active TINYINT(1) NOT NULL DEFAULT 1, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_bank_gl (gl_account_id), 
  CONSTRAINT fk_bank_accounts_company_id FOREIGN KEY (company_id) REFERENCES companies(id), 
  CONSTRAINT fk_bank_accounts_gl_account_id FOREIGN KEY (gl_account_id) REFERENCES accounts(id), 
  CONSTRAINT fk_bank_accounts_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_bank_accounts_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE bank_statements ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  bank_account_id BIGINT UNSIGNED NOT NULL, 
  statement_date DATE NOT NULL, 
  opening_balance DECIMAL(18,2) NOT NULL, 
  closing_balance DECIMAL(18,2) NOT NULL, 
  file_name VARCHAR(255) DEFAULT NULL, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  KEY ix_bs_account (bank_account_id, statement_date), 
  CONSTRAINT fk_bank_statements_bank_account_id FOREIGN KEY (bank_account_id) REFERENCES 
bank_accounts(id), 
  CONSTRAINT fk_bank_statements_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_bank_statements_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE bank_statement_lines ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  bank_statement_id BIGINT UNSIGNED NOT NULL, 
  txn_date DATE NOT NULL, 
  description VARCHAR(255) DEFAULT NULL, 
  reference VARCHAR(60) DEFAULT NULL, 
  amount DECIMAL(18,2) NOT NULL, 
  matched_journal_line_id BIGINT UNSIGNED DEFAULT NULL, 
  match_status ENUM('UNMATCHED','SUGGESTED','MATCHED','IGNORED') NOT NULL DEFAULT 'UNMATCHED', 
  PRIMARY KEY (id), 
  KEY ix_bsl_stmt (bank_statement_id), 
  KEY ix_bsl_match (matched_journal_line_id), 
  CONSTRAINT fk_bank_statement_lines_bank_statement_id FOREIGN KEY (bank_statement_id) REFERENCES 
bank_statements(id) ON DELETE CASCADE, 
  CONSTRAINT fk_bank_statement_lines_matched_journal_line_id FOREIGN KEY (matched_journal_line_id) 
REFERENCES journal_lines(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
CREATE TABLE bank_reconciliations ( 
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  bank_account_id BIGINT UNSIGNED NOT NULL, 
  statement_date DATE NOT NULL, 
  statement_balance DECIMAL(18,2) NOT NULL, 
  book_balance DECIMAL(18,2) NOT NULL, 
  difference DECIMAL(18,2) NOT NULL DEFAULT 0.00, 
  status ENUM('IN_PROGRESS','COMPLETED') NOT NULL DEFAULT 'IN_PROGRESS', 
  completed_at DATETIME(3) DEFAULT NULL, 
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), 
  created_by BIGINT UNSIGNED DEFAULT NULL, 
  updated_at DATETIME(3) NULL ON UPDATE CURRENT_TIMESTAMP(3), 
  updated_by BIGINT UNSIGNED DEFAULT NULL, 
  version INT NOT NULL DEFAULT 0, 
  PRIMARY KEY (id), 
  UNIQUE KEY uk_recon (bank_account_id, statement_date), 
  CONSTRAINT fk_bank_reconciliations_bank_account_id FOREIGN KEY (bank_account_id) REFERENCES 
bank_accounts(id), 
  CONSTRAINT fk_bank_reconciliations_created_by FOREIGN KEY (created_by) REFERENCES users(id), 
  CONSTRAINT fk_bank_reconciliations_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; 
  
SET FOREIGN_KEY_CHECKS = 1;