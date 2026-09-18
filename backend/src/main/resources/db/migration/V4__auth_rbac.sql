-- ============================================================
-- SmartFactory V4: Authentication + RBAC
-- Extends user_role enum, adds email column, seeds dev users
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
-- SEED DEVELOPMENT USERS
-- Password for all: password123
-- BCrypt hash: $2a$10$EqKcp1WFKVQIShMPC7B3k.sN7fP7bA0TPVSGTCzKxN3wH6aFn.UOO
-- ============================================================

INSERT INTO users (user_id, username, password_hash, display_name, role, is_active, email) VALUES
(2, 'prod_worker',  '$2a$10$EqKcp1WFKVQIShMPC7B3k.sN7fP7bA0TPVSGTCzKxN3wH6aFn.UOO', 'Production Worker 1', 'PRODUCTION_WORKER', TRUE, 'prod_worker@smartfactory.dev')
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (user_id, username, password_hash, display_name, role, is_active, email) VALUES
(3, 'maint_worker', '$2a$10$EqKcp1WFKVQIShMPC7B3k.sN7fP7bA0TPVSGTCzKxN3wH6aFn.UOO', 'Maintenance Worker 1', 'MAINTENANCE_WORKER', TRUE, 'maint_worker@smartfactory.dev')
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (user_id, username, password_hash, display_name, role, is_active, email) VALUES
(4, 'quality_worker', '$2a$10$EqKcp1WFKVQIShMPC7B3k.sN7fP7bA0TPVSGTCzKxN3wH6aFn.UOO', 'Quality Worker 1', 'QUALITY_WORKER', TRUE, 'quality_worker@smartfactory.dev')
ON CONFLICT (username) DO NOTHING;

-- Update admin with display_name and email
UPDATE users SET display_name = 'System Administrator', email = 'admin@smartfactory.dev'
WHERE username = 'admin';

-- ============================================================
-- AUDIT LOGS: already exists from V1
-- No additional schema changes needed
-- ============================================================
