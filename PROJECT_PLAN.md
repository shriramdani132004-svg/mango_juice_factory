# SMARTFACTORY – Project Implementation Plan

## Overview

This document defines the 12 implementation phases for the SmartFactory Mango
Juice Manufacturing Control System. Each phase builds on the previous one and
produces a testable, working increment.

**Current Phase**: Phase 1 (Architecture + Database Foundation)
**Status**: Complete

---

## Phase 1 — Architecture + Database Foundation

**Goal**: Establish the project structure, database schema, and Docker environment.

**Deliverables**:
- [x] Spring Boot project scaffold (Maven, Java 17)
- [x] React project scaffold (Vite, React 18)
- [x] PostgreSQL schema via Flyway migrations (V1__init_schema.sql)
- [x] Seed data for 500 machines, 10 production lines, recipes, raw materials
- [x] docker-compose.yml with PostgreSQL + backend + frontend
- [x] .env.example for environment variables
- [x] Architecture and project plan documentation

**Database Tables (V1)**:
- `users` — Admin/Worker accounts
- `machines` — 500 machines with capabilities and states
- `machine_telemetry` — Time-series health data
- `production_orders` — Top-level orders
- `production_plans` — Order breakdown
- `production_batches` — Individual batches
- `production_jobs` — Machine-level tasks
- `recipes` — Product recipes
- `recipe_ingredients` — Recipe line items
- `bill_of_materials` — Materials per plan
- `raw_materials` — Raw material catalog
- `packaging_materials` — Packaging material catalog
- `inventory_items` — Current stock levels
- `quality_checks` — QC inspection results
- `workers` — Factory worker profiles
- `incidents` — Failure/anomaly records
- `maintenance_records` — Maintenance history
- `audit_logs` — System audit trail
- `warehouse_slots` — Finished goods storage

**Tests**: Schema migration runs cleanly. Docker stack starts.

---

## Phase 2 — Spring Boot Backend Foundation

**Goal**: Working backend with health check, configuration, and SSE setup.

**Deliverables**:
- [ ] Spring Boot application starts on port 8080
- [ ] application.yml with dev and prod profiles
- [ ] Global exception handler
- [ ] CORS configuration
- [ ] SSE emitter configuration and EventService
- [ ] Health check endpoint (`GET /api/health`)
- [ ] Swagger/OpenAPI documentation enabled
- [ ] Logging configuration (structured JSON for production)

**Depends on**: Phase 1

**Tests**: Backend starts, `/api/health` returns 200, SSE endpoint connects.

---

## Phase 3 — Entities + Inventory + Recipes/BOM

**Goal**: Full JPA entity layer with repositories and basic service CRUD.

**Deliverables**:
- [ ] All JPA entities mapped to V1 schema
- [ ] All enums (MachineState, MachineCapability, ProductionStage, OrderStatus, etc.)
- [ ] Spring Data JPA repositories for all entities
- [ ] InventoryService: stock check, deduct, replenish
- [ ] RecipeService: CRUD, ingredient management
- [ ] MaterialService: raw materials and packaging materials CRUD
- [ ] DTOs for all request/response objects

**Depends on**: Phase 2

**Tests**: CRUD operations for inventory, recipes, and materials via integration tests.

---

## Phase 4 — Authentication + Admin/RBAC

**Goal**: Secure API with JWT authentication and role-based access.

**Deliverables**:
- [ ] User entity and UserRepository
- [ ] BCrypt password hashing
- [ ] JWT token generation and validation
- [ ] Spring Security filter chain
- [ ] AuthController (login, register, refresh)
- [ ] Role-based endpoint security (ADMIN, WORKER)
- [ ] Admin seed account creation on startup
- [ ] AuditLogService: log all state-changing operations

**Depends on**: Phase 3

**Tests**: Can login, get token, access protected endpoints, roles enforced.

---

## Phase 5 — Production Planning + Job Generation

**Goal**: Convert a production order into a plan with batches and jobs.

**Deliverables**:
- [ ] ProductionOrderService: create order with validation
- [ ] ProductionPlanService: generate plan from order
  - Calculate batch count from input kg and recipe yield
  - Generate Bill of Materials
  - Calculate material requirements
  - Estimate timeline
