# SMARTFACTORY – Mango Juice Manufacturing Control System

## 1. System Overview

SmartFactory is a browser-based manufacturing execution system (MES) that simulates
a complete mango juice production facility. An Admin creates production orders, and
the system automatically coordinates 500 simulated machines through a 10-stage
manufacturing workflow — from raw mango receiving to warehouse storage.

---

## 2. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     BROWSER (React SPA)                     │
│                   Admin Control Center                      │
│  Dashboard │ Orders │ Machines │ Inventory │ Quality │ Logs │
└────────┬────────────────────────────────────┬──────────────┘
         │ REST API (HTTP)                    │ SSE (real-time)
         │                                    │
┌────────▼────────────────────────────────────▼──────────────┐
│                  SPRING BOOT BACKEND                        │
│                                                             │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │ REST API    │  │ SSE Event    │  │ Simulation       │  │
│  │ Controllers │  │ Broadcaster  │  │ Engine           │  │
│  └──────┬──────┘  └──────┬───────┘  └────────┬─────────┘  │
│         │                │                    │             │
│  ┌──────▼────────────────▼────────────────────▼─────────┐  │
│  │              SERVICE LAYER                           │  │
│  │  OrderService │ MachineService │ InventoryService    │  │
│  │  ProductionPlanService │ QualityService              │  │
│  │  WarehouseService │ WorkerService │ EventService     │  │
│  └──────┬──────────────────────────────────┬────────────┘  │
│         │                                  │               │
│  ┌──────▼──────────────┐    ┌──────────────▼────────────┐  │
│  │  REPOSITORY LAYER   │    │  SIMULATION ENGINE        │  │
│  │  JPA / Hibernate    │    │  ScheduledExecutorService │  │
│  │  Spring Data JPA    │    │  500 machine actors       │  │
│  └──────┬──────────────┘    └───────────────────────────┘  │
│         │                                                   │
└─────────┼───────────────────────────────────────────────────┘
          │
┌─────────▼───────────────────────────────────────────────────┐
│                     POSTGRESQL                              │
│  Production data, inventory, machine states, audit logs     │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Technology Stack

| Layer        | Technology                | Purpose                              |
|--------------|---------------------------|--------------------------------------|
| Frontend     | React 18 + Vite           | SPA Admin Control Center             |
| UI Library   | Tailwind CSS + Recharts   | Styling + data visualization         |
| State Mgmt   | React Query (TanStack)    | Server state + caching               |
| Backend      | Java 17 + Spring Boot 3   | REST API + simulation engine         |
| Real-time    | SSE (Server-Sent Events)  | Push updates from backend to frontend|
| Database     | PostgreSQL 16             | Persistent storage                   |
| ORM          | Hibernate / Spring Data JPA | Database access                    |
| Migration    | Flyway                    | Schema versioning                    |
| Build (BE)   | Maven                     | Backend build                        |
| Build (FE)   | npm / Vite                | Frontend build                       |
| Container    | Docker + Docker Compose   | Local dev + deployment               |
| CI/CD        | GitHub Actions            | Automated build + deploy             |
| Hosting FE   | GitHub Pages              | Static frontend hosting              |
| Hosting BE   | Railway / Render / Fly.io | Backend + PostgreSQL hosting         |

### Why SSE over WebSocket?

- SSE is unidirectional (server → client) which matches our use case: the backend
  pushes machine state changes and production events to the admin dashboard.
- SSE works over standard HTTP, simpler to configure with reverse proxies.
- SSE auto-reconnects natively in the browser (EventSource API).
- WebSocket would only be needed for bidirectional chat-like features, which we
  don't have. Admin commands go through REST API.

### Why not Redis yet?

Redis is a good fit for pub/sub across multiple backend instances and as a message
broker between microservices. Since we start with a single Spring Boot instance,
PostgreSQL handles everything. Redis will be introduced when:
- We need pub/sub for horizontal scaling
- We want to cache hot machine telemetry data
- We want a lightweight job queue for simulation ticks

---

## 4. Folder Structure

