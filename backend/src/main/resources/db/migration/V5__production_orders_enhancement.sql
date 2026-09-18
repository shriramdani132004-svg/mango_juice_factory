-- ============================================================
-- SmartFactory V5: Production Orders Enhancement
-- Adds item_name, item_type, mandatory columns to requirements
-- ============================================================

ALTER TABLE production_order_requirements ADD COLUMN IF NOT EXISTS item_name VARCHAR(100);
ALTER TABLE production_order_requirements ADD COLUMN IF NOT EXISTS item_type VARCHAR(30);
ALTER TABLE production_order_requirements ADD COLUMN IF NOT EXISTS mandatory BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE production_order_requirements ADD COLUMN IF NOT EXISTS item_code VARCHAR(50);
