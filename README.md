# Transaction Monitoring & Alerts - Version 1

A basic starter application with:
- Spring Boot REST backend
- In-memory H2 database
- React UI (served as static assets from Spring Boot)

This is intentionally simple and focused on core must-have features.

## Version 1 - 3 Phases

### Phase 1 - Core API and Data
- Store and list transactions
- Store/list alert records
- Seed baseline monitoring rules
- Persist data using JPA + H2

### Phase 2 - Rules and Alert Lifecycle
- Evaluate transactions against 4 basic rule types:
  - Amount Threshold
  - Velocity
  - New Payee
  - Daily Limit
- Create alerts with triggering transactions
- Support lifecycle transitions:
  - `OPEN -> ACKNOWLEDGED -> INVESTIGATING -> CLOSED`
  - `OPEN/ACKNOWLEDGED/INVESTIGATING -> DISMISSED`
- Capture alert history for audit trail

### Phase 3 - Basic UI (React + HTML/CSS/JS)
- Dashboard summary cards
- Transaction create form + transaction list/search
- Alerts list with filters and detail view
- Alert lifecycle actions (acknowledge/investigate/close/dismiss)
- Rule list with edit/save
- Sample data simulator button

## Run

```powershell
cd C:\Users\Administrator\Documents\firstDraft
.\mvnw.cmd spring-boot:run
```

Open:
- App: `http://localhost:8080`
- H2 Console: `http://localhost:8080/h2-console`

H2 JDBC URL:
- `jdbc:h2:mem:monitoringdb;DB_CLOSE_DELAY=-1`

## API Endpoints (V1)

- `POST /api/transactions`
- `GET /api/transactions`
- `GET /api/alerts`
- `GET /api/alerts/{id}`
- `GET /api/alerts/{id}/history`
- `PATCH /api/alerts/{id}/status`
- `GET /api/rules`
- `PUT /api/rules/{id}`
- `GET /api/dashboard/summary`
- `POST /api/simulator/generate`

## Example Transaction Payload

```json
{
  "reference": "TXN-1001",
  "accountId": "ACC-001",
  "payeeId": "PAYEE-NEW-001",
  "amount": 15000.00,
  "currency": "USD",
  "timestamp": "2026-08-01T10:00:00Z",
  "description": "High-value transfer"
}
```

## Notes

- No authentication in V1 (single-operator training setup).
- Rules are persisted and editable, but still simple for quick iteration.
- Next versions can add async processing, auth, and richer UI charts.

