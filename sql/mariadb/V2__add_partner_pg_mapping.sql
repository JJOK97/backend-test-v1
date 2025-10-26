-- Partner-PG 매핑 테이블 추가
CREATE TABLE IF NOT EXISTS partner_pg_mapping (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  partner_id BIGINT NOT NULL,
  pg_type VARCHAR(50) NOT NULL,
  priority INT NOT NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_partner_priority (partner_id, priority),
  INDEX idx_partner_active (partner_id, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;