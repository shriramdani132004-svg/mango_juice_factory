# Quality Control — Part 7 Implementation

## Overview

Part 7 adds an in-process quality control gate between each production phase and its verification. When a phase completes, the system creates quality checks, a worker (or auto-evaluate) inspects them, and only batches with all mandatory checks PASS can proceed to admin verification and the next phase.

---

## Architecture

```
Phase COMPLETED
       │
       ▼
┌──────────────────────┐
│ SimulationEngine      │
│ completePhase()       │
│ → QualityCheckService │
│   .createChecksFor    │
│     Phase(batch, phase)│
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ QualityCheckController│
│  POST /{id}/inspect   │
│  POST /auto-evaluate  │
│  POST /batch/quarantine│
│  POST /batch/reject   │
│  POST /batch/reprocess│
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ QualityCheckService   │
│  createChecksForPhase │
│  inspectQualityCheck  │
│  autoEvaluateQC       │
│  quarantineBatch      │
│  rejectBatch          │
│  reprocessBatch       │
│  getPhaseResult       │
│  getBatchSummary      │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ QualityRuleEngine     │
│  evaluate(value, min, │
│           max) → PASS │
│       or FAIL         │
└──────────────────────┘
```

---

## Key Components

### QualityCheck Entity
- `id`, `batchId`, `phaseId`, `orderId`, `batchCode`, `phaseName`, `orderNumber`
- `checkType` — enum of 23 types (TEMPERATURE, PROCESSING_TIME, YIELD, etc.)
- `observedValue`, `expectedMin`, `expectedMax` — numeric range validation
- `status` — PENDING, IN_PROGRESS, PASS, FAIL, REVIEW, VERIFIED, QUARANTINED, REJECTED, REPROCESS_REQUESTED, REPROCESSED
- `result` — PASS, FAIL, CONDITIONAL (after inspection)
- `mandatory` — true if check must pass to proceed
- `inspector`, `machine`, `batchCode`, `orderNumber` — audit fields
- `failureReason`, `correctiveAction`, `inspectionNotes` — failure details

### QualityCheckStatus Enum
```
PENDING → IN_PROGRESS → PASS or FAIL or REVIEW
PASS → VERIFIED
FAIL → QUARANTINED or REJECTED or REPROCESS_REQUESTED → REPROCESSED
```

### QualityResult Enum
- `PASS` — observed value within [expectedMin, expectedMax]
- `FAIL` — observed value outside range
- `CONDITIONAL` — no range defined, needs manual review

### QualityCheckType Enum (23 types)
| Type | Typical Phase |
|------|---------------|
| TEMPERATURE | Phase 6 (Pasteurization), Phase 7 |
| PROCESSING_TIME | Phase 6 |
| FLOW_RATE | Phases 1, 4 |
| YIELD | Phases 3, 10 |
| CONSISTENCY | Phase 5 |
| pH_LEVEL | Phases 4, 7 |
| VISCOSITY | Phases 5, 7 |
| COLOR | Phase 7 |
| AROMATIC_PROFILE | Phase 5 |
| BRIX | Phase 7 |
| MICROBIOLOGICAL | Phase 7 |
| SUGAR_CONTENT | Phase 5 |
| ACIDITY | Phase 7 |
| BOTTLE_INTEGRITY | Phase 8 |
| LABEL_ACCURACY | Phase 9 |
| FILL_LEVEL | Phase 8 |
| CAPPING_TORQUE | Phase 8 |
| SEAL_INTEGRITY | Phase 8 |
| WEIGHT | Phase 10 |
| CONTAMINATION | Phase 7 |
| PARTICLE_SIZE | Phases 3, 4 |
| WATER_QUALITY | Phase 1 |
| SANITATION | Phase 2 |

### QualityRuleEngine
Deterministic evaluation (no randomness):
- If both `expectedMin` and `expectedMax` are set: **PASS** if value is in range, **FAIL** otherwise
- If no range defined: **CONDITIONAL** (requires manual review)

