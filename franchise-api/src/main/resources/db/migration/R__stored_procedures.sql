-- ============================================================================
-- Stored procedures
--
-- The API performs every write, and the top-stock report, through these
-- procedures. They own the business rules (existence of the parent, name
-- uniqueness, non-negative stock, product/branch ownership), so the rules hold
-- for any client of the database, not only for the API.
--
-- Repeatable migration (R__): Flyway re-applies it whenever its checksum
-- changes, which is why every procedure is dropped before being recreated.
--
-- Error contract
--   A rejected call raises SQLSTATE '45000' with one of these MYSQL_ERRNO
--   values; MESSAGE_TEXT is the client-facing message (max 128 characters).
--     50400  invalid input            -> HTTP 400
--     50404  resource not found       -> HTTP 404
--     50409  name already in use      -> HTTP 409
--
-- Names are trimmed and compared case-insensitively (utf8mb4_unicode_ci). The
-- collation is forced on each comparison so the procedures behave the same
-- whatever the server or database default collation is.
-- ============================================================================

DELIMITER //

-- ----------------------------------------------------------------------------
-- Helpers
-- ----------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS sp_raise_error //
CREATE PROCEDURE sp_raise_error(
    IN p_errno   INT,
    IN p_message VARCHAR(512) CHARACTER SET utf8mb4
)
BEGIN
    -- SIGNAL only accepts plain variables, and MESSAGE_TEXT is limited to 128.
    DECLARE v_message VARCHAR(128) CHARACTER SET utf8mb4 DEFAULT LEFT(p_message, 128);
    SIGNAL SQLSTATE '45000' SET MYSQL_ERRNO = p_errno, MESSAGE_TEXT = v_message;
END //

DROP PROCEDURE IF EXISTS sp_assert_valid_name //
CREATE PROCEDURE sp_assert_valid_name(
    IN p_name VARCHAR(255) CHARACTER SET utf8mb4
)
BEGIN
    IF p_name IS NULL OR p_name = '' THEN
        CALL sp_raise_error(50400, 'Name is required');
    END IF;
    IF CHAR_LENGTH(p_name) > 100 THEN
        CALL sp_raise_error(50400, 'Name must not exceed 100 characters');
    END IF;
END //

