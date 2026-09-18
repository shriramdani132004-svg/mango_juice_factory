-- ============================================================
-- SmartFactory V2: Seed Data
-- Factory, 10 lines, 500 machines, materials, recipes
-- ============================================================

-- ============================================================
-- FACTORY
-- ============================================================

INSERT INTO factory (factory_id, name, location)
VALUES (1, 'SmartFactory Mango Processing Plant', 'Main Campus');

-- ============================================================
-- PRODUCTION LINES (10 lines)
-- ============================================================

INSERT INTO production_lines (line_id, factory_id, line_code, name, capacity) VALUES
(1,  1, 'LINE-01', 'Production Line 1', 50),
(2,  1, 'LINE-02', 'Production Line 2', 50),
(3,  1, 'LINE-03', 'Production Line 3', 50),
(4,  1, 'LINE-04', 'Production Line 4', 50),
(5,  1, 'LINE-05', 'Production Line 5', 50),
(6,  1, 'LINE-06', 'Production Line 6', 50),
(7,  1, 'LINE-07', 'Production Line 7', 50),
(8,  1, 'LINE-08', 'Production Line 8', 50),
(9,  1, 'LINE-09', 'Production Line 9', 50),
(10, 1, 'LINE-10', 'Production Line 10', 50);

-- ============================================================
-- MACHINE TYPES (10 types, one per manufacturing phase)
-- ============================================================

INSERT INTO machine_types (type_id, name, description, capability, default_capacity, default_production_rate) VALUES
(1,  'Receiver',            'Raw material receiving and weighing',       'RECEIVING',             1000.00, 500.00),
(2,  'Washer-Sorter',       'Washing and sorting raw materials',         'WASHING',               800.00,  400.00),
(3,  'Peeler-Pulper',       'Peeling and extracting pulp',               'PEELING',               600.00,  300.00),
(4,  'Filter',              'Filtration of juice',                       'FILTERING',             700.00,  350.00),
(5,  'Blender',             'Blending juice with ingredients',           'BLENDING',              500.00,  250.00),
(6,  'Pasteurizer',         'Heat pasteurization treatment',             'PASTEURIZING',          500.00,  200.00),
(7,  'Quality Inspector',   'Quality inspection and testing',            'QUALITY_INSPECTION',    100.00,  50.00),
(8,  'Filler-Capper',       'Bottle filling and capping',                'FILLING',               400.00,  200.00),
(9,  'Labeler-Packer',      'Labeling and packaging',                    'LABELING',              400.00,  200.00),
(10, 'Material Handler',    'Conveyor and warehouse handling',           'WAREHOUSING',           1000.00, 500.00);

-- ============================================================
-- MACHINES (500 total: 10 lines x 50 machines)
-- Each line: 5 of each machine type
-- ============================================================

DO $$
DECLARE
    line_rec RECORD;
    type_rec RECORD;
    machine_counter INTEGER;
    machine_num INTEGER;
    prefix VARCHAR(10);
BEGIN
    FOR line_rec IN SELECT line_id FROM production_lines ORDER BY line_id LOOP
        machine_counter := 0;
        prefix := 'L' || LPAD(line_rec.line_id::TEXT, 2, '0');
        FOR type_rec IN SELECT type_id, capability, default_capacity, default_production_rate
                         FROM machine_types ORDER BY type_id LOOP
            FOR machine_num IN 1..5 LOOP
                machine_counter := machine_counter + 1;
                INSERT INTO machines (
                    machine_code, name, type_id, line_id, capability,
                    capacity, production_rate, status, health_score
                ) VALUES (
                    prefix || '-' || LPAD(machine_counter::TEXT, 3, '0'),
                    prefix || ' Machine ' || machine_counter,
                    type_rec.type_id,
                    line_rec.line_id,
                    type_rec.capability,
                    type_rec.default_capacity,
                    type_rec.default_production_rate,
                    'IDLE',
                    100
                );
            END LOOP;
        END LOOP;
    END LOOP;
END $$;

-- ============================================================
-- MATERIALS (RAW)
-- ============================================================

INSERT INTO materials (material_id, name, material_code, material_type, unit, description) VALUES
(1, 'Fresh Mangoes',      'RAW-MANGO',    'RAW', 'kg',   'Fresh Alphonso mangoes'),
(2, 'Water',              'RAW-WATER',    'RAW', 'L',    'Purified drinking water'),
(3, 'Sugar',              'RAW-SUGAR',    'RAW', 'kg',   'Refined white sugar'),
(4, 'Citric Acid',        'RAW-CITRIC',   'RAW', 'kg',   'Food-grade citric acid'),
(5, 'Pectin',             'RAW-PECTIN',   'RAW', 'kg',   'Food-grade pectin'),
(6, 'Salt',               'RAW-SALT',     'RAW', 'kg',   'Iodized table salt');

-- ============================================================
-- MATERIALS (PACKAGING)
-- ============================================================

