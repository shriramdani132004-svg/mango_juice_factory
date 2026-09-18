-- Add missing columns to production_jobs that the JPA entity expects
-- These were added to the entity but never migrated from V1

ALTER TABLE production_jobs ADD COLUMN IF NOT EXISTS input_quantity NUMERIC(12,4);
ALTER TABLE production_jobs ADD COLUMN IF NOT EXISTS expected_output NUMERIC(12,4);
ALTER TABLE production_jobs ADD COLUMN IF NOT EXISTS actual_output NUMERIC(12,4);
ALTER TABLE production_jobs ADD COLUMN IF NOT EXISTS production_rate NUMERIC(10,4);
ALTER TABLE production_jobs ADD COLUMN IF NOT EXISTS progress NUMERIC(5,2) DEFAULT 0;