```
mango_juice_factory/
│
├── ARCHITECTURE.md              # This file
├── PROJECT_PLAN.md              # 12-phase implementation plan
├── README.md                    # Project overview + quick start
├── docker-compose.yml           # Full local dev stack
├── .gitignore
│
├── backend/                     # Spring Boot application
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/
│       │   ├── java/com/smartfactory/
│       │   │   ├── SmartFactoryApplication.java
│       │   │   │
│       │   │   ├── config/                    # Spring configuration
│       │   │   │   ├── SseConfig.java
│       │   │   │   ├── SchedulerConfig.java
│       │   │   │   └── SecurityConfig.java
│       │   │   │
│       │   │   ├── controller/                # REST controllers
│       │   │   │   ├── ProductionOrderController.java
│       │   │   │   ├── MachineController.java
│       │   │   │   ├── InventoryController.java
│       │   │   │   ├── QualityController.java
│       │   │   │   ├── WorkerController.java
│       │   │   │   ├── DashboardController.java
│       │   │   │   ├── WarehouseController.java
│       │   │   │   ├── EventController.java       # SSE endpoint
│       │   │   │   └── AuthController.java
│       │   │   │
│       │   │   ├── dto/                       # Request/Response DTOs
│       │   │   │   ├── request/
│       │   │   │   └── response/
│       │   │   │
│       │   │   ├── entity/                    # JPA entities
│       │   │   │   ├── Machine.java
│       │   │   │   ├── MachineTelemetry.java
│       │   │   │   ├── ProductionOrder.java
│       │   │   │   ├── ProductionPlan.java
│       │   │   │   ├── ProductionJob.java
│       │   │   │   ├── Batch.java
│       │   │   │   ├── Recipe.java
│       │   │   │   ├── RecipeIngredient.java
│       │   │   │   ├── BillOfMaterials.java
│       │   │   │   ├── InventoryItem.java
│       │   │   │   ├── RawMaterial.java
│       │   │   │   ├── PackagingMaterial.java
│       │   │   │   ├── QualityCheck.java
│       │   │   │   ├── Worker.java
│       │   │   │   ├── Incident.java
│       │   │   │   ├── MaintenanceRecord.java
│       │   │   │   ├── AuditLog.java
│       │   │   │   ├── WarehouseSlot.java
│       │   │   │   └── User.java
│       │   │   │
│       │   │   ├── enums/                     # Enumerations
│       │   │   │   ├── MachineState.java
│       │   │   │   ├── MachineCapability.java
│       │   │   │   ├── ProductionStage.java
│       │   │   │   ├── OrderStatus.java
│       │   │   │   ├── JobStatus.java
│       │   │   │   ├── BatchStatus.java
│       │   │   │   ├── QualityResult.java
│       │   │   │   └── Severity.java
│       │   │   │
│       │   │   ├── repository/                # Spring Data JPA repos
│       │   │   │
│       │   │   ├── service/                   # Business logic
│       │   │   │   ├── ProductionOrderService.java
│       │   │   │   ├── ProductionPlanService.java
│       │   │   │   ├── ProductionJobService.java
│       │   │   │   ├── MachineService.java
│       │   │   │   ├── InventoryService.java
│       │   │   │   ├── QualityService.java
│       │   │   │   ├── WarehouseService.java
│       │   │   │   ├── WorkerService.java
│       │   │   │   ├── IncidentService.java
│       │   │   │   ├── AuditLogService.java
│       │   │   │   ├── DashboardService.java
│       │   │   │   └── EventService.java       # SSE broadcast
│       │   │   │
│       │   │   ├── simulation/                # 500-machine simulation
│       │   │   │   ├── SimulationEngine.java
│       │   │   │   ├── MachineActor.java
│       │   │   │   ├── ProductionLine.java
│       │   │   │   ├── SimulationTick.java
│       │   │   │   ├── JobScheduler.java
│       │   │   │   └── FailureDetector.java
│       │   │   │
│       │   │   ├── security/                  # Auth + RBAC
│       │   │   │   ├── JwtTokenProvider.java
│       │   │   │   ├── JwtAuthFilter.java
│       │   │   │   └── UserDetailsServiceImpl.java
│       │   │   │
│       │   │   └── exception/                 # Global error handling
│       │   │       ├── GlobalExceptionHandler.java
│       │   │       └── ResourceNotFoundException.java
│       │   │
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       ├── application-prod.yml
│       │       └── db/migration/              # Flyway migrations
│       │           ├── V1__init_schema.sql
│       │           └── V2__seed_data.sql
│       │
│       └── test/
│           └── java/com/smartfactory/
│
├── frontend/                    # React application
│   ├── package.json
│   ├── vite.config.js
│   ├── Dockerfile
│   ├── index.html
│   └── src/
│       ├── main.jsx
│       ├── App.jsx
│       │
│       ├── api/                 # API client + SSE connection
│       │   ├── axios.js
│       │   ├── sse.js
│       │   ├── orders.js
│       │   ├── machines.js
│       │   ├── inventory.js
│       │   └── quality.js
│       │
│       ├── components/          # Reusable UI components
│       │   ├── layout/
│       │   │   ├── Sidebar.jsx
│       │   │   ├── Header.jsx
│       │   │   └── MainLayout.jsx
│       │   ├── charts/
│       │   │   ├── MachineStatusChart.jsx
│       │   │   ├── ProductionTimeline.jsx
│       │   │   └── InventoryLevels.jsx
│       │   ├── machines/
│       │   │   ├── MachineGrid.jsx
│       │   │   ├── MachineCard.jsx
│       │   │   └── MachineDetail.jsx
│       │   ├── orders/
│       │   │   ├── OrderForm.jsx
│       │   │   ├── OrderList.jsx
│       │   │   └── OrderDetail.jsx
│       │   ├── quality/
│       │   │   ├── QualityPanel.jsx
│       │   │   └── InspectionForm.jsx
│       │   └── common/
│       │       ├── StatusBadge.jsx
│       │       ├── DataTable.jsx
│       │       └── EventLog.jsx
│       │
│       ├── pages/               # Route-level components
│       │   ├── Dashboard.jsx
│       │   ├── ProductionOrders.jsx
│       │   ├── MachineControl.jsx
│       │   ├── Inventory.jsx
│       │   ├── QualityControl.jsx
│       │   ├── Warehouse.jsx
│       │   ├── Workers.jsx
│       │   ├── AuditLogs.jsx
│       │   └── Login.jsx
│       │
│       ├── hooks/               # Custom React hooks
│       │   ├── useSse.js
│       │   ├── useMachines.js
│       │   └── useProduction.js
│       │
│       ├── store/               # Client state (if needed)
│       │   └── authStore.js
│       │
│       └── utils/
│           ├── constants.js
│           └── formatters.js
│
└── docs/                        # Additional documentation
    └── api-reference.md
```

