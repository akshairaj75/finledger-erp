-- ===== Expense Approvals Table =====
CREATE TABLE IF NOT EXISTS expense_approvals (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  expense_id BIGINT UNSIGNED NOT NULL,
  step INT NOT NULL,
  approver_id BIGINT UNSIGNED NOT NULL,
  approver_name VARCHAR(150) DEFAULT NULL,
  decision ENUM('APPROVED','REJECTED') NOT NULL,
  comment VARCHAR(500) DEFAULT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY ix_exp_appr_expense (expense_id),
  CONSTRAINT fk_expense_approvals_expense_id FOREIGN KEY (expense_id) REFERENCES expenses(id) ON DELETE CASCADE,
  CONSTRAINT fk_expense_approvals_approver_id FOREIGN KEY (approver_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

