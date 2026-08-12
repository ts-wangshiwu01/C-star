-- V0: employee table — local employee record, lazily created on first SSO login (ADR-0004).
-- See detail-design 9527-01-SSO-login.md §3.4.
--
-- Invariants:
--   I2: uq_employee_sso_id unique constraint (one row per sso_id)
--   I5: NO team / manager / employment_status columns (PRD §5.1, ADR-0006)
--
-- Note: Phase 1 has no name search requirement (PRD未要求模糊搜索);
--       if Phase 2 admin adds search, supplement idx_employee_name then.

CREATE TABLE employee (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  sso_id        VARCHAR(64)  NOT NULL COMMENT 'SSO 账号（ALDP），全局唯一',
  name          VARCHAR(128) NOT NULL COMMENT '员工姓名（来自 SSO claims，可更新）',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（首次登录懒同步）',
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uq_employee_sso_id (sso_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工记录，首次 SSO 登录懒同步创建（ADR-0004）';