---

## 5. Domain Entities & Relationships

### 5.1 Entity Relationship Diagram

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐
│   User   │     │ProductionOrder│     │   Recipe     │
│──────────│     │──────────────│     │──────────────│
│ id       │     │ id           │     │ id           │
│ username │     │ orderNumber  │     │ name         │
│ password │     │ mangoInputKg │     │ bottleSizeMl │
│ role     │     │ productType  │     │ yieldPerBatch│
│ enabled  │     │ targetQty    │     │ stageCount   │
└──────────┘     │ status       │     └──────┬───────┘
     │           │ recipe ────────────────┐  │
     │           │ createdAt  │          │  │
     │           └──────┬─────┘          │  │
     │                  │                │  │
     │           ┌──────▼─────┐    ┌─────▼──▼───────┐
     │           │ProductionPlan│   │RecipeIngredient│
     │           │──────────────│   │───────────────│
     │           │ id           │   │ id            │
     │           │ order ───────│   │ recipe ───────│
     │           │ totalBatches │   │ rawMaterial ──│
     │           │ plannedStart │   │ quantityKg    │
     │           │ plannedEnd   │   │ unit          │
     │           └──────┬───────┘   └───────────────┘
     │                  │
     │    ┌─────────────┼─────────────┐
     │    │             │             │
     │  ┌─▼───────┐ ┌───▼────┐ ┌─────▼──────┐
     │  │Batch    │ │ProdJob │ │BillOfMat.  │
     │  │─────────│ │────────│ │────────────│
     │  │ id      │ │ id     │ │ id         │
     │  │ plan ───│ │ plan───│ │ recipe ────│
     │  │ batchNo │ │ machine│ │ materials[]│
     │  │ status  │ │ stage  │ └────────────┘
     │  │ yield   │ │ status │
     │  └────┬────┘ │ startAt│
     │       │      │ endAt  │
     │       │      └────────┘
     │       │
     │  ┌────▼────────┐   ┌──────────────┐
     │  │QualityCheck │   │   Machine    │
     │  │─────────────│   │──────────────│
     │  │ id          │   │ id           │
     │  │ batch ──────│   │ machineCode  │
     │  │ stage       │   │ name         │
     │  │ result      │   │ capability   │
     │  │ inspector ──│   │ state        │
     │  │ notes       │   │ currentJob───│
     │  │ timestamp   │   │ productionLine│
     │  └─────────────┘   │ healthScore  │
     │                    │ lastPingAt   │
     │                    │ failureCount │
     │                    └──────┬───────┘
     │                           │
     │                    ┌──────▼───────┐
     │                    │MachineTel.   │
     │                    │──────────────│
     │                    │ id           │
     │                    │ machine ─────│
     │                    │ temperature  │
     │                    │ pressure     │
     │                    │ speed        │
     │                    │ vibration    │
     │                    │ timestamp    │
     │                    └──────────────┘
     │
     │  ┌─────────────┐  ┌──────────────┐  ┌──────────┐
     │  │InventoryItem│  │WarehouseSlot │  │ Worker   │
     │  │─────────────│  │──────────────│  │──────────│
     │  │ id          │  │ id           │  │ id       │
     │  │ material ───│  │ batch ───────│  │ name     │
     │  │ quantityKg  │  │ slotCode     │  │ role     │
     │  │ unit        │  │ quantity     │  │ skills[] │
     │  │ reorderLevel│  │ location     │  │ shift    │
     │  │ supplier    │  │ receivedAt   │  │ status   │
     │  └─────────────┘  └──────────────┘  └──────────┘
     │
     │  ┌──────────────┐  ┌─────────────────┐
     │  │  Incident    │  │ MaintenanceRec. │
     │  │──────────────│  │─────────────────│
     │  │ id           │  │ id              │
     │  │ machine ─────│  │ machine ────────│
     │  │ severity     │  │ type            │
     │  │ description  │  │ scheduledAt     │
     │  │ resolvedAt   │  │ completedAt     │
     │  │ assignedTo ──│  │ assignedTo ─────│
     │  └──────────────┘  └─────────────────┘
     │
     └──── ┌───────────┐
           │ AuditLog  │
           │───────────│
           │ id        │
           │ user ─────│
           │ action    │
           │ entity    │
           │ entityId  │
           │ timestamp │
           │ details   │
           └───────────┘
