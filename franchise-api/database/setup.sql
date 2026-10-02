-- ============================================================================
-- One-off setup of the MySQL database and application user.
--
-- Run it once as an administrator (root) on a MySQL 8 server that is NOT the
-- Docker Compose one (Compose creates the database and user by itself):
--
--   mysql -u root -p < database/setup.sql
--
-- It only creates the empty database and the user. Tables, indexes and stored
-- procedures are created by Flyway the first time the API starts, from
-- src/main/resources/db/migration, so they are never defined twice.
--
-- Do NOT run the files in db/migration by hand: Flyway refuses to migrate a
-- schema that already holds objects it did not create, and objects created as
-- root cannot be replaced by franchise_user afterwards.
--
-- Change the password before running it, and use the same values in the API's
-- DB_NAME / DB_USERNAME / DB_PASSWORD environment variables.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS franchise_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'franchise_user'@'%' IDENTIFIED BY 'franchise_password';

-- Data access (API)        : SELECT, INSERT, UPDATE, DELETE, EXECUTE
-- Schema migrations (Flyway): CREATE, ALTER, DROP, INDEX, REFERENCES,
--                             CREATE ROUTINE, ALTER ROUTINE
GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE,
      CREATE, ALTER, DROP, INDEX, REFERENCES,
      CREATE ROUTINE, ALTER ROUTINE
    ON franchise_db.* TO 'franchise_user'@'%';

FLUSH PRIVILEGES;