- [ ] ProductionJobService: generate 10 jobs per batch (one per stage)
- [ ] Material availability check before plan approval
- [ ] Order lifecycle: CREATED → PLANNED → IN_PROGRESS → COMPLETED/CANCELLED
- [ ] Batch lifecycle: PENDING → IN_PROGRESS → COMPLETED → FAILED
- [ ] Job lifecycle: QUEUED → ASSIGNED → RUNNING → COMPLETED/FAILED

**Depends on**: Phase 4

**Tests**: Create order → plan generated → correct batch count → correct job count per batch.

---

## Phase 6 — 500-Machine Simulation Engine

**Goal**: Core simulation engine that manages 500 machines and executes jobs.

**Deliverables**:
- [ ] SimulationEngine: main loop with ScheduledExecutorService (1 tick/second)
- [ ] MachineActor: per-machine state machine update logic
- [ ] ProductionLine: grouping of machines (10 lines × 50 machines)
- [ ] JobScheduler: assigns queued jobs to idle machines
- [ ] Machine allocation algorithm (capability match → health sort → availability)
- [ ] Job execution simulation (each stage has configurable duration)
- [ ] Telemetry generation per tick (temperature, pressure, speed, vibration)
- [ ] Health score degradation model (based on runtime and failures)
- [ ] Simulation start/stop/pause controls
- [ ] Configurable simulation speed multiplier

**Depends on**: Phase 5

**Tests**: Simulation starts, machines transition states, jobs progress through stages.

---

## Phase 7 — Real-Time Production Execution

**Goal**: End-to-end production execution with real-time updates to frontend.

**Deliverables**:
- [ ] EventService: publish domain events to all SSE connections
- [ ] Event throttling (telemetry at 3s intervals, state changes immediate)
- [ ] Production execution flow:
  1. Admin starts order → plan activates
  2. Batches queued to production lines
  3. Jobs assigned to machines
  4. Machines process jobs (simulated time)
  5. Jobs complete → batch advances stage
  6. All stages done → batch moves to warehouse
  7. All batches done → order complete
- [ ] Batch stage advancement logic
- [ ] WarehouseService: auto-store finished goods
- [ ] DashboardService: aggregate stats (active orders, machine utilization, etc.)

**Depends on**: Phase 6

**Tests**: Full order lifecycle from creation to warehouse, SSE events fire at each transition.

---

## Phase 8 — React Admin Control Center

**Goal**: Complete admin dashboard with all management screens.

**Deliverables**:
- [ ] Main layout with sidebar navigation
- [ ] Dashboard page: KPI cards, machine status chart, production timeline
- [ ] Production Orders page: list, create, detail view
- [ ] Machine Control page: grid view of 500 machines, filter by state/line/capability
- [ ] Machine detail: telemetry charts, current job, history
- [ ] Inventory page: raw materials, packaging materials, stock levels
- [ ] Quality Control page: inspection list, create inspection
- [ ] Warehouse page: slot map, finished goods inventory
- [ ] Workers page: list, assign to incidents
- [ ] Audit Logs page: searchable log viewer
- [ ] SSE integration: live dashboard updates without refresh
- [ ] Login page with JWT auth
- [ ] React Query for all API calls
- [ ] Responsive design (works on tablet+)

**Depends on**: Phase 7

**Tests**: Can login, create order, watch machines process in real-time, view all pages.

---

## Phase 9 — Quality Control + Batch Traceability

**Goal**: Quality inspection workflow and full batch traceability.

**Deliverables**:
- [ ] QualityService: create inspection, record result
- [ ] QC rules: automatic check at stage 7 (pasteurize → inspect)
- [ ] Quality results: PASS, FAIL, REWORK
- [ ] Failed batch handling: flag, route to rework or discard
- [ ] Batch traceability: full history of every stage, machine, time, result
- [ ] Quality dashboard: pass/fail rates, common failure reasons
- [ ] Batch genealogy: link raw materials to finished goods
- [ ] Quality check timestamps and inspector attribution

**Depends on**: Phase 8

**Tests**: QC inspection recorded, failed batch handled, batch history is complete.

---

## Phase 10 — Machine Failure + Maintenance + Recovery

**Goal**: Simulate machine failures, maintenance, and automatic recovery.

