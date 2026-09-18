-- ============================================================
-- SmartFactory V4.1: Seed RBAC Development Users
-- Runs after V4 to ensure new enum values are committed
-- ============================================================

-- ============================================================
-- SEED DEVELOPMENT USERS
-- Password for all: password123
-- BCrypt hash: $2a$10$EqKcp1WFKVQIShMPC7B3k.sN7fP7bA0TPVSGTCzKxN3wH6aFn.UOO
-- ============================================================

INSERT INTO users (user_id, username, password_hash, full_name, display_name, role, is_active, email) VALUES
(2, 'prod_worker',  '$2a$10$EqKcp1WFKVQIShMPC7B3k.sN7fP7bA0TPVSGTCzKxN3wH6aFn.UOO', 'Production Worker 1', 'Production Worker 1', 'PRODUCTION_WORKER', TRUE, 'prod_worker@smartfactory.dev')
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (user_id, username, password_hash, full_name, display_name, role, is_active, email) VALUES
(3, 'maint_worker', '$2a$10$EqKcp1WFKVQIShMPC7B3k.sN7fP7bA0TPVSGTCzKxN3wH6aFn.UOO', 'Maintenance Worker 1', 'Maintenance Worker 1', 'MAINTENANCE_WORKER', TRUE, 'maint_worker@smartfactory.dev')
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (user_id, username, password_hash, full_name, display_name, role, is_active, email) VALUES
(4, 'quality_worker', '$2a$10$EqKcp1WFKVQIShMPC7B3k.sN7fP7bA0TPVSGTCzKxN3wH6aFn.UOO', 'Quality Worker 1', 'Quality Worker 1', 'QUALITY_WORKER', TRUE, 'quality_worker@smartfactory.dev')
ON CONFLICT (username) DO NOTHING;