```

### 5.2 Entity Descriptions

| Entity             | Purpose                                                       |
|--------------------|---------------------------------------------------------------|
| User               | Admin (primary) or Worker accounts with roles                 |
| ProductionOrder    | Top-level request: "make X mango beverage from Y kg mangoes"  |
| ProductionPlan     | Breakdown of an order into batches and timeline               |
| ProductionJob      | A single machine-level task for one stage of one batch        |
| Batch              | One production unit flowing through all 10 stages             |
| Recipe             | Template: what goes into a product, what stages, yield       |
| RecipeIngredient   | One ingredient line in a recipe                               |
| BillOfMaterials    | Full materials list needed for a production plan              |
| RawMaterial        | Mangoes, sugar, citric acid, water, etc.                     |
| PackagingMaterial  | Bottles, caps, labels, cartons, shrink wrap                   |
| InventoryItem      | Current stock of any material (raw or packaging)              |
| Machine            | One of the 500 simulated machines                            |
| MachineTelemetry   | Time-series health data per machine                           |
| ProductionStage    | Enum: RECEIVE → WASH → PEEL → FILTER → BLEND → PASTEURIZE → INSPECT → FILL → LABEL → WAREHOUSE |
| QualityCheck       | Inspection result at any stage for any batch                  |
| Worker             | Factory worker who can operate, inspect, or maintain machines |
| Incident           | Machine failure or production anomaly                         |
| MaintenanceRecord  | Scheduled or reactive maintenance on a machine                |
| AuditLog           | Who did what, when                                            |
| WarehouseSlot      | Physical storage location for finished goods                  |

---

## 6. The 10-Stage Manufacturing Workflow

```
┌─────────────────────────────────────────────────────────────┐
│                  10-STAGE PRODUCTION LINE                    │
│                                                             │
│  STAGE 1         STAGE 2         STAGE 3         STAGE 4   │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐│
│  │ RECEIVE  │──▶│WASH/SORT │──▶│PEEL/PULP │──▶│  FILTER  ││
│  │ Mangoes  │   │ Clean &  │   │ Remove   │   │ Remove   ││
│  │          │   │ grade    │   │ skin,     │   │ pulp     ││
│  │          │   │          │   │ extract   │   │ fibers   ││
│  └──────────┘   └──────────┘   └──────────┘   └──────────┘│
│                                                             │
│  STAGE 5         STAGE 6         STAGE 7         STAGE 8   │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐│
│  │  BLEND   │──▶│PASTEURIZE│──▶│ QUALITY  │──▶│ FILL/CAP ││
│  │ Mix with │   │ Heat     │   │ INSPECT  │   │ Fill     ││
│  │ sugar,   │   │ treatment│   │ Lab test │   │ bottles, ││
│  │ water    │   │          │   │          │   │ seal     ││
│  └──────────┘   └──────────┘   └──────────┘   └──────────┘│
│                                                             │
│  STAGE 9         STAGE 10                                │
│  ┌──────────┐   ┌──────────┐                             │
│  │LABEL/PACK│──▶│WAREHOUSE │                             │
│  │ Apply    │   │ Store    │                             │
│  │ labels,  │   │ finished │                             │
│  │ carton   │   │ goods    │                             │
│  └──────────┘   └──────────┘                             │
└─────────────────────────────────────────────────────────────┘
```

### Stage Details

| #  | Stage           | Input                | Output               | Machine Type         | Duration (sim) |
|----|-----------------|----------------------|----------------------|----------------------|----------------|
| 1  | Receive         | Raw mangoes (kg)     | Graded mangoes       | Receiver/Scale       | 2 min          |
| 2  | Wash & Sort     | Graded mangoes       | Clean sorted mangoes | Washer/Sorter        | 3 min          |
| 3  | Peel & Pulp     | Clean mangoes        | Mango pulp           | Peeler/Pulper        | 4 min          |
| 4  | Filter          | Mango pulp           | Filtered juice       | Filter               | 2 min          |
| 5  | Blend           | Filtered juice       | Juice blend          | Blender              | 3 min          |
| 6  | Pasteurize      | Juice blend          | Pasteurized juice    | Pasteurizer          | 5 min          |
| 7  | Quality Insp.   | Pasteurized juice    | QC-passed juice      | Inspection Station   | 3 min          |
| 8  | Fill & Cap      | QC-passed juice      | Filled bottles       | Filler/Capper        | 3 min          |
| 9  | Label & Pack    | Filled bottles       | Packaged product     | Labeler/Packer       | 2 min          |
| 10 | Warehouse       | Packaged product     | Stored inventory     | Conveyor/AGV         | 2 min          |

### Transition Rules

1. A batch must complete stage N before advancing to stage N+1.
2. If a batch fails QC at stage 7, it is flagged and routed to rework or discard.
3. If the assigned machine fails during a stage, the job is reassigned.
4. Each stage produces telemetry and audit log entries.

---

## 7. Machine Simulation Model

### 7.1 Machine Properties

```java
Machine {
    Long id;
    String machineCode;          // e.g. "WASH-042", "BLEND-007"
    String name;
    MachineCapability capability; // Which stage it serves
    MachineState state;          // Current operational state
    ProductionLine productionLine; // Which line it belongs to
    Integer healthScore;         // 0-100
    Integer failureCount;
    LocalDateTime lastPingAt;
    Long currentJobId;           // Currently assigned job (nullable)
}
```

### 7.2 Machine States

```
                    ┌──────────┐
              ┌────▶│   IDLE   │◀───── job complete
              │     └────┬─────┘
              │          │ job assigned
              │     ┌────▼─────┐
              │     │ RUNNING  │──────▶ COMPLETED
              │     └────┬─────┘
              │          │ failure detected
              │     ┌────▼─────┐
              │     │  DOWN    │──────▶ maintenance needed
              │     └────┬─────┘
              │          │ fixed
              │     ┌────▼─────┐
              └─────│ MAINTAIN │
                    └──────────┘
