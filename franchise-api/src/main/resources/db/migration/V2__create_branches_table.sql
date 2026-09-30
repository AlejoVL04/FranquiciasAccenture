-- ============================================================================
-- V2: branches
-- A branch always belongs to an existing franchise. Branch names are unique
-- within a franchise (not globally), hence the composite unique constraint.
-- ============================================================================
CREATE TABLE branches (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(100) NOT NULL,
    franchise_id BIGINT       NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    CONSTRAINT pk_branches PRIMARY KEY (id),
    CONSTRAINT uq_branches_franchise_id_name UNIQUE (franchise_id, name),
    CONSTRAINT fk_branches_franchise FOREIGN KEY (franchise_id)
        REFERENCES franchises (id)
        ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Supports "list branches of a franchise" and the top-stock report.
-- Note: uq_branches_franchise_id_name already provides a (franchise_id, ...)
-- left-most prefix index, so no additional single-column index is created.
