# Production Execution — Part 6 Implementation

## Overview

Part 6 adds the live production execution engine to SmartFactory. Once an order is approved, the system can run each production phase on allocated machines, stream real-time events via SSE, and present a live production dashboard in the frontend.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Frontend (React)                                           │
│  • ActiveProduction component                               │
│  • SSE via EventSource (production-event name)              │
│  • Controls: Start / Pause / Resume / Stop / Verify         │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP + SSE
┌────────────────────────▼────────────────────────────────────┐
│  ProductionPhaseController (10 endpoints)                   │
├─────────────────────────────────────────────────────────────┤
│  PhaseExecutionService — orchestrates phase lifecycle       │
│  ├── MachineAllocationService — assigns machines to phases  │
│  ├── ProductionEventService — broadcasts SSE events         │
│  └── SimulationEngine — tick-based execution loop           │
│      ├── ConcurrentHashMap<Long, PhaseExecutionState>       │
│      ├── ScheduledExecutorService (configurable tick rate)  │
│      └── Telemetry generation (temp, rpm, vibration, power) │
└─────────────────────────────────────────────────────────────┘
```

---

## Key Components

### SimulationEngine (`SimulationEngine.java`)
- Single `ScheduledExecutorService` with configurable tick interval (default 500ms) and speed multiplier (default 20x)
- Each tick: advances active phases, updates progress, generates telemetry, publishes SSE events
- Phase completion triggers automatic state transition (COMPLETED → WAITING_FOR_VERIFICATION)
- Machine telemetry: temperature, RPM, vibration, power consumption

### MachineAllocationService (`MachineAllocationService.java`)
- Deterministic allocation: capability match → READY over IDLE → higher health → higher production rate → lower machine code
- Min health threshold: 50%
- Up to 3 machines allocated per phase

### PhaseExecutionService (`PhaseExecutionService.java`)
- Start: validates machine allocation, creates PhaseExecutionState, begins tick processing
- Pause/Resume: toggles execution state, emits PAUSED/RESUMED events
- Stop: marks phase as FAILED (emergency_stop), stops machine execution
- Verify: validates completion, unlocks next phase (READY or COMPLETED for last phase)

### ProductionEventService (`ProductionEventService.java`)
- SSE broadcasting via `SseEmitter`
- Per-order subscriptions + global broadcast
- Events typed: PHASE_PROGRESS, PHASE_COMPLETED, PHASE_PAUSED, PHASE_RESUMED, PHASE_FAILED, MACHINE_TELEMETRY, etc.

---

## Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/production-phases/{id}/start` | MANAGER/OPERATOR | Start a production phase |
| POST | `/api/production-phases/{id}/pause` | MANAGER/OPERATOR | Pause active phase |
| POST | `/api/production-phases/{id}/resume` | MANAGER/OPERATOR | Resume paused phase |
| POST | `/api/production-phases/{id}/stop` | MANAGER/OPERATOR | Emergency stop |
| POST | `/api/production-phases/{id}/completion` | — | Get completion details |
| POST | `/api/production-phases/{id}/verify` | MANAGER | Verify completed phase |
| GET | `/api/production-phases/{id}/status` | — | Get phase execution status |
| GET | `/api/production-phases/active` | — | List all active executions |
| GET | `/api/production-phases/events/{orderId}` | — | SSE stream for order |
| GET | `/api/production-phases/events` | — | SSE global event stream |

---

## Phase Lifecycle

```
LOCKED → READY → RUNNING → COMPLETED → WAITING_FOR_VERIFICATION → VERIFIED
                                ↓                                    ↓
                            FAILED/PAUSED                      READY (next phase)
                                ↓
                            RUNNING (resume)
```

- Phase 1 starts as READY after order approval
- Phases 2–10 remain LOCKED until previous phase is VERIFIED
- One phase runs at a time
- Admin (MANAGER) must verify before next phase unlocks

---

## Material Flow

| Phase | Type | Waste Rate |
|-------|------|-----------|
| 1 | Raw Material Preparation | 2% |
| 2 | Cleaning & Sorting | 3% |
| 3 | Extraction & Pressing | 15% |
| 4 | Filtering & Clarification | 2% |
| 5 | Blending & Mixing | 1% |
| 6 | Pasteurization | 0.5% |
| 7 | Quality Testing | 3% |
| 8 | Filling & Packaging | 1% |
| 9 | Labeling & Coding | 0.5% |
| 10 | Final Inspection & Storage | 0.2% |

---

## SSE Event Format

```json
{
  "eventType": "PHASE_PROGRESS",
  "orderId": 1,
  "orderNumber": "MO-20260916-001",
  "batchId": 1,
  "batchCode": "BATCH-001",
  "phaseId": 1,
  "phaseNumber": 1,
  "phaseType": "RAW_MATERIAL_PREPARATION",
  "machineId": 101,
  "machineCode": "MIXER-001",
  "message": "Phase 1 in progress",
  "data": {
    "progressPercentage": 65.3,
    "currentQuantity": 3265.0,
    "targetQuantity": 5000.0,
    "elapsedTimeSeconds": 120,
    "estimatedTimeRemainingSeconds": 64
  }
}
```

Event names: `production-event` (all events)
Event types: `PHASE_STARTED`, `PHASE_PROGRESS`, `PHASE_PAUSED`, `PHASE_RESUMED`, `PHASE_COMPLETED`, `PHASE_FAILED`, `MACHINE_ALLOCATED`, `MACHINE_RELEASED`, `MACHINE_TELEMETRY`, `PHASE_VERIFIED`

---

## Frontend — ActiveProduction Screen

The `ActiveProduction` component in `App.jsx` provides:

- **SSE Lifecycle**: Connects on mount, auto-reconnects on error, disconnects on unmount
- **Phase Pipeline**: Visual pipeline showing all 10 phases with status indicators
- **Live Progress**: Real-time progress bar with percentage and ETA
- **Machine Status Cards**: Allocated machines with current telemetry readings
- **Telemetry Grid**: Temperature, RPM, vibration, power for all active machines
- **Event Stream**: Rolling event log with timestamps
- **Controls**: Start, Pause, Resume, Stop (with confirmation), Verify buttons
- **Phase Completion Card**: Completion details when phase finishes
- **Verify Result**: Shows next phase info after admin verification

---

## Configuration

```yaml
# application.yml
simulation:
  speed-multiplier: 20        # 20x faster than real-time
  tick-interval-ms: 500       # 500ms between ticks
  mode: DEMO                  # DEMO mode for testing
```

---

## Tests

17 integration tests in `SimulationIntegrationTests.java`:
- Start phase, verify RUNNING + SSE progress events
- Pause phase, verify PAUSED state
- Resume phase, verify progress continues
- Stop phase, verify FAILED state
- RBAC: OPERATOR can start/pause/resume, MANAGER can verify
- Phase gating: cannot start LOCKED phase
- Error cases: not found, wrong order, etc.

All 71 tests pass (23 auth + 20 production orders + 17 simulation + 11 unit).
