-- ============================================================
-- SmartFactory V1: Initial Schema
-- Mango Juice Manufacturing Control System
-- ============================================================

-- ============================================================
-- ENUM TYPES
-- ============================================================

CREATE TYPE machine_state AS ENUM (
    'IDLE', 'READY', 'RUNNING', 'PAUSED',
    'MAINTENANCE', 'FAILED', 'OFFLINE', 'EMERGENCY_STOP'
);

CREATE TYPE machine_capability AS ENUM (
    'RECEIVING', 'WASHING', 'SORTING', 'PEELING', 'PULPING',
    'FILTERING', 'BLENDING', 'PASTEURIZING', 'QUALITY_INSPECTION',
    'FILLING', 'CAPPING', 'LABELING', 'PACKAGING', 'WAREHOUSING',
    'MATERIAL_HANDLING'
);

CREATE TYPE material_type AS ENUM ('RAW', 'PACKAGING', 'FINISHED');

CREATE TYPE order_status AS ENUM (
    'DRAFT', 'REQUIREMENTS_CHECK', 'READY',
    'RUNNING', 'PAUSED', 'QUALITY_HOLD',
    'COMPLETED', 'CANCELLED'
);

CREATE TYPE batch_status AS ENUM (
    'PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'REJECTED'
);

CREATE TYPE phase_status AS ENUM (
    'LOCKED', 'REQUIREMENTS_CHECK', 'READY', 'RUNNING',
    'COMPLETED', 'WAITING_FOR_VERIFICATION', 'VERIFIED',
    'FAILED', 'QUARANTINED'
);

CREATE TYPE job_status AS ENUM (
    'QUEUED', 'ASSIGNED', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED'
);

CREATE TYPE transaction_type AS ENUM (
    'RECEIPT', 'RESERVATION', 'CONSUMPTION', 'PRODUCTION',
    'WASTE', 'REJECTION', 'ADJUSTMENT', 'TRANSFER'
);

CREATE TYPE quality_result AS ENUM ('PENDING', 'PASS', 'FAIL', 'REWORK');

CREATE TYPE incident_severity AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL');

CREATE TYPE user_role AS ENUM ('ADMIN', 'WORKER', 'VIEWER');

-- ============================================================
-- FACTORY
-- ============================================================