**Deliverables**:
- [ ] FailureDetector: detect failures from telemetry and health scores
- [ ] Random failure injection with configurable rate
- [ ] Incident creation on failure
- [ ] Automatic job reassignment on machine failure
- [ ] Maximum reassignment attempts (3) before escalation
- [ ] Mandatory maintenance after 5 consecutive failures
- [ ] MaintenanceRecord: schedule, assign worker, complete
- [ ] Worker flow: receive notification, inspect, fix, close
- [ ] Machine health restoration after maintenance
- [ ] Failure/recovery analytics (MTBF, MTTR)

**Depends on**: Phase 9

**Tests**: Machine fails → incident created → job reassigned → worker fixes → machine recovers.

---

## Phase 11 — Analytics + Warehouse + Demo Polish

**Goal**: Analytics dashboard, warehouse management, and production-ready polish.

**Deliverables**:
- [ ] Analytics page: production throughput, OEE, downtime analysis
- [ ] Machine utilization charts (by line, by capability)
- [ ] Inventory turnover and consumption forecasting
- [ ] Warehouse slot management: assign slots, track fill levels
- [ ] Finished goods dispatch simulation
- [ ] Demo data generator: quick-seed for live demo
- [ ] Production order presets (quick-create common orders)
- [ ] Loading states, error boundaries, empty states
- [ ] Toast notifications for real-time events
- [ ] Keyboard shortcuts for power users
- [ ] Dark mode toggle

**Depends on**: Phase 10

**Tests**: Analytics data is accurate, warehouse functions correctly, UI is polished.

---

## Phase 12 — Testing + Docker + Deployment + GitHub Release

**Goal**: Production-ready deployment with full test coverage.

**Deliverables**:
- [ ] Unit tests for all services (>80% coverage)
- [ ] Integration tests for API endpoints
- [ ] E2E smoke test (Cypress or Playwright)
- [ ] Dockerfile for backend (multi-stage build)
- [ ] Dockerfile for frontend (nginx)
- [ ] docker-compose.yml for full local stack
- [ ] GitHub Actions CI: build, test, lint on every push
- [ ] GitHub Actions CD: deploy frontend to GitHub Pages
- [ ] Backend deployment to Railway/Render
- [ ] Production environment configuration
- [ ] README.md with setup instructions, architecture diagram, screenshots
- [ ] API documentation (Swagger UI accessible)
- [ ] Performance test: 500 machines running simultaneously
- [ ] GitHub Release with tagged version

**Depends on**: Phase 11

**Tests**: Full test suite passes, Docker starts locally, live demo accessible.

---

## Phase Dependency Graph

```
Phase 1  ──▶ Phase 2  ──▶ Phase 3  ──▶ Phase 4
                                          │
                                          ▼
                                     Phase 5  ──▶ Phase 6  ──▶ Phase 7
                                                                    │
                                                                    ▼
                                                               Phase 8
                                                                    │
                                                                    ▼
                              Phase 10 ◀── Phase 9
                                  │
                                  ▼
                              Phase 11 ──▶ Phase 12
```

---

## Estimated Timeline

| Phase | Description                        | Est. Effort |
|-------|------------------------------------|-------------|
| 1     | Architecture + DB Foundation       | 1 day       |
| 2     | Backend Foundation                 | 1 day       |
| 3     | Entities + Inventory + Recipes     | 2 days      |
| 4     | Authentication + RBAC              | 1 day       |
| 5     | Production Planning + Jobs         | 2 days      |
| 6     | 500-Machine Simulation Engine      | 3 days      |
| 7     | Real-Time Execution                | 2 days      |
| 8     | React Admin Control Center         | 3 days      |
| 9     | Quality Control + Traceability     | 2 days      |
| 10    | Failure + Maintenance + Recovery   | 2 days      |
| 11    | Analytics + Warehouse + Polish     | 2 days      |
| 12    | Testing + Docker + Deployment      | 2 days      |
|       | **Total**                          | **~23 days**|

---

## Success Criteria

1. Admin can create a production order with mango input, product type, and bottle size.
2. System generates a production plan with batches and material requirements.
3. 500 simulated machines process batches through 10 stages.
4. Real-time updates stream to the browser via SSE.
5. Machine failures trigger automatic job reassignment.
6. Quality inspections are recorded at stage 7.
7. Finished goods appear in warehouse inventory.
8. Full audit trail of all operations.
9. Deployed live demo accessible via browser.
10. Architecture is clean enough to explain in an interview.

---

*This plan is a living document. Update status as phases are completed.*
