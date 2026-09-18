-- Part 7: Quality Control + Batch Verification

CREATE TABLE quality_checks (
    quality_check_id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL REFERENCES batches(batch_id),
    order_id BIGINT NOT NULL REFERENCES production_orders(order_id),
    phase_id BIGINT NOT NULL REFERENCES production_phases(phase_id),
    check_type VARCHAR(50) NOT NULL,
    parameter_name VARCHAR(100) NOT NULL,
    observed_value NUMERIC(16,6),
    expected_min NUMERIC(16,6),
    expected_max NUMERIC(16,6),
    expected_value NUMERIC(16,6),
    unit VARCHAR(20),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    result VARCHAR(20),
    mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    inspector_id BIGINT REFERENCES users(user_id),
    machine_id BIGINT REFERENCES machines(machine_id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    verified_at TIMESTAMP
);

CREATE INDEX idx_qc_batch ON quality_checks(batch_id);
CREATE INDEX idx_qc_phase ON quality_checks(phase_id);
CREATE INDEX idx_qc_order ON quality_checks(order_id);
CREATE INDEX idx_qc_batch_phase ON quality_checks(batch_id, phase_id);
CREATE INDEX idx_qc_status ON quality_checks(status);
CREATE INDEX idx_qc_result ON quality_checks(result);