-- ----------------------------------------------------------------------------
-- Franchises
-- ----------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS sp_franchise_create //
CREATE PROCEDURE sp_franchise_create(
    IN p_name VARCHAR(255) CHARACTER SET utf8mb4
)
BEGIN
    DECLARE v_name VARCHAR(255) CHARACTER SET utf8mb4 DEFAULT TRIM(p_name);

    CALL sp_assert_valid_name(v_name);

    IF EXISTS (SELECT 1 FROM franchises WHERE name = v_name COLLATE utf8mb4_unicode_ci) THEN
        CALL sp_raise_error(50409, CONCAT('A franchise named ''', v_name, ''' already exists'));
    END IF;

    INSERT INTO franchises (name, created_at, updated_at)
    VALUES (v_name, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

    SELECT id, name, created_at, updated_at
    FROM franchises
    WHERE id = LAST_INSERT_ID();
END //

DROP PROCEDURE IF EXISTS sp_franchise_update_name //
CREATE PROCEDURE sp_franchise_update_name(
    IN p_franchise_id BIGINT,
    IN p_name         VARCHAR(255) CHARACTER SET utf8mb4
)
BEGIN
    DECLARE v_name VARCHAR(255) CHARACTER SET utf8mb4 DEFAULT TRIM(p_name);

    IF NOT EXISTS (SELECT 1 FROM franchises WHERE id = p_franchise_id) THEN
        CALL sp_raise_error(50404, CONCAT('Franchise with id ', p_franchise_id, ' not found'));
    END IF;

    CALL sp_assert_valid_name(v_name);

    -- The franchise itself is excluded, so a change of capitalisation is allowed.
    IF EXISTS (SELECT 1 FROM franchises
               WHERE name = v_name COLLATE utf8mb4_unicode_ci
                 AND id <> p_franchise_id) THEN
        CALL sp_raise_error(50409, CONCAT('A franchise named ''', v_name, ''' already exists'));
    END IF;

    UPDATE franchises
    SET name = v_name,
        updated_at = CURRENT_TIMESTAMP(6)
    WHERE id = p_franchise_id;

    SELECT id, name, created_at, updated_at
    FROM franchises
    WHERE id = p_franchise_id;
END //

-- Highest-stock product of every branch of a franchise, in a single statement.
--
-- The derived table ranks the franchise's products with ROW_NUMBER()
-- partitioned by branch and ordered by stock descending; keeping row_num = 1
-- leaves one row per branch. idx_products_branch_id_stock (branch_id, stock
-- DESC) feeds both the partitioning and the ordering.
--
--   * Tie on the maximum: broken by id ASC, so the oldest product wins and
--     the result is deterministic.
--   * Stock 0: a legitimate maximum, reported normally.
--   * Branch without products: kept by the LEFT JOIN with NULL product
--     columns; the API decides whether to report or omit it.
DROP PROCEDURE IF EXISTS sp_franchise_top_stock_products //
CREATE PROCEDURE sp_franchise_top_stock_products(
    IN p_franchise_id BIGINT
)
BEGIN
    -- An unknown franchise is an error, not an empty report: an empty result is
    -- already the right answer for a franchise that exists but has no branches.
    IF NOT EXISTS (SELECT 1 FROM franchises WHERE id = p_franchise_id) THEN
        CALL sp_raise_error(50404, CONCAT('Franchise with id ', p_franchise_id, ' not found'));
    END IF;

    SELECT f.id      AS franchiseId,
           f.name    AS franchiseName,
           b.id      AS branchId,
           b.name    AS branchName,
           tp.id     AS productId,
           tp.name   AS productName,
           tp.stock  AS stock
    FROM branches b
    INNER JOIN franchises f
            ON f.id = b.franchise_id
    LEFT JOIN (
        SELECT p.id,
               p.name,
               p.stock,
               p.branch_id,
               ROW_NUMBER() OVER (
                   PARTITION BY p.branch_id
                   ORDER BY p.stock DESC, p.id ASC
               ) AS row_num
        FROM products p
        INNER JOIN branches rb
                ON rb.id = p.branch_id
        WHERE rb.franchise_id = p_franchise_id
    ) tp
            ON tp.branch_id = b.id
           AND tp.row_num = 1
    WHERE b.franchise_id = p_franchise_id
    ORDER BY b.id ASC;
END //

-- ----------------------------------------------------------------------------
-- Branches
-- ----------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS sp_branch_create //
CREATE PROCEDURE sp_branch_create(
    IN p_franchise_id BIGINT,
    IN p_name         VARCHAR(255) CHARACTER SET utf8mb4
)
BEGIN
    DECLARE v_name VARCHAR(255) CHARACTER SET utf8mb4 DEFAULT TRIM(p_name);

    IF NOT EXISTS (SELECT 1 FROM franchises WHERE id = p_franchise_id) THEN
        CALL sp_raise_error(50404, CONCAT('Franchise with id ', p_franchise_id, ' not found'));
    END IF;

    CALL sp_assert_valid_name(v_name);

    IF EXISTS (SELECT 1 FROM branches
               WHERE franchise_id = p_franchise_id
                 AND name = v_name COLLATE utf8mb4_unicode_ci) THEN
        CALL sp_raise_error(50409, CONCAT('A branch named ''', v_name,
                                          ''' already exists in franchise ', p_franchise_id));
    END IF;

    INSERT INTO branches (name, franchise_id, created_at, updated_at)
    VALUES (v_name, p_franchise_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

    SELECT id, name, franchise_id, created_at, updated_at
    FROM branches
    WHERE id = LAST_INSERT_ID();
END //

DROP PROCEDURE IF EXISTS sp_branch_update_name //
CREATE PROCEDURE sp_branch_update_name(
    IN p_branch_id BIGINT,
    IN p_name      VARCHAR(255) CHARACTER SET utf8mb4
)
BEGIN
    DECLARE v_name VARCHAR(255) CHARACTER SET utf8mb4 DEFAULT TRIM(p_name);
    DECLARE v_franchise_id BIGINT;

    -- A scalar subquery rather than SELECT ... INTO, which would raise a
    -- "no data" warning for an unknown branch.
    SET v_franchise_id = (SELECT franchise_id FROM branches WHERE id = p_branch_id);

    IF v_franchise_id IS NULL THEN
        CALL sp_raise_error(50404, CONCAT('Branch with id ', p_branch_id, ' not found'));
    END IF;

    CALL sp_assert_valid_name(v_name);

    IF EXISTS (SELECT 1 FROM branches
               WHERE franchise_id = v_franchise_id
                 AND name = v_name COLLATE utf8mb4_unicode_ci
                 AND id <> p_branch_id) THEN
        CALL sp_raise_error(50409, CONCAT('A branch named ''', v_name,
                                          ''' already exists in franchise ', v_franchise_id));
    END IF;

    UPDATE branches
    SET name = v_name,
        updated_at = CURRENT_TIMESTAMP(6)
    WHERE id = p_branch_id;

    SELECT id, name, franchise_id, created_at, updated_at
    FROM branches
    WHERE id = p_branch_id;
END //

-- ----------------------------------------------------------------------------
-- Products
--
-- Every operation on an existing product is scoped by its branch: a product
-- addressed through a branch that does not own it is reported as not found,
-- never modified.
-- ----------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS sp_product_create //
CREATE PROCEDURE sp_product_create(
    IN p_branch_id BIGINT,
    IN p_name      VARCHAR(255) CHARACTER SET utf8mb4,
    IN p_stock     INT
)
BEGIN
    DECLARE v_name VARCHAR(255) CHARACTER SET utf8mb4 DEFAULT TRIM(p_name);

    IF NOT EXISTS (SELECT 1 FROM branches WHERE id = p_branch_id) THEN
        CALL sp_raise_error(50404, CONCAT('Branch with id ', p_branch_id, ' not found'));
    END IF;

    IF p_stock IS NULL OR p_stock < 0 THEN
        CALL sp_raise_error(50400, 'Stock must be greater than or equal to 0');
    END IF;

    CALL sp_assert_valid_name(v_name);

    IF EXISTS (SELECT 1 FROM products
               WHERE branch_id = p_branch_id
                 AND name = v_name COLLATE utf8mb4_unicode_ci) THEN
        CALL sp_raise_error(50409, CONCAT('A product named ''', v_name,
                                          ''' already exists in branch ', p_branch_id));
    END IF;

    INSERT INTO products (name, stock, branch_id, created_at, updated_at)
    VALUES (v_name, p_stock, p_branch_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

    SELECT id, name, stock, branch_id, created_at, updated_at
    FROM products
    WHERE id = LAST_INSERT_ID();
END //

DROP PROCEDURE IF EXISTS sp_product_update_stock //
CREATE PROCEDURE sp_product_update_stock(
    IN p_branch_id  BIGINT,
    IN p_product_id BIGINT,
    IN p_stock      INT
)
BEGIN
    IF p_stock IS NULL OR p_stock < 0 THEN
        CALL sp_raise_error(50400, 'Stock must be greater than or equal to 0');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM products WHERE id = p_product_id AND branch_id = p_branch_id) THEN
        CALL sp_raise_error(50404, CONCAT('Product with id ', p_product_id,
                                          ' not found in branch ', p_branch_id));
    END IF;

    UPDATE products
    SET stock = p_stock,
        updated_at = CURRENT_TIMESTAMP(6)
    WHERE id = p_product_id;

    SELECT id, name, stock, branch_id, created_at, updated_at
    FROM products
    WHERE id = p_product_id;
END //

DROP PROCEDURE IF EXISTS sp_product_update_name //
CREATE PROCEDURE sp_product_update_name(
    IN p_branch_id  BIGINT,
    IN p_product_id BIGINT,
    IN p_name       VARCHAR(255) CHARACTER SET utf8mb4
)
BEGIN
    DECLARE v_name VARCHAR(255) CHARACTER SET utf8mb4 DEFAULT TRIM(p_name);

    IF NOT EXISTS (SELECT 1 FROM products WHERE id = p_product_id AND branch_id = p_branch_id) THEN
        CALL sp_raise_error(50404, CONCAT('Product with id ', p_product_id,
                                          ' not found in branch ', p_branch_id));
    END IF;

    CALL sp_assert_valid_name(v_name);

    IF EXISTS (SELECT 1 FROM products
               WHERE branch_id = p_branch_id
                 AND name = v_name COLLATE utf8mb4_unicode_ci
                 AND id <> p_product_id) THEN
        CALL sp_raise_error(50409, CONCAT('A product named ''', v_name,
                                          ''' already exists in branch ', p_branch_id));
    END IF;

    UPDATE products
    SET name = v_name,
        updated_at = CURRENT_TIMESTAMP(6)
    WHERE id = p_product_id;

    SELECT id, name, stock, branch_id, created_at, updated_at
    FROM products
    WHERE id = p_product_id;
END //

DROP PROCEDURE IF EXISTS sp_product_delete //
CREATE PROCEDURE sp_product_delete(
    IN p_branch_id  BIGINT,
    IN p_product_id BIGINT
)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM products WHERE id = p_product_id AND branch_id = p_branch_id) THEN
        CALL sp_raise_error(50404, CONCAT('Product with id ', p_product_id,
                                          ' not found in branch ', p_branch_id));
    END IF;

    DELETE FROM products
    WHERE id = p_product_id;
END //

DELIMITER ;
