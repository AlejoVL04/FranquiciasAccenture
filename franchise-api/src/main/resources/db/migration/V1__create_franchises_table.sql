-- ============================================================================
-- V1: franchises
-- Root aggregate of the domain. A franchise name must be globally unique.
-- ============================================================================
CREATE TABLE franchises (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_franchises PRIMARY KEY (id),
    CONSTRAINT uq_franchises_name UNIQUE (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