```

- **IDLE**: Available, no job running. Can accept new jobs.
- **RUNNING**: Actively processing a production job.
- **DOWN**: Machine has failed. Cannot accept jobs. Triggers incident.
- **MAINTENANCE**: Under scheduled or reactive repair.
- **OFFLINE**: Decommissioned or not yet provisioned.

### 7.3 Production Lines

The 500 machines are organized into **production lines**. A production line is a
logical grouping of machines that together can process a batch end-to-end.

```
Production Line = {
    id,
    name,                   // e.g. "Line-Alpha"
    machines: Machine[50],  // ~50 machines per line
    currentBatch: Batch     // The batch this line is currently processing
}
```

Each production line has at least one machine per stage capability. With 10 stages
and ~50 machines per line, we get **10 production lines** running concurrently.

### 7.4 Machine Allocation Algorithm

When a production job needs a machine:

1. Determine the required `MachineCapability` for the current stage.
2. Find all machines with that capability in the same production line.
3. Filter to machines in `IDLE` state.
4. Sort by: (a) health score descending, (b) failure count ascending.
5. Select the best candidate.
6. If no idle machine found in same line, search other lines (cross-line assignment).
7. If no machine available globally, the job enters a **waiting queue**.

### 7.5 Machine Capability Mapping

| Stage           | Capability Enum         |
|-----------------|-------------------------|
| 1. Receive      | `RECEIVING`             |
| 2. Wash & Sort  | `WASHING_SORTING`       |
| 3. Peel & Pulp  | `PEELING_PULPING`       |
| 4. Filter       | `FILTERING`             |
| 5. Blend        | `BLENDING`              |
| 6. Pasteurize   | `PASTEURIZING`          |
| 7. Quality Insp | `QUALITY_INSPECTION`    |
| 8. Fill & Cap   | `FILLING_CAPPING`       |
| 9. Label & Pack | `LABELING_PACKING`      |
| 10. Warehouse   | `WAREHOUSING`           |

---

## 8. Real-Time Event Flow

### 8.1 Architecture

```
SimulationEngine (ScheduledExecutor, 1 tick/sec)
    │
    ├──▶ MachineActor.update() ──▶ writes to DB
    │                                │
    │                                ▼
    │                          EventService.publish(ProductionEvent)
    │                                │
    │                                ▼
    │                          SseEmitter.send(event) ──▶ Browser EventSource
    │
    └──▶ JobScheduler.evaluate() ──▶ advances jobs
                                     │
                                     ▼
                               EventService.publish(StageTransition)
