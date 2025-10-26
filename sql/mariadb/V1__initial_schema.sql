-- MariaDB Initial Schema
-- partner master
CREATE TABLE IF NOT EXISTS partner (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL,
  active TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- partner fee policy
CREATE TABLE IF NOT EXISTS partner_fee_policy (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  partner_id BIGINT NOT NULL,
  effective_from TIMESTAMP NOT NULL,
  percentage DECIMAL(10,6) NOT NULL,
  fixed_fee DECIMAL(15,0) NULL,
  INDEX idx_fee_partner_from (partner_id, effective_from DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- payment history
CREATE TABLE IF NOT EXISTS payment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  partner_id BIGINT NOT NULL,
  amount DECIMAL(15,0) NOT NULL,
  applied_fee_rate DECIMAL(10,6) NOT NULL,
  fee_amount DECIMAL(15,0) NOT NULL,
  net_amount DECIMAL(15,0) NOT NULL,
  card_bin VARCHAR(8) NULL,
  card_last4 VARCHAR(4) NULL,
  approval_code VARCHAR(32) NOT NULL,
  approved_at TIMESTAMP NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  INDEX idx_payment_created (created_at DESC, id DESC),
  INDEX idx_payment_partner_created (partner_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
