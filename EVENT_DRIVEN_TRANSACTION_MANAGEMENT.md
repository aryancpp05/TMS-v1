# Event-Driven Transaction Management (Design Note)

## Why this document
This note explains event-driven transaction management for the current V1 project and gives a phased path you can implement later without rewriting everything.

## Current state in V1
Today, transaction processing is synchronous:

1. `POST /api/transactions` calls `TransactionService.create`.
2. Transaction is saved.
3. `RuleEngineService.evaluate` runs in the same request.
4. Alerts are created before the API response returns.

This is simple, but tightly couples transaction write latency to rule evaluation latency.

## What event-driven means here
In an event-driven design, creating a transaction and evaluating rules are decoupled:

1. API persists transaction quickly.
2. System emits `TransactionCreated` event.
3. Rule processor consumes event asynchronously.
4. If needed, processor emits `AlertCreated` or other downstream events.

Benefits:
- Faster API response for transaction creation.
- Better scalability for rule processing.
- Cleaner separation of concerns.
- Easier to add new listeners (notifications, reporting, ML scoring).

Tradeoffs:
- Eventual consistency (alerts appear shortly after transaction, not always instantly).
- Extra operational complexity (retries, dead-letter handling, idempotency).

## Suggested target transaction lifecycle
Add a transaction status model (example):

- `COMPLETED` (normal posted transaction)
- `ROLLBACK_REQUESTED`
- `ROLLBACK_APPROVED`
- `ROLLBACK_REJECTED`
- `REFUNDED`

Notes:
- `REFUNDED` is final for rollback flow.
- Keep original transaction immutable where possible; model rollback/refund as related records.

## Rollback workflow (operator approval required)
Suggested business flow:

1. User submits rollback request with reason and evidence.
2. System validates eligibility (exists, not already refunded, within allowed window, etc.).
3. Transaction enters `ROLLBACK_REQUESTED`.
4. Operator reviews and approves/rejects.
5. If approved:
   - Transaction enters `ROLLBACK_APPROVED`.
   - Refund transaction is created (or posted to payment/refund service).
   - Original transaction transitions to `REFUNDED`.
6. If rejected:
   - Transaction enters `ROLLBACK_REJECTED`.

## API shape proposal

### Rollback request
`POST /api/transactions/{id}/rollback-requests`

Request body:
```json
{
  "reasonCode": "DUPLICATE",
  "reasonDetail": "Duplicate transfer submitted in error",
  "requestedBy": "customer-123",
  "requestedAt": "2026-08-02T10:30:00Z",
  "supportingReference": "CASE-7788"
}
```

### Operator decision
`POST /api/rollback-requests/{requestId}/decision`

Request body:
```json
{
  "decision": "APPROVE",
  "operatorId": "op-001",
  "note": "Validated duplicate transaction"
}
```

### Read models
- `GET /api/rollback-requests`
- `GET /api/transactions/{id}` with rollback/refund metadata

## Events and contracts
Start with small event contracts:

### `TransactionCreated`
```json
{
  "eventId": "uuid",
  "eventType": "TransactionCreated",
  "occurredAt": "2026-08-02T10:31:12Z",
  "transactionId": 123,
  "reference": "TXN-1001",
  "accountId": "ACC-001",
  "payeeId": "PAYEE-NEW-001",
  "amount": 15000.00,
  "currency": "USD",
  "timestamp": "2026-08-02T10:30:58Z"
}
```

### `RollbackRequested`
```json
{
  "eventId": "uuid",
  "eventType": "RollbackRequested",
  "occurredAt": "2026-08-02T10:40:00Z",
  "rollbackRequestId": 456,
  "transactionId": 123,
  "reasonCode": "DUPLICATE"
}
```

### `RollbackApproved`
### `RollbackRejected`
### `RefundCompleted`
Use these for audit, notifications, and downstream reconciliation.

## Reliability patterns you should adopt

1. Idempotency
   - Every consumer should handle duplicate event delivery.
   - Persist processed `eventId` values or enforce unique business keys.

2. Outbox pattern (recommended)
   - Write domain state + outbox event in one DB transaction.
   - Separate publisher sends outbox rows to broker.
   - Avoids lost events between DB commit and publish.

3. Retry and dead-letter queue
   - Retries for transient failures.
   - Dead-letter queue for poison messages and manual replay.

4. Correlation IDs
   - Carry `correlationId` across API requests and events.
   - Makes tracing and debugging easier.

## Phased implementation path for this repo

### Phase A (low complexity): In-process async events
- Keep same DB.
- Publish Spring application events after transaction save.
- Consume with `@TransactionalEventListener` + `@Async` for rule engine.
- Good first step to decouple controller latency.

### Phase B: Durable outbox
- Add `outbox_events` table.
- Store event rows in same transaction as transaction/rollback writes.
- Add scheduled publisher job.

### Phase C: External broker
- Introduce Kafka or RabbitMQ.
- Publisher sends outbox events to topic/queue.
- Consumers process alerts, rollback decisions, notifications.

## Data model extensions (minimum)

1. Extend `transactions` table/entity with status and optional parent/refund links.
2. New `rollback_requests` table/entity:
   - `id`, `transaction_id`, `status`, `reason_code`, `reason_detail`,
   - `requested_by`, `requested_at`, `operator_id`, `operator_note`, `decided_at`.
3. Optional `transaction_history` / generic audit table.
4. Optional `outbox_events` table for reliable event publishing.

## Security and validation notes
Even without full authentication in V1:
- Enforce operator-only rollback decision endpoint.
- Validate rollback constraints (time window, one active rollback per transaction, amount/currency consistency).
- Prevent direct transition to `REFUNDED` without approval event.

## Testing strategy

1. Unit tests for rollback state transitions.
2. Integration tests for approval -> refund creation path.
3. Duplicate event tests (idempotency).
4. Failure tests for partial failures and retries.
5. Performance tests comparing sync vs async rule processing latency.

## Decision checklist
Before implementing, decide:

- Expected alert latency (`real-time` vs `near-real-time`).
- Broker choice (`Kafka`, `RabbitMQ`, or in-process first).
- Rollback SLA and approval rules.
- Whether refunds are internal transaction records or external payment calls.

## Recommended first step in this project
Implement Phase A first (in-process async events) plus rollback workflow with approval/refund status transitions. This gives immediate architecture and business-value improvements without requiring broker infrastructure yet.