```

### 8.2 Event Types

| Event                  | Trigger                               | Payload                       |
|------------------------|---------------------------------------|-------------------------------|
| `MACHINE_STATE_CHANGED`| Machine state transitions             | machineId, oldState, newState |
| `MACHINE_TELEMETRY`    | Every tick (throttled to frontend)    | temperature, pressure, etc.   |
| `JOB_STARTED`          | A job begins on a machine             | jobId, machineId, stage       |
| `JOB_COMPLETED`        | A job finishes successfully           | jobId, batchId, stage, result |
| `JOB_FAILED`           | A job fails mid-execution             | jobId, error, severity        |
| `BATCH_STAGE_ADVANCED` | Batch moves to next stage             | batchId, fromStage, toStage   |
| `BATCH_COMPLETED`      | Batch reaches warehouse               | batchId, yield                |
| `QUALITY_CHECK_RESULT` | QC inspection result                  | checkId, batchId, result      |
| `INCIDENT_RAISED`      | Failure or anomaly detected           | incidentId, machineId, severity|
| `INCIDENT_RESOLVED`    | Incident handled                      | incidentId, resolvedBy        |
| `INVENTORY_LOW`        | Material below reorder level          | materialId, currentQty        |
| `ORDER_STATUS_CHANGED` | Production order status update        | orderId, oldStatus, newStatus |

### 8.3 SSE Connection Flow

```
1. Browser opens: GET /api/events/stream
2. Backend creates SseEmitter, stores in EventService
3. Simulation engine publishes events to EventService
4. EventService fans out to all connected emitters
5. Browser receives events via EventSource API
6. React components update via React Query cache invalidation
7. Browser auto-reconnects on connection loss
```

### 8.4 Event Throttling

Machine telemetry is generated every tick (1 second) but sent to the frontend
at most every 3 seconds to prevent overwhelming the browser. State changes and
job events are sent immediately.

---

## 9. Failure & Recovery Concept

### 9.1 Failure Detection

```
┌─────────────────────────────────────────────────────────┐
│                 FAILURE DETECTION                        │
│                                                         │
│  SimulationEngine (every tick)                          │
│    │                                                    │
│    ├──▶ Check machine healthScore                       │
│    │      if score < 20 ──▶ trigger FAILURE             │
│    │                                                    │
│    ├──▶ Check telemetry anomalies                       │
│    │      if temp > threshold ──▶ trigger WARNING       │
│    │      if pressure > threshold ──▶ trigger FAILURE   │
│    │                                                    │
│    ├──▶ Random failure injection (configurable rate)    │
│    │      if random < failureRate ──▶ trigger FAILURE   │
│    │                                                    │
│    └──▶ Job timeout                                     │
│           if job running > expected * 2 ──▶ trigger STUCK│
└─────────────────────────────────────────────────────────┘
```

### 9.2 Failure Response

```
Machine Failure Detected
    │
    ▼
1. Set machine state → DOWN
2. Create Incident record (severity, description)
3. Mark current ProductionJob → FAILED (if any)
4. Check if a batch was in progress
    │
    ├── Yes ──▶ Auto-reassign job to next available machine
    │           with same capability
    │
    │   ├── Machine found ──▶ Create new job on new machine
    │   │                     Resume batch from where it left off
    │   │
    │   └── No machine available ──▶ Queue the job
    │                                 Alert admin via SSE event
    │
    └── No ──▶ Just mark machine DOWN
    
5. Broadcast INCIDENT_RAISED via SSE
6. If worker assigned: notify worker for physical inspection
```

### 9.3 Maintenance Flow

```
Scheduled Maintenance (preventive):
    - Every N production cycles OR every M hours of operation
    - System sets machine → MAINTENANCE state
    - Worker performs inspection
    - Worker marks maintenance complete → machine → IDLE