### QualityCheckService
- `createQualityChecksForPhase(batchId, phaseId)` — creates 2–3 mandatory checks per phase (configurable rules)
- `inspectQualityCheck(checkId, observedValue, result, notes, userId)` — records inspection
- `autoEvaluateQualityCheck(checkId)` — auto-evaluate using rule engine
- `quarantineBatch(batchId, reason)` — batch → QUARANTINED, all pending checks → QUARANTINED
- `rejectBatch(batchId, reason)` — batch → REJECTED, all pending checks → REJECTED
- `reprocessBatch(batchId, reason)` — batch → REPROCESS_REQUESTED, all pending checks → REPROCESS_REQUESTED
- `getPhaseQualityResult(batchId, phaseId)` — aggregate result for a phase
- `getBatchQualitySummary(batchId)` — summary for entire batch

### SimulationEngine Integration
In `completePhase()`:
1. Phase status → `WAITING_FOR_QUALITY`
2. Calls `QualityCheckService.createQualityChecksForPhase()`
3. Publishes `PHASE_WAITING_FOR_QUALITY` SSE event
4. Phase cannot proceed to verification until quality is resolved

### PhaseExecutionService Integration
In `verifyPhase()`:
1. Checks phase status is `WAITING_FOR_VERIFICATION`
2. Validates all mandatory quality checks are PASS
3. Validates batch status is not QUARANTINED or REJECTED
4. If quality not passed → throws ValidationException
5. If quality passed → proceeds with verification and unlocks next phase

---

## Quality Gate Flow

```
Phase COMPLETED
       │
       ▼
Phase → WAITING_FOR_QUALITY
       │
       ▼
Quality checks created (2–3 per phase)
       │
       ▼
Worker inspects OR auto-evaluate
       │
       ├── All mandatory PASS → Phase → WAITING_FOR_VERIFICATION
       │                                   │
       │                                   ▼
       │                          Admin verifies → VERIFIED
       │                                   │
       │                                   ▼
       │                          Next phase → READY
       │
       └── Any mandatory FAIL → Batch → QUARANTINE hold
                                         │
                                         ├── REPROCESS → Reset checks, retry
                                         ├── REJECT → Batch scrapped
                                         └── QUARANTINE → Hold pending decision
```