INSERT INTO materials (material_id, name, material_code, material_type, unit, description) VALUES
(10, '500mL PET Bottles',  'PKG-BOTTLE500','PACKAGING', 'pcs', '500 mL transparent PET bottles'),
(11, 'Bottle Caps',        'PKG-CAP',      'PACKAGING', 'pcs', 'Tamper-evident screw caps'),
(12, 'Product Labels',     'PKG-LABEL',    'PACKAGING', 'pcs', 'Printed product labels'),
(13, 'Carton Boxes',       'PKG-CARTON',   'PACKAGING', 'pcs', 'Corrugated cardboard cartons'),
(14, 'Shrink Wrap',        'PKG-SHRINK',   'PACKAGING', 'm',   'Heat-shrink packaging film');

-- ============================================================
-- MATERIALS (FINISHED)
-- ============================================================

INSERT INTO materials (material_id, name, material_code, material_type, unit, description) VALUES
(20, 'Mango Beverage 500mL', 'FIN-MANGO500', 'FINISHED', 'btl', 'Finished mango beverage bottle'),
(21, 'Carton of 12 Bottles', 'FIN-CARTON12', 'FINISHED', 'crt', 'Packaged carton with 12 bottles');

-- ============================================================
-- INVENTORY (initial stock levels)
-- ============================================================

INSERT INTO inventory (material_id, current_quantity, reserved_quantity, minimum_threshold, unit, location) VALUES
(1,  50000.00, 0.00, 10000.00, 'kg',  'Raw Material Warehouse A'),
(2,  100000.00, 0.00, 20000.00, 'L',   'Water Tank Farm'),
(3,  8000.00, 0.00, 2000.00, 'kg',  'Raw Material Warehouse A'),
(4,  500.00, 0.00, 100.00, 'kg',  'Raw Material Warehouse B'),
(5,  200.00, 0.00, 50.00, 'kg',  'Raw Material Warehouse B'),
(6,  300.00, 0.00, 50.00, 'kg',  'Raw Material Warehouse B'),
(10, 200000.00, 0.00, 50000.00, 'pcs', 'Packaging Warehouse C'),
(11, 200000.00, 0.00, 50000.00, 'pcs', 'Packaging Warehouse C'),
(12, 200000.00, 0.00, 50000.00, 'pcs', 'Packaging Warehouse C'),
(13, 50000.00, 0.00, 10000.00, 'pcs', 'Packaging Warehouse C'),
(14, 10000.00, 0.00, 2000.00, 'm',   'Packaging Warehouse C');

-- ============================================================
-- PRODUCTS
-- ============================================================

INSERT INTO products (product_id, name, product_code, description, bottle_size_ml) VALUES
(1, 'Mango Beverage', 'PROD-MANGO', 'Premium mango beverage made from fresh mangoes', 500);

-- ============================================================
-- RECIPES
-- ============================================================

INSERT INTO recipes (recipe_id, product_id, name, version, yield_percentage) VALUES
(1, 1, 'Mango Beverage 500mL Recipe v1', 1, 85.00);

-- ============================================================
-- RECIPE MATERIALS (BOM)
-- Per 1000 bottles of 500mL output
-- ============================================================

INSERT INTO recipe_materials (recipe_id, material_id, quantity_per_batch, unit, is_optional) VALUES
-- Raw materials
(1, 1,  2500.00, 'kg',  FALSE),   -- Fresh Mangoes: 2500 kg per 1000 bottles
(1, 2,  1500.00, 'L',   FALSE),   -- Water: 1500 L per 1000 bottles
(1, 3,  150.00,  'kg',  FALSE),   -- Sugar: 150 kg per 1000 bottles
(1, 4,  10.00,   'kg',  FALSE),   -- Citric Acid: 10 kg per 1000 bottles
(1, 5,  5.00,    'kg',  TRUE),    -- Pectin: 5 kg per 1000 bottles (optional)
-- Packaging materials
(1, 10, 1020.00, 'pcs', FALSE),   -- Bottles: 1020 per 1000 (2% buffer)
(1, 11, 1020.00, 'pcs', FALSE),   -- Caps: 1020 per 1000
(1, 12, 1020.00, 'pcs', FALSE),   -- Labels: 1020 per 1000
(1, 13, 85.00,   'pcs', FALSE),   -- Cartons: 85 per 1000 (12 bottles each)
(1, 14, 100.00,  'm',   FALSE);   -- Shrink Wrap: 100m per 1000 bottles

-- ============================================================
-- DEFAULT ADMIN USER
-- Password: admin123 (BCrypt hash)
-- ============================================================

INSERT INTO users (user_id, username, password_hash, full_name, role) VALUES
(1, 'admin', '$2a$10$WhBGYdFrB1jsf9hErmqAkuDGANy8b0zsRRmzA7VFH3YiOEMMkmsca', 'System Administrator', 'ADMIN');