CREATE TABLE factory (
    factory_id      BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    location        VARCHAR(200),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- PRODUCTION LINES
-- ============================================================

CREATE TABLE production_lines (
    line_id         BIGSERIAL PRIMARY KEY,
    factory_id      BIGINT NOT NULL REFERENCES factory(factory_id),
    line_code       VARCHAR(20) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    capacity        INTEGER NOT NULL DEFAULT 1,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_production_lines_factory ON production_lines(factory_id);

-- ============================================================
-- MACHINE TYPES
-- ============================================================

CREATE TABLE machine_types (
    type_id         BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    description     TEXT,
    capability      machine_capability NOT NULL,
    default_capacity NUMERIC(10,2) NOT NULL DEFAULT 1.0,
    default_production_rate NUMERIC(10,2) NOT NULL DEFAULT 1.0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- MACHINES
-- ============================================================

CREATE TABLE machines (
    machine_id          BIGSERIAL PRIMARY KEY,
    machine_code        VARCHAR(30) NOT NULL UNIQUE,
    name                VARCHAR(100) NOT NULL,
    type_id             BIGINT NOT NULL REFERENCES machine_types(type_id),
    line_id             BIGINT NOT NULL REFERENCES production_lines(line_id),
    capability          machine_capability NOT NULL,
    capacity            NUMERIC(10,2) NOT NULL DEFAULT 1.0,
    production_rate     NUMERIC(10,4) NOT NULL DEFAULT 1.0,
    status              machine_state NOT NULL DEFAULT 'IDLE',
    health_score        INTEGER NOT NULL DEFAULT 100 CHECK (health_score BETWEEN 0 AND 100),
    temperature         NUMERIC(6,2),
    rpm                 NUMERIC(8,2),
    vibration           NUMERIC(6,3),
    power_consumption   NUMERIC(8,2),
    current_job_id      BIGINT,
    failure_count       INTEGER NOT NULL DEFAULT 0,
    last_maintenance_at TIMESTAMPTZ,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_machines_line ON machines(line_id);
CREATE INDEX idx_machines_type ON machines(type_id);
CREATE INDEX idx_machines_status ON machines(status);
CREATE INDEX idx_machines_capability ON machines(capability);
CREATE INDEX idx_machines_line_status ON machines(line_id, status);
CREATE INDEX idx_machines_capability_status ON machines(capability, status);

-- ============================================================
-- USERS
-- ============================================================

CREATE TABLE users (
    user_id         BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50) NOT NULL UNIQUE,
    password_hash   VARCHAR(200) NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    role            user_role NOT NULL DEFAULT 'WORKER',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- MATERIALS (RAW + PACKAGING + FINISHED)
-- ============================================================

CREATE TABLE materials (
    material_id     BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    material_code   VARCHAR(30) NOT NULL UNIQUE,
    material_type   material_type NOT NULL,
    unit            VARCHAR(20) NOT NULL,
    description     TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_materials_type ON materials(material_type);

-- ============================================================
-- INVENTORY
-- ============================================================

CREATE TABLE inventory (
    inventory_id        BIGSERIAL PRIMARY KEY,
    material_id         BIGINT NOT NULL REFERENCES materials(material_id),
    current_quantity    NUMERIC(12,4) NOT NULL DEFAULT 0 CHECK (current_quantity >= 0),
    reserved_quantity   NUMERIC(12,4) NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    minimum_threshold   NUMERIC(12,4) NOT NULL DEFAULT 0,
    unit                VARCHAR(20) NOT NULL,
    location            VARCHAR(100),
    last_restocked_at   TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_available_positive CHECK (current_quantity >= reserved_quantity),
    CONSTRAINT uq_inventory_material UNIQUE (material_id)
);

-- ============================================================
-- INVENTORY TRANSACTIONS
-- ============================================================

CREATE TABLE inventory_transactions (
    transaction_id      BIGSERIAL PRIMARY KEY,
    material_id         BIGINT NOT NULL REFERENCES materials(material_id),
    transaction_type    transaction_type NOT NULL,
    quantity            NUMERIC(12,4) NOT NULL,
    unit                VARCHAR(20) NOT NULL,
    reference_type      VARCHAR(50),
    reference_id        BIGINT,
    notes               TEXT,
    performed_by        BIGINT REFERENCES users(user_id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inv_txn_material ON inventory_transactions(material_id);
CREATE INDEX idx_inv_txn_type ON inventory_transactions(transaction_type);
CREATE INDEX idx_inv_txn_reference ON inventory_transactions(reference_type, reference_id);
CREATE INDEX idx_inv_txn_created ON inventory_transactions(created_at);

-- ============================================================
-- PRODUCTS
-- ============================================================

CREATE TABLE products (
    product_id      BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    product_code    VARCHAR(30) NOT NULL UNIQUE,
    description     TEXT,
    bottle_size_ml  INTEGER NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- RECIPES
-- ============================================================

CREATE TABLE recipes (
    recipe_id           BIGSERIAL PRIMARY KEY,
    product_id          BIGINT NOT NULL REFERENCES products(product_id),
    name                VARCHAR(100) NOT NULL,
    version             INTEGER NOT NULL DEFAULT 1,
    yield_percentage    NUMERIC(5,2) NOT NULL DEFAULT 100.00,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_recipe_product_version UNIQUE (product_id, version)
);

-- ============================================================
-- RECIPE MATERIALS (BOM per recipe)
-- ============================================================

CREATE TABLE recipe_materials (
    recipe_material_id  BIGSERIAL PRIMARY KEY,
    recipe_id           BIGINT NOT NULL REFERENCES recipes(recipe_id) ON DELETE CASCADE,
    material_id         BIGINT NOT NULL REFERENCES materials(material_id),
    quantity_per_batch  NUMERIC(12,4) NOT NULL,
    unit                VARCHAR(20) NOT NULL,
    is_optional         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recipe_materials_recipe ON recipe_materials(recipe_id);
CREATE INDEX idx_recipe_materials_material ON recipe_materials(material_id);

-- ============================================================
-- PRODUCTION ORDERS
-- ============================================================

CREATE TABLE production_orders (
    order_id                BIGSERIAL PRIMARY KEY,
    order_number            VARCHAR(30) NOT NULL UNIQUE,
    product_id              BIGINT NOT NULL REFERENCES products(product_id),
    recipe_id               BIGINT NOT NULL REFERENCES recipes(recipe_id),
    input_material_id       BIGINT NOT NULL REFERENCES materials(material_id),
    input_quantity          NUMERIC(12,4) NOT NULL CHECK (input_quantity > 0),
    bottle_size_ml          INTEGER NOT NULL,
    priority                INTEGER NOT NULL DEFAULT 5,
    deadline                TIMESTAMPTZ,
    status                  order_status NOT NULL DEFAULT 'DRAFT',
    estimated_output_qty    NUMERIC(12,4),
    estimated_output_unit   VARCHAR(20),
    estimated_production_rate NUMERIC(10,4),
    estimated_duration_minutes INTEGER,
    started_at              TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    created_by              BIGINT REFERENCES users(user_id),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_status ON production_orders(status);
CREATE INDEX idx_orders_product ON production_orders(product_id);
CREATE INDEX idx_orders_created ON production_orders(created_at);

-- ============================================================
-- PRODUCTION ORDER REQUIREMENTS
-- (for the Requirements Check before production starts)
-- ============================================================

CREATE TABLE production_order_requirements (
    requirement_id      BIGSERIAL PRIMARY KEY,
    order_id            BIGINT NOT NULL REFERENCES production_orders(order_id) ON DELETE CASCADE,
    requirement_type    VARCHAR(30) NOT NULL,
    material_id         BIGINT REFERENCES materials(material_id),
    required_quantity   NUMERIC(12,4) NOT NULL,
    required_unit       VARCHAR(20) NOT NULL,
    available_quantity  NUMERIC(12,4) NOT NULL DEFAULT 0,
    status              VARCHAR(10) NOT NULL DEFAULT 'RED',
    explanation         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_requirements_order ON production_order_requirements(order_id);

-- ============================================================
-- BATCHES
-- ============================================================

CREATE TABLE batches (
    batch_id            BIGSERIAL PRIMARY KEY,
    batch_code          VARCHAR(40) NOT NULL UNIQUE,
    order_id            BIGINT NOT NULL REFERENCES production_orders(order_id),
    parent_batch_id     BIGINT REFERENCES batches(batch_id),
    material_id         BIGINT REFERENCES materials(material_id),
    input_quantity      NUMERIC(12,4) NOT NULL DEFAULT 0,
    output_quantity     NUMERIC(12,4) NOT NULL DEFAULT 0,
    waste_quantity      NUMERIC(12,4) NOT NULL DEFAULT 0,
    rejected_quantity   NUMERIC(12,4) NOT NULL DEFAULT 0,
    current_phase       INTEGER NOT NULL DEFAULT 0,
    status              batch_status NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_batches_order ON batches(order_id);
CREATE INDEX idx_batches_status ON batches(status);
CREATE INDEX idx_batches_parent ON batches(parent_batch_id);

-- ============================================================
-- PRODUCTION PHASES
-- (10 phases per batch, phase-gated: must verify before unlock)
-- ============================================================

CREATE TABLE production_phases (
    phase_id                    BIGSERIAL PRIMARY KEY,
    batch_id                    BIGINT NOT NULL REFERENCES batches(batch_id) ON DELETE CASCADE,
    phase_number                INTEGER NOT NULL CHECK (phase_number BETWEEN 1 AND 10),
    phase_type                  VARCHAR(30) NOT NULL,
    status                      phase_status NOT NULL DEFAULT 'LOCKED',
    input_quantity              NUMERIC(12,4),
    output_quantity             NUMERIC(12,4),
    waste_quantity              NUMERIC(12,4),
    production_rate             NUMERIC(10,4),
    estimated_duration_minutes  INTEGER,
    actual_duration_minutes     INTEGER,
    required_machine_capability machine_capability,
    quality_status              quality_result NOT NULL DEFAULT 'PENDING',
    verification_status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    verified_by                 BIGINT REFERENCES users(user_id),
    started_at                  TIMESTAMPTZ,
    completed_at                TIMESTAMPTZ,
    verified_at                 TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_phase_batch_number UNIQUE (batch_id, phase_number)
);

CREATE INDEX idx_phases_batch ON production_phases(batch_id);
CREATE INDEX idx_phases_status ON production_phases(status);

-- ============================================================
-- PRODUCTION JOBS
-- (machine-level task for one phase of one batch)
-- ============================================================

CREATE TABLE production_jobs (
    job_id          BIGSERIAL PRIMARY KEY,
    batch_id        BIGINT NOT NULL REFERENCES batches(batch_id),
    phase_id        BIGINT NOT NULL REFERENCES production_phases(phase_id),
    machine_id      BIGINT REFERENCES machines(machine_id),
    status          job_status NOT NULL DEFAULT 'QUEUED',
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    error_message   TEXT,
    retry_count     INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_jobs_batch ON production_jobs(batch_id);
CREATE INDEX idx_jobs_phase ON production_jobs(phase_id);
CREATE INDEX idx_jobs_machine ON production_jobs(machine_id);
CREATE INDEX idx_jobs_status ON production_jobs(status);

-- ============================================================
-- MACHINE EVENTS
-- (telemetry and state change log)
-- ============================================================

CREATE TABLE machine_events (
    event_id        BIGSERIAL PRIMARY KEY,
    machine_id      BIGINT NOT NULL REFERENCES machines(machine_id),
    event_type      VARCHAR(30) NOT NULL,
    temperature     NUMERIC(6,2),
    rpm             NUMERIC(8,2),
    vibration       NUMERIC(6,3),
    power_consumption NUMERIC(8,2),
    state_from      machine_state,
    state_to        machine_state,
    message         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_machine_events_machine ON machine_events(machine_id);
CREATE INDEX idx_machine_events_type ON machine_events(event_type);
CREATE INDEX idx_machine_events_created ON machine_events(created_at);

-- ============================================================
-- QUALITY CHECKS
-- ============================================================

CREATE TABLE quality_checks (
    check_id        BIGSERIAL PRIMARY KEY,
    batch_id        BIGINT NOT NULL REFERENCES batches(batch_id),
    phase_id        BIGINT REFERENCES production_phases(phase_id),
    check_type      VARCHAR(50) NOT NULL,
    result          quality_result NOT NULL DEFAULT 'PENDING',
    notes           TEXT,
    inspector_id    BIGINT REFERENCES users(user_id),
    checked_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_quality_checks_batch ON quality_checks(batch_id);
CREATE INDEX idx_quality_checks_result ON quality_checks(result);

-- ============================================================
-- INCIDENTS
-- ============================================================

CREATE TABLE incidents (
    incident_id     BIGSERIAL PRIMARY KEY,
    machine_id      BIGINT REFERENCES machines(machine_id),
    batch_id        BIGINT REFERENCES batches(batch_id),
    severity        incident_severity NOT NULL DEFAULT 'MEDIUM',
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    resolved        BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at     TIMESTAMPTZ,
    resolved_by     BIGINT REFERENCES users(user_id),
    assigned_to     BIGINT REFERENCES users(user_id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_incidents_machine ON incidents(machine_id);
CREATE INDEX idx_incidents_resolved ON incidents(resolved);
CREATE INDEX idx_incidents_severity ON incidents(severity);

-- ============================================================
-- MAINTENANCE RECORDS
-- ============================================================

CREATE TABLE maintenance_records (
    maintenance_id  BIGSERIAL PRIMARY KEY,
    machine_id      BIGINT NOT NULL REFERENCES machines(machine_id),
    incident_id     BIGINT REFERENCES incidents(incident_id),
    maintenance_type VARCHAR(30) NOT NULL,
    description     TEXT,
    scheduled_at    TIMESTAMPTZ,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    performed_by    BIGINT REFERENCES users(user_id),
    status          VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_maintenance_machine ON maintenance_records(machine_id);
CREATE INDEX idx_maintenance_status ON maintenance_records(status);

-- ============================================================
-- WAREHOUSE INVENTORY
-- (finished goods storage)
-- ============================================================

CREATE TABLE warehouse_inventory (
    warehouse_id    BIGSERIAL PRIMARY KEY,
    batch_id        BIGINT REFERENCES batches(batch_id),
    material_id     BIGINT NOT NULL REFERENCES materials(material_id),
    product_id      BIGINT REFERENCES products(product_id),
    quantity        NUMERIC(12,4) NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    unit            VARCHAR(20) NOT NULL,
    location        VARCHAR(100),
    slot_code       VARCHAR(30),
    stored_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_warehouse_batch ON warehouse_inventory(batch_id);
CREATE INDEX idx_warehouse_product ON warehouse_inventory(product_id);

-- ============================================================
-- AUDIT LOGS
-- ============================================================

CREATE TABLE audit_logs (
    log_id          BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(user_id),
    action          VARCHAR(50) NOT NULL,
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       BIGINT,
    old_values      JSONB,
    new_values      JSONB,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_created ON audit_logs(created_at);

-- ============================================================
-- Foreign key: machines.current_job_id -> production_jobs.job_id
-- Added after production_jobs table exists
-- ============================================================

ALTER TABLE machines ADD CONSTRAINT fk_machines_current_job
    FOREIGN KEY (current_job_id) REFERENCES production_jobs(job_id);