---

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/quality-checks` | ADMIN, QUALITY_WORKER | Create quality check |
| POST | `/api/quality-checks/batch/{batchId}/phase/{phaseId}/create` | ADMIN, QUALITY_WORKER | Create checks for phase |
| GET | `/api/quality-checks/{id}` | ADMIN, QUALITY_WORKER, PRODUCTION_WORKER | Get quality check by ID |
| POST | `/api/quality-checks/{id}/inspect` | ADMIN, QUALITY_WORKER | Record inspection result |
| POST | `/api/quality-checks/{id}/auto-evaluate` | ADMIN, QUALITY_WORKER | Auto-evaluate using rules |
| GET | `/api/quality-checks/batch/{batchId}` | ADMIN, QUALITY_WORKER, PRODUCTION_WORKER | Get checks by batch |
| GET | `/api/quality-checks/phase/{phaseId}` | ADMIN, QUALITY_WORKER, PRODUCTION_WORKER | Get checks by phase |
| GET | `/api/quality-checks/order/{orderId}` | ADMIN, QUALITY_WORKER, PRODUCTION_WORKER | Get checks by order |
| GET | `/api/quality-checks/batch/{batchId}/phase/{phaseId}/result` | ADMIN, QUALITY_WORKER, PRODUCTION_WORKER | Get phase result |
| GET | `/api/quality-checks/batch/{batchId}/summary` | ADMIN, QUALITY_WORKER, PRODUCTION_WORKER | Get batch summary |
| POST | `/api/quality-checks/batch/{batchId}/quarantine` | ADMIN | Quarantine batch |
| POST | `/api/quality-checks/batch/{batchId}/reject` | ADMIN | Reject batch |
| POST | `/api/quality-checks/batch/{batchId}/reprocess` | ADMIN | Request reprocess |
| POST | `/api/quality-checks/batch/{batchId}/phase/{phaseId}/update-status` | ADMIN | Update phase quality status |

---

## Quality Rules Per Phase

| Phase | Checks Created | Typical Ranges |
|-------|---------------|----------------|
| 1 (Raw Material Prep) | Water Quality, Flow Rate | pH 6.5–7.5, flow ≥ 100 L/hr |
| 2 (Cleaning & Sorting) | Sanitation, Particle Size | Sanitation ≥ 95%, particle ≤ 5mm |
| 3 (Extraction & Pressing) | Yield, Particle Size | Yield ≥ 80%, particle ≤ 2mm |
| 4 (Filtering & Clarification) | pH Level, Flow Rate | pH 3.5–4.5, flow ≥ 50 L/hr |
| 5 (Blending & Mixing) | Consistency, Viscosity, Sugar, Aroma | Viscosity 50–200 cP |
| 6 (Pasteurization) | Temperature, Processing Time | Temp 72–85°C, time 15–30 min |
| 7 (Quality Testing) | pH, Brix, Color, Micro, Acidity | Brix 11–13°, pH 3.5–4.5 |
| 8 (Filling & Packaging) | Bottle Integrity, Fill Level, Capping, Seal | Fill 495–505 mL |
| 9 (Labeling & Coding) | Label Accuracy | ≥ 99% |
| 10 (Final Inspection) | Weight, Yield | Weight 495–505g, yield ≥ 85% |

---

## Frontend — Quality Panel

The `ActiveProduction` component includes a quality panel that:

- **Auto-loads quality checks** when a phase enters WAITING_FOR_QUALITY status
- **Displays quality checks** with status indicators (green PASS, red FAIL, yellow PENDING)
- **Shows expected ranges** for each check type (min–max)
- **Records observed values** via inspection form
- **Provides admin actions** on failure: Quarantine, Reject, Reprocess
- **Confirm dialogs** for destructive actions (quarantine, reject, reprocess)
- **Auto-pass all** button for development/testing
- **Simulate failure** button for testing quality gate behavior

---

## SSE Events

Quality-related event types:
- `PHASE_WAITING_FOR_QUALITY` — phase completed, quality checks created
- `QUALITY_CHECK_CREATED` — new quality check created
- `QUALITY_CHECK_PASSED` — check passed inspection
- `QUALITY_CHECK_FAILED` — check failed inspection
- `BATCH_QUARANTINED` — batch placed on hold
- `BATCH_REJECTED` — batch rejected
- `BATCH_REPROCESS_REQUESTED` — reprocessing requested

---

## Database Schema

```sql
CREATE TABLE quality_checks (
    id              BIGSERIAL PRIMARY KEY,
    batch_id        BIGINT NOT NULL,
    phase_id        BIGINT NOT NULL,
    order_id        BIGINT,
    batch_code      VARCHAR(50),
    phase_name      VARCHAR(100),
    order_number    VARCHAR(50),
    check_type      VARCHAR(50) NOT NULL,
    observed_value  DECIMAL(12,4),
    expected_min    DECIMAL(12,4),
    expected_max    DECIMAL(12,4),
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    result          VARCHAR(20),
    mandatory       BOOLEAN NOT NULL DEFAULT TRUE,
    inspector       VARCHAR(100),
    machine         VARCHAR(100),
    failure_reason  TEXT,
    corrective_action TEXT,
    inspection_notes TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    inspected_at    TIMESTAMP
);
```

---

## Tests

20 integration tests in `QualityCheckIntegrationTests.java`:

1. `createQualityChecksForPhase` — creates checks with correct types and defaults
2. `inspectQualityCheck` — records observation, evaluates result
3. `autoEvaluateQualityCheck` — deterministic rule engine evaluation
4. `phaseResultShowsPass` — all mandatory checks PASS → overall PASS
5. `phaseResultShowsFail` — any mandatory FAIL → overall FAIL
6. `phaseResultShowsPending` — no inspections → overall PENDING
7. `batchQualitySummary` — summary across all phases
8. `quarantineBatch` — batch status → QUARANTINED
9. `rejectBatch` — batch status → REJECTED
10. `reprocessPreservesHistory` — reprocess records history
11. `mandatoryFailBlocksProgression` — FAIL blocks verification
12. `verifyRequiresQualityPass` — verification requires all mandatory PASS
13. `batchQuarantineBlocksAllPhases` — QUARANTINED batch blocks all
14. `batchRejectStopsProduction` — REJECTED batch stops production
15. `allMandatoryChecksPass` — multiple phases all PASS
16. `nonMandatoryCheckFailureAllowsProgression` — non-mandatory FAIL doesn't block
17. `qualityCheckAuditTrail` — inspection history preserved
18. `inspectWithoutRangeReturnsConditional` — no range → CONDITIONAL result
19. `multipleCheckTypesPerPhase` — multiple types created
20. `updatePhaseQualityStatus` — manual status update

All 91 tests pass (23 auth + 20 production orders + 17 simulation + 20 quality + 11 unit).