Reactive Maintenance (after failure):
    - Incident created with severity
    - Worker assigned to diagnose
    - Worker fixes issue, updates maintenance record
    - Machine state → IDLE, healthScore restored
    - Any queued jobs are reassigned
```

### 9.4 Automatic Reassignment Rules

1. Only reassign if the batch still has a valid production plan.
2. Prefer same production line first (minimize transport simulation).
3. Never assign to a machine in DOWN or MAINTENANCE state.
4. Maximum 3 reassignment attempts per job before escalating to admin.
5. If a machine fails 5+ times in a row, mark it for mandatory maintenance.

---

## 10. Deployment Architecture

### 10.1 Local Development

```
┌─────────────────────────────────────────────┐
│           Docker Compose (Local)             │
│                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
│  │ Frontend │  │ Backend  │  │PostgreSQL│ │
│  │ Vite dev │  │ Spring   │  │ 16       │ │
│  │ :5173    │  │ Boot     │  │ :5432    │ │
│  │          │  │ :8080    │  │          │ │
│  └──────────┘  └──────────┘  └──────────┘ │
│                                             │
│  Frontend proxies /api to backend:8080      │
│  Backend connects to postgres:5432          │
└─────────────────────────────────────────────┘

Commands:
    docker-compose up -d          # Start all services
    docker-compose up --build     # Rebuild and start
    docker-compose down           # Stop all services
```

### 10.2 Production / Live Demo

```
┌─────────────────────────────────────────────────────────────┐
│                    PRODUCTION DEPLOYMENT                     │
│                                                             │
│  ┌─────────────────────┐    ┌────────────────────────────┐ │
│  │    GitHub Pages      │    │   Railway / Render / Fly.io│ │
│  │    (Frontend)        │    │   (Backend + Database)     │ │
│  │                     │    │                            │ │
│  │  React SPA built    │    │  Spring Boot JAR           │ │
│  │  via GitHub Actions │    │  + PostgreSQL              │ │
│  │                     │    │  + Flyway migrations       │ │
│  │  URL:               │    │                            │ │
│  │  user.github.io/    │    │  URL:                      │ │
│  │  mango_juice_factory│    │  smartfactory-api.railway  │ │
│  │                     │    │  .app (or similar)         │ │
│  └─────────┬───────────┘    └─────────────┬──────────────┘ │
│            │                               │                │
│            │         HTTPS / REST + SSE    │                │
│            └───────────────────────────────┘                │
│                                                             │
│  CORS configured on backend to allow GitHub Pages origin    │
└─────────────────────────────────────────────────────────────┘
```

### 10.3 CI/CD Pipeline (GitHub Actions)

```
Push to main
    │
    ├──▶ GitHub Action: Build & Test Backend
    │       ├── mvn clean test
    │       └── mvn package
    │
    ├──▶ GitHub Action: Build Frontend
    │       ├── npm ci
    │       ├── npm run build
    │       └── Deploy to GitHub Pages
    │
    └──▶ GitHub Action: Deploy Backend
            ├── docker build
            └── push to registry / deploy to Railway
```

### 10.4 Environment Configuration

| Variable                 | Local (dev)              | Production                |
|--------------------------|--------------------------|---------------------------|
| `SPRING_PROFILES_ACTIVE` | `dev`                    | `prod`                    |
| `DB_HOST`                | `localhost`              | Managed PostgreSQL host   |
| `DB_PORT`                | `5432`                   | `5432`                    |
| `DB_NAME`                | `smartfactory_dev`       | `smartfactory_prod`       |
| `DB_USERNAME`            | `postgres`               | From secrets              |
| `DB_PASSWORD`            | `postgres`               | From secrets              |
| `JWT_SECRET`             | `dev-secret`             | From secrets              |
| `CORS_ALLOWED_ORIGINS`   | `http://localhost:5173`  | `https://user.github.io`  |
| `SIMULATION_ENABLED`     | `true`                   | `true`                    |
| `SIMULATION_SPEED`       | `1` (real-time)          | `1`                       |

---

## 11. API Design Overview

### 11.1 REST Endpoints

