# SmartFactory — Mango Juice Manufacturing Control System

A browser-based manufacturing execution system (MES) that simulates a complete
mango juice production facility with **500 machines** across **10 production lines**.

An Admin creates a production order (e.g., "5,000 kg mangoes → 500 mL mango beverage"),
and the system automatically coordinates production through **10 manufacturing stages** —
from raw mango receiving to warehouse storage — with real-time monitoring.

## Tech Stack

| Layer    | Technology               |
|----------|--------------------------|
| Frontend | React 18, Vite, Tailwind |
| Backend  | Java 17, Spring Boot 3   |
| Database | PostgreSQL 16            |
| Real-time| SSE (Server-Sent Events) |
| Auth     | JWT + Spring Security    |
| Build    | Maven (BE), npm (FE)     |
| Deploy   | Docker, GitHub Pages     |

## Manufacturing Stages

```
Receive → Wash/Sort → Peel/Pulp → Filter → Blend →
Pasteurize → Quality Inspect → Fill/Cap → Label/Pack → Warehouse
```

## Quick Start

```bash
# Clone and start with Docker
git clone https://github.com/USER/mango_juice_factory.git
cd mango_juice_factory
docker-compose up -d

# Frontend: http://localhost:5173
# Backend:  http://localhost:8080
# Database: localhost:5432
```

## Documentation

- [Architecture](ARCHITECTURE.md) — Full system design, entity relationships, API overview
- [Project Plan](PROJECT_PLAN.md) — 12-phase implementation roadmap

## Project Status

| Phase | Description                        | Status   |
|-------|------------------------------------|----------|
| 1     | Architecture + DB Foundation       | In Progress |
| 2     | Backend Foundation                 | Pending  |
| 3     | Entities + Inventory + Recipes     | Pending  |
| 4     | Authentication + RBAC              | Pending  |
| 5     | Production Planning + Jobs         | Pending  |
| 6     | 500-Machine Simulation Engine      | Pending  |
| 7     | Real-Time Execution                | Pending  |
| 8     | React Admin Control Center         | Pending  |
| 9     | Quality Control + Traceability     | Pending  |
| 10    | Failure + Maintenance + Recovery   | Pending  |
| 11    | Analytics + Warehouse + Polish     | Pending  |
| 12    | Testing + Docker + Deployment      | Pending  |

## License

MIT
