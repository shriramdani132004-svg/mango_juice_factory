-- ============================================================
-- SmartFactory V3: Enhanced Material Categories, Inventory Status
-- Extends V1 enums for Part 3 inventory + recipe foundation
-- ============================================================

-- ============================================================
-- EXTEND material_type ENUM with new categories
-- ============================================================

ALTER TYPE material_type ADD VALUE IF NOT EXISTS 'INGREDIENT';
ALTER TYPE material_type ADD VALUE IF NOT EXISTS 'CONSUMABLE';
ALTER TYPE material_type ADD VALUE IF NOT EXISTS 'INTERMEDIATE_PRODUCT';

-- ============================================================
-- ADD inventory_status enum type
-- ============================================================

CREATE TYPE inventory_status AS ENUM (
    'IN_STOCK', 'LOW_STOCK', 'OUT_OF_STOCK', 'RESERVED', 'QUARANTINED'
);

-- ============================================================
-- ADD inventory transaction_type values
-- ============================================================

ALTER TYPE transaction_type ADD VALUE IF NOT EXISTS 'RELEASE';

-- ============================================================
-- ADD columns to inventory table
-- ============================================================

ALTER TABLE inventory ADD COLUMN IF NOT EXISTS status inventory_status NOT NULL DEFAULT 'IN_STOCK';

-- ============================================================
-- ADD columns to materials table for enhanced categorization
-- ============================================================

ALTER TABLE materials ADD COLUMN IF NOT EXISTS category VARCHAR(30);
ALTER TABLE materials ADD COLUMN IF NOT EXISTS density NUMERIC(8,4);
ALTER TABLE materials ADD COLUMN IF NOT EXISTS conversion_factor NUMERIC(8,4);

-- ============================================================
-- ADD columns to inventory_transactions for previous/resulting qty
-- ============================================================

ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS previous_quantity NUMERIC(12,4);
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS resulting_quantity NUMERIC(12,4);
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS reason VARCHAR(200);

-- ============================================================
-- ADD columns to recipe for output configuration
-- ============================================================

ALTER TABLE recipes ADD COLUMN IF NOT EXISTS output_quantity NUMERIC(12,4) DEFAULT 1000.00;
ALTER TABLE recipes ADD COLUMN IF NOT EXISTS output_unit VARCHAR(20) DEFAULT 'btl';
ALTER TABLE recipes ADD COLUMN IF NOT EXISTS batch_size NUMERIC(12,4) DEFAULT 1000.00;

-- ============================================================
-- ADD columns to recipe_materials for BOM ordering
-- ============================================================

ALTER TABLE recipe_materials ADD COLUMN IF NOT EXISTS bom_order INTEGER DEFAULT 0;
ALTER TABLE recipe_materials ADD COLUMN IF NOT EXISTS material_category VARCHAR(30);

-- ============================================================
-- UPDATE existing materials with category values
-- ============================================================

UPDATE materials SET category = 'RAW_MATERIAL' WHERE material_type = 'RAW';
UPDATE materials SET category = 'PACKAGING' WHERE material_type = 'PACKAGING';
UPDATE materials SET category = 'FINISHED_GOOD' WHERE material_type = 'FINISHED';

-- ============================================================
-- UPDATE existing inventory with status
-- ============================================================

UPDATE inventory SET status = CASE
    WHEN current_quantity <= 0 THEN 'OUT_OF_STOCK'::inventory_status
    WHEN current_quantity <= minimum_threshold THEN 'LOW_STOCK'::inventory_status
    ELSE 'IN_STOCK'::inventory_status
END;