| Method | Endpoint                          | Description                    |
|--------|-----------------------------------|--------------------------------|
| POST   | `/api/auth/login`                 | Login, get JWT                 |
| POST   | `/api/auth/register`              | Register worker account        |
|        |                                   |                                |
| GET    | `/api/dashboard/stats`            | Dashboard overview stats       |
| GET    | `/api/dashboard/timeline`         | Production timeline data       |
|        |                                   |                                |
| POST   | `/api/orders`                     | Create production order        |
| GET    | `/api/orders`                     | List all orders                |
| GET    | `/api/orders/{id}`                | Order detail                   |
| PATCH  | `/api/orders/{id}/status`         | Update order status            |
| POST   | `/api/orders/{id}/start`          | Start production               |
| POST   | `/api/orders/{id}/cancel`         | Cancel order                   |
|        |                                   |                                |
| GET    | `/api/plans/{orderId}`            | Get production plan            |
| GET    | `/api/plans/{orderId}/jobs`       | List jobs for plan             |
|        |                                   |                                |
| GET    | `/api/machines`                   | List all machines (paginated)  |
| GET    | `/api/machines/{id}`              | Machine detail                 |
| GET    | `/api/machines/{id}/telemetry`    | Machine telemetry history      |
| PATCH  | `/api/machines/{id}/maintenance`  | Trigger maintenance            |
| GET    | `/api/machines/lines`             | List production lines          |
|        |                                   |                                |
| GET    | `/api/batches`                    | List batches                   |
| GET    | `/api/batches/{id}`               | Batch detail + stage history   |
|        |                                   |                                |
| GET    | `/api/inventory`                  | List inventory items           |
| PATCH  | `/api/inventory/{id}`             | Update inventory               |
| GET    | `/api/inventory/alerts`           | Low stock alerts               |
|        |                                   |                                |
| GET    | `/api/quality`                    | List quality checks            |
| POST   | `/api/quality`                    | Create quality check           |
|        |                                   |                                |
| GET    | `/api/warehouse`                  | List warehouse slots           |
| GET    | `/api/warehouse/stock`            | Warehouse inventory            |
|        |                                   |                                |
| GET    | `/api/workers`                    | List workers                   |
| POST   | `/api/workers`                    | Create worker                  |
| PATCH  | `/api/workers/{id}/assign`        | Assign worker to incident      |
|        |                                   |                                |
| GET    | `/api/incidents`                  | List incidents                 |
| PATCH  | `/api/incidents/{id}/resolve`     | Resolve incident               |
|        |                                   |                                |
| GET    | `/api/audit`                      | Audit log (paginated)          |
|        |                                   |                                |
| GET    | `/api/events/stream`              | SSE endpoint (EventSource)     |

### 11.2 Key DTOs

```
CreateOrderRequest {
    mangoInputKg: Double
    productType: String
    bottleSizeMl: Integer
    targetQuantity: Integer
}

ProductionPlanResponse {
    orderId, totalBatches, batchList[], materialRequirements[], timeline

}

MachineStatusResponse {
    id, code, state, capability, healthScore, currentJobId, lineId, telemetry
}
```

---

## 12. Security Model

- **JWT-based authentication**: Admin and Workers login to get a token.
- **Role-Based Access Control (RBAC)**:
  - `ADMIN`: Full access. Creates orders, views everything, manages workers.
  - `WORKER`: Can view assigned tasks, update machine status, submit QC results, log maintenance.
  - `VIEWER`: Read-only dashboard access (future).
- **Admin is the primary controller**: Only Admin can create production orders, start/cancel production, and manage inventory.
- **Workers handle physical tasks**: Machine inspection, maintenance, quality checks, exception handling.
- CORS configured to allow frontend origin only.
- Passwords hashed with BCrypt.

---

## 13. Key Design Decisions Summary

| Decision                        | Choice               | Rationale                                    |
|---------------------------------|----------------------|----------------------------------------------|
| Real-time protocol              | SSE                  | Unidirectional, simpler, HTTP-compatible     |
| Simulation engine               | ScheduledExecutor    | Single-JVM, no external dependency needed    |
| Machine count                   | 500 (10 lines × 50) | Realistic factory scale, good demo           |
| DB ORM                          | Spring Data JPA      | Standard for Spring Boot, well-documented    |
| Schema migration                | Flyway               | Version-controlled, integrates with Spring   |
| Frontend state                  | React Query          | Declarative server state, auto-refetch       |
| Auth                            | JWT + Spring Security| Stateless, works with GitHub Pages frontend |
| Deployment (FE)                 | GitHub Pages         | Free, reliable, static hosting               |
| Deployment (BE)                 | Railway/Render       | Free tier available, Docker support          |
| Containerization                | Docker Compose       | One-command local dev, mirrors production    |

---

*This architecture document is the single source of truth for the SmartFactory system.*
*All implementation phases reference this document for design decisions.*
