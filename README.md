# 🌊 HYDRO-OS — Automated Smart Reservoir & Dam Management System

[![.NET 10](https://img.shields.io/badge/.NET-10.0-512BD4?logo=dotnet&logoColor=white)](https://dotnet.microsoft.com/)
[![C# 13](https://img.shields.io/badge/C%23-13.0-239120?logo=csharp&logoColor=white)](https://learn.microsoft.com/dotnet/csharp/)
[![Entity Framework Core](https://img.shields.io/badge/EF%20Core-9.0-512BD4)](https://learn.microsoft.com/ef/core/)
[![MySQL 8.0](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Tests](https://img.shields.io/badge/Tests-13%20Passed-brightgreen)](tests/DamControlSystem.Tests)

An intelligent, real-time hydrological decision support engine and operational dashboard for reservoir management, proactive flood mitigation, automated sluice gate scheduling, and emergency response dispatch.

---

## 🏗 Architecture Overview

The system has been completely ported from Java (Spring Boot) to high-performance modern **C# (.NET 10 / ASP.NET Core Web API)**.

```
Dam-Control-System/
├── DamControlSystem.sln             # Visual Studio / .NET Solution
├── DamControlSystem.slnx            # Modern .NET 10 Solution format
├── Dockerfile                       # Multi-stage production container build
├── docker-compose.yml               # Multi-container orchestration (API + MySQL 8)
├── src/
│   └── DamControlSystem/            # ASP.NET Core Web API Project
│       ├── BackgroundServices/      # WaterFlowBackgroundService (hourly assessment)
│       ├── Controllers/             # DamController (REST API endpoints)
│       ├── Data/                    # SmartDamDbContext & Repositories
│       ├── DTOs/                    # Request and response models
│       ├── Models/                  # DamMetadata, ReservoirState, ControlLog, EmergencyAlert
│       ├── Services/                # DamControlEngineService, AiSuggestionService, WeatherForecastService
│       ├── wwwroot/                 # HYDRO-OS v4.2 static frontend dashboard
│       └── appsettings.json         # Configuration & connection strings
└── tests/
    └── DamControlSystem.Tests/      # xUnit automated tests (Hydrological engine, AI risk, metadata)
```

---

## ⚡ Key Features

- **Hydrological Inflow Prediction**:
  Calculates projected inflows using catchment basin area, runoff coefficient, and live 3-day meteorological rainfall data from the Open-Meteo API.
- **Mass Balance & Proactive Discharge Engine**:
  Maintains reservoir volume below the 85% safety threshold. Computes necessary 24-hour release rates and throttles discharge to the downstream safe channel capacity ($Q_{safe}$).
- **Automated Emergency & Siren Alerting**:
  Triggers immediate alert escalation when inflow projections exceed downstream channel thresholds, alerting affected villages.
- **AI Recommendation Engine**:
  Synthesizes reservoir telemetry into classified risk advisories (`CRITICAL`, `WARNING`, `NOMINAL`), confidence metrics, and gate configurations.
- **Multi-Dam Support**:
  Built-in support for multiple reservoirs across Maharashtra:
  - **Erai Dam** (Chandrapur)
  - **Khadakwasla Dam** (Pune)
  - **Panshet Dam** (Pune)
  - **Mulshi Dam** (Pune)
- **Background Scheduler Worker**:
  `WaterFlowBackgroundService` evaluates telemetry hourly, updates gate percentages, and logs decisions.
- **Two-Tier Command Panel (Operator & Authority)**:
  Operators review automated telemetry drafts and dispatch priority alerts. Government authority admins approve or manually override sluice discharge rates.

---

## 🚀 Quick Start (Local Run)

### Prerequisites
- [.NET 10 SDK](https://dotnet.microsoft.com/download) (or .NET 8 / 9 runtime)
- *(Optional)* Local MySQL 8 database or Docker (the app automatically falls back to SQLite `smart_dam.db` if MySQL is not detected).

### 1. Clone & Build
```bash
dotnet build DamControlSystem.sln
```

### 2. Run Automated Tests
```bash
dotnet test DamControlSystem.sln
```

### 3. Launch Application
```bash
dotnet run --project src/DamControlSystem
```

Open your browser at **[http://localhost:8080](http://localhost:8080)** to access the HYDRO-OS control dashboard!

---

## 🐳 Docker Deployment

To launch the full stack (ASP.NET Core Web API + MySQL 8 database):

```bash
docker compose up --build -d
```

- Web Dashboard & API: `http://localhost:8080`
- MySQL Host Port: `3307` (Mapped to internal `3306`)

---

## 📡 REST API Reference

All endpoints are rooted under `/api/v1/dam`:

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/dam/status` | Returns latest reservoir state and decision log (Default: Erai). |
| `GET` | `/api/v1/dam/{damId}/status` | Returns current status and telemetry for specified dam. |
| `POST` | `/api/v1/dam/{damId}/update-state` | Updates reservoir volume/level, fetches rain forecast, and executes control logic. |
| `GET` | `/api/v1/dam/{damId}/forecast-eval` | Runs on-demand hypothetical simulation without persisting to DB. |
| `GET` | `/api/v1/dam/{damId}/ai-recommendation` | Generates AI risk synthesis, confidence score, and gate schedules. |
| `POST` | `/api/v1/dam/{damId}/submit-decision` | Submits draft decision log for higher authority review. |
| `POST` | `/api/v1/dam/{damId}/authority-action` | Authority approval (`action=APPROVE`) or manual discharge override (`action=REJECT`). |
| `POST` | `/api/v1/dam/{damId}/emergency-alert` | Dispatches emergency priority alert from shift officer. |
| `GET` | `/api/v1/dam/alerts` | Lists all active/unresolved emergency alerts. |
| `POST` | `/api/v1/dam/alert/{id}/resolve` | Resolves an emergency alert by ID. |

---

## 🧪 Automated Testing

Tests are implemented with **xUnit** and **EF Core In-Memory**:
- `HydrologicalEngineTests`: Runoff projection formulas, proactive releases, flood alert trigger conditions, and non-mutating simulation checks.
- `AiSuggestionServiceTests`: Risk level classification (`CRITICAL`, `WARNING`, `NOMINAL`) and gate schedule generation.
- `DamMetadataTests`: Registry validation for Erai, Khadakwasla, Panshet, Mulshi.

Execute tests via:
```bash
dotnet test
```
