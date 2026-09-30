-- ============================================================================
-- V3: products
-- A product always belongs to an existing branch. Product names are unique
-- within a branch. Stock is guarded at the database level so that it can
-- never become negative, regardless of the code path that writes it.
-- ============================================================================
CREATE TABLE products (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    stock      INT          NOT NULL DEFAULT 0,
    branch_id  BIGINT       NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uq_products_branch_id_name UNIQUE (branch_id, name),
    CONSTRAINT chk_products_stock_non_negative CHECK (stock >= 0),
    CONSTRAINT fk_products_branch FOREIGN KEY (branch_id)
        REFERENCES branches (id)
        ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Covering index for the "highest stock per branch" window-function query:
-- ROW_NUMBER() OVER (PARTITION BY branch_id ORDER BY stock DESC, id ASC).
-- Having branch_id + stock in the index lets InnoDB feed the partition and
-- the ordering straight from the index, avoiding a filesort.
CREATE INDEX idx_products_branch_id_stock ON products (branch_id, stock DESC);
