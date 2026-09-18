-- ============================================================
-- SmartFactory V4: Authentication + RBAC
-- Extends user_role enum, adds email column, updates admin
-- ============================================================

-- ============================================================
-- EXTEND user_role ENUM with new roles
-- ============================================================

ALTER TYPE user_role ADD VALUE IF NOT EXISTS 'PRODUCTION_WORKER';
ALTER TYPE user_role ADD VALUE IF NOT EXISTS 'MAINTENANCE_WORKER';
ALTER TYPE user_role ADD VALUE IF NOT EXISTS 'QUALITY_WORKER';

-- ============================================================
-- ADD email and display_name columns to users
-- ============================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS display_name VARCHAR(100);

-- Migrate existing full_name to display_name for the admin user
UPDATE users SET display_name = full_name WHERE display_name IS NULL;

-- ============================================================
-- UPDATE admin with display_name and email
-- ============================================================

UPDATE users SET display_name = 'System Administrator', email = 'admin@smartfactory.dev'
WHERE username = 'admin';

-- ============================================================
-- SEED USERS: moved to V4.1__seed_rbac_users.sql
-- PostgreSQL requires new enum values to be committed in a
-- separate transaction before they can be used in INSERT.
-- ============================================================
