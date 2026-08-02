# Risk Management Progress Tracker

## Project Context Snapshot
- Module focus: Rules Management
- Current feature in progress: Feature 1 - Monitoring Rule Entity
- Integration note: Avoid changes to Transaction Management and Alert Management for this step.

## Change Log

### 2026-08-02 - Feature 1 Implemented
**Scope:** Monitoring Rule Entity only

**Files changed:**
- `src/main/java/com/example/firstDraft/entity/MonitoringRule.java`

**What was added/updated:**
- Reused existing enums:
  - `RuleType` (`AMOUNT_THRESHOLD`, `VELOCITY`, `NEW_PAYEE`, `DAILY_LIMIT`)
  - `AlertSeverity` (`HIGH`, `MEDIUM`, `LOW`)
- Added creation timestamp field:
  - `createdAt` (`Instant`)
  - JPA mapping: `@Column(nullable = false, updatable = false)`
- Added automatic timestamp initialization:
  - `@PrePersist` method `onCreate()` to set `createdAt` during insert
- Kept existing fields for rule parameters:
  - `amountThreshold`
  - `transactionCountThreshold`
  - `timeWindowMinutes`
- Kept package structure and table name unchanged (`monitoring_rules`)

**Why:**
- Meets Feature 1 requirement to store all core monitoring-rule attributes plus creation timestamp.
- Keeps compatibility with current architecture and existing Transaction Management data model.
- No CRUD/API/rule engine logic introduced in this step.

### 2026-08-02 - Feature 2 Implemented
**Scope:** Create Monitoring Rule API only

**Files changed:**
- `src/main/java/com/example/firstDraft/controller/RuleController.java`
- `src/main/java/com/example/firstDraft/service/RuleService.java`
- `src/main/java/com/example/firstDraft/repository/MonitoringRuleRepository.java`
- `src/main/java/com/example/firstDraft/dto/RuleCreateRequest.java`
- `src/main/java/com/example/firstDraft/exception/ConflictException.java`
- `src/main/java/com/example/firstDraft/exception/GlobalExceptionHandler.java`

**What was added/updated:**
- Added `POST /api/rules` endpoint returning `201 Created`
- Added `RuleCreateRequest` DTO with bean validation annotations
- Added rule creation service logic with:
  - type-dependent required-parameter checks
  - positive threshold checks
  - duplicate-name check (case-insensitive)
- Added repository support for duplicate check: `existsByNameIgnoreCase`
- Added `ConflictException` mapped to HTTP `409 Conflict`

**Why:**
- Implements Feature 2 while preserving existing architecture (controller/service/repository/dto/exception patterns).
- Keeps integration-safe behavior with Transaction and Alert modules by changing only Rules API create flow.

### 2026-08-02 - Feature 3 Implemented
**Scope:** View Monitoring Rules API with optional filters

**Files changed:**
- `src/main/java/com/example/firstDraft/controller/RuleController.java`
- `src/main/java/com/example/firstDraft/service/RuleService.java`
- `src/main/java/com/example/firstDraft/repository/MonitoringRuleRepository.java`
- `src/main/java/com/example/firstDraft/exception/GlobalExceptionHandler.java`
- `src/test/java/com/example/firstDraft/FirstDraftApplicationTests.java`

**What was added/updated:**
- Enhanced `GET /api/rules` to accept optional query filters:
  - `active`
  - `type`
  - `severity`
- Added repository-level filtered query with nullable parameters.
- Added service overload `getRules(active, type, severity)` and preserved existing no-arg variant.
- Added explicit query-parameter type mismatch handling for invalid filter values with `400 Bad Request`.
- Added tests for active filtering and type+severity filtering.

**Why:**
- Implements Feature 3 using existing controller→service→repository flow.
- Keeps behavior backward-compatible (no filters still returns all rules).
- Avoids changes in Transaction and Alert modules.

### 2026-08-02 - Feature 4 Implemented
**Scope:** Update Monitoring Rule API only

**Files changed:**
- `src/main/java/com/example/firstDraft/dto/RuleUpdateRequest.java`
- `src/main/java/com/example/firstDraft/repository/MonitoringRuleRepository.java`
- `src/main/java/com/example/firstDraft/service/RuleService.java`
- `src/test/java/com/example/firstDraft/FirstDraftApplicationTests.java`

**What was added/updated:**
- Enhanced update DTO to allow updating:
  - `name`
  - `type`
  - `severity`
  - `active`
  - rule thresholds/time-window fields
- Added duplicate-name check for updates excluding current rule id.
- Added type-dependent validation for update requests:
  - `AMOUNT_THRESHOLD` / `DAILY_LIMIT` require positive `amountThreshold`
  - `VELOCITY` requires positive `transactionCountThreshold` and `timeWindowMinutes`
- Updated service to normalize fields per selected type and clear non-applicable values.
- Added tests for successful update, validation failure, duplicate-name conflict, and not-found update case.

**Why:**
- Implements Feature 4 in existing controller→service→repository design without touching Transaction or Alert modules.
- Preserves unique rule-name behavior and consistent error handling patterns.

### 2026-08-02 - Feature 5 Implemented
**Scope:** Enable/Disable Monitoring Rule only

**Files changed:**
- `src/main/java/com/example/firstDraft/dto/RuleStatusUpdateRequest.java`
- `src/main/java/com/example/firstDraft/controller/RuleController.java`
- `src/main/java/com/example/firstDraft/service/RuleService.java`
- `src/test/java/com/example/firstDraft/FirstDraftApplicationTests.java`

**What was added/updated:**
- Added dedicated status update DTO with only one field:
  - `active` (`@NotNull Boolean`)
- Added endpoint:
  - `PATCH /api/rules/{id}/status`
- Added service method to update only active/inactive status by rule id.
- Added tests for:
  - successful status change
  - ensuring other rule fields remain unchanged
  - not-found rule handling

**Why:**
- Implements focused activate/deactivate behavior without changing rule configuration fields.
- Reuses existing NotFound/validation/error handling patterns in the current architecture.

### 2026-08-02 - Feature 6 Implemented
**Scope:** Delete Monitoring Rule (Soft Delete) only

**Files changed:**
- `src/main/java/com/example/firstDraft/controller/RuleController.java`
- `src/main/java/com/example/firstDraft/service/RuleService.java`
- `src/test/java/com/example/firstDraft/FirstDraftApplicationTests.java`

**What was added/updated:**
- Added endpoint:
  - `DELETE /api/rules/{id}`
- Added service method:
  - `softDeleteRule(id)`
- Soft delete behavior uses existing rule design by setting `active = false`.
- Added tests for:
  - successful soft delete
  - deleted rule excluded from active filters
  - not-found delete handling

**Why:**
- Uses current entity design (`active` flag) without introducing unnecessary schema changes.
- Ensures deleted rules are no longer treated as active while retaining rule record for history-safe workflows.

### 2026-08-02 - Feature 7 Implemented
**Scope:** Rule Validation Service only

**Files changed:**
- `src/main/java/com/example/firstDraft/service/RuleValidationService.java`
- `src/main/java/com/example/firstDraft/service/RuleService.java`
- `src/test/java/com/example/firstDraft/FirstDraftApplicationTests.java`

**What was added/updated:**
- Added reusable validation component `RuleValidationService` with:
  - common-field validation (`name`, `type`, `severity`)
  - type-specific validation (`AMOUNT_THRESHOLD`, `VELOCITY`, `NEW_PAYEE`, `DAILY_LIMIT`)
- Integrated validation into:
  - `createRule(...)`
  - `updateRule(...)`
- Removed duplicated inline validation methods from `RuleService`.
- Added tests for shared validation behavior (blank name, missing type on update).

**Why:**
- Centralizes rule configuration validation in one place and prevents duplication.
- Keeps existing exception handling (`BadRequestException`) and response format unchanged.

### 2026-08-02 - Feature 8 Implemented
**Scope:** Rule Engine for Monitoring Rules

**Files changed:**
- `src/main/java/com/example/firstDraft/service/RuleEngineService.java`
- `src/main/java/com/example/firstDraft/service/ruleengine/RuleEvaluator.java`
- `src/main/java/com/example/firstDraft/service/ruleengine/RuleEvaluationResult.java`
- `src/main/java/com/example/firstDraft/service/ruleengine/AmountThresholdRuleEvaluator.java`
- `src/main/java/com/example/firstDraft/service/ruleengine/VelocityRuleEvaluator.java`
- `src/main/java/com/example/firstDraft/service/ruleengine/NewPayeeRuleEvaluator.java`
- `src/main/java/com/example/firstDraft/service/ruleengine/DailyLimitRuleEvaluator.java`

**What was added/updated:**
- Refactored the rule engine into an extensible strategy design:
  - `RuleEngineService` orchestrates rule retrieval and evaluator dispatch
  - one evaluator per rule type (`AMOUNT_THRESHOLD`, `VELOCITY`, `NEW_PAYEE`, `DAILY_LIMIT`)
- Active rules are loaded and evaluated against incoming transaction.
- Triggered rules produce `RuleEvaluationResult` with reason + triggering transactions.
- Alert creation remains integrated with existing Alert and AlertHistory entities/repositories.
- `TransactionService.create(...)` integration remains unchanged (`ruleEngineService.evaluate(saved)`).

**Why:**
- Keeps rule evaluation logic cleanly separated and easier to extend.
- Reuses current alert-management data model and avoids changes to transaction storage logic.

### 2026-08-02 - Feature 9 Implemented
**Scope:** Rule Execution History

**Files changed:**
- `src/main/java/com/example/firstDraft/model/RuleExecutionOutcome.java`
- `src/main/java/com/example/firstDraft/entity/RuleExecutionHistory.java`
- `src/main/java/com/example/firstDraft/repository/RuleExecutionHistoryRepository.java`
- `src/main/java/com/example/firstDraft/service/RuleEngineService.java`
- `src/test/java/com/example/firstDraft/FirstDraftApplicationTests.java`

**What was added/updated:**
- Added rule execution history model to capture:
  - execution id
  - rule id
  - transaction id
  - evaluation outcome (`TRIGGERED` / `NOT_TRIGGERED`)
  - reason/result message
  - execution timestamp
- Integrated history persistence into rule engine evaluation loop.
- Added test validating history rows are created with mixed outcomes and shared execution id per evaluation run.

**Why:**
- Provides audit/debug visibility for every rule evaluation without changing transaction storage logic.
- Keeps implementation fully within existing entity→repository→service architecture.

### 2026-08-02 - Feature 10 Implemented
**Scope:** Rule Audit History

**Files changed:**
- `src/main/java/com/example/firstDraft/model/RuleAuditAction.java`
- `src/main/java/com/example/firstDraft/entity/RuleAuditHistory.java`
- `src/main/java/com/example/firstDraft/repository/RuleAuditHistoryRepository.java`
- `src/main/java/com/example/firstDraft/dto/RuleAuditHistoryResponse.java`
- `src/main/java/com/example/firstDraft/service/ApiMapper.java`
- `src/main/java/com/example/firstDraft/service/RuleService.java`
- `src/main/java/com/example/firstDraft/controller/RuleController.java`
- `src/test/java/com/example/firstDraft/FirstDraftApplicationTests.java`

**What was added/updated:**
- Added rule audit model with fields for:
  - audit id
  - rule id
  - action (`CREATED`, `UPDATED`, `ACTIVATED`, `DEACTIVATED`, `DELETED`)
  - previous/new values
  - timestamp
  - changedBy
- Integrated automatic audit recording on rule create/update/status-change/delete flows.
- Added history retrieval endpoint:
  - `GET /api/rules/{id}/history`
- Added tests validating tracked actions and not-found behavior for missing rule history.

**Why:**
- Provides rule-change traceability for audit/debug without altering existing transaction or alert lifecycle behavior.

### 2026-08-02 - Feature 11 Implemented
**Scope:** Rule Statistics API

**Files changed:**
- `src/main/java/com/example/firstDraft/dto/RuleStatsResponse.java`
- `src/main/java/com/example/firstDraft/repository/MonitoringRuleRepository.java`
- `src/main/java/com/example/firstDraft/service/RuleService.java`
- `src/main/java/com/example/firstDraft/controller/RuleController.java`
- `src/test/java/com/example/firstDraft/FirstDraftApplicationTests.java`

**What was added/updated:**
- Added stats response DTO with:
  - total rules
  - active rules
  - inactive rules
  - grouped counts by rule type
  - grouped counts by severity
- Added repository aggregate methods using count/group-by queries.
- Added service aggregation method `getRuleStats()`.
- Added endpoint `GET /api/rules/stats`.
- Added integration test validating totals and grouped counts update correctly.

**Why:**
- Provides efficient summary insights for rules without loading full datasets in memory.

### 2026-08-02 - Feature 12 Implemented
**Scope:** Monitoring Rule Type Implementations

**Files changed:**
- `src/main/java/com/example/firstDraft/service/ruleengine/RuleEvaluator.java`
- `src/main/java/com/example/firstDraft/service/ruleengine/RuleEvaluationResult.java`
- `src/main/java/com/example/firstDraft/service/ruleengine/AmountThresholdRuleEvaluator.java`
- `src/main/java/com/example/firstDraft/service/ruleengine/VelocityRuleEvaluator.java`
- `src/main/java/com/example/firstDraft/service/ruleengine/NewPayeeRuleEvaluator.java`
- `src/main/java/com/example/firstDraft/service/ruleengine/DailyLimitRuleEvaluator.java`
- `src/main/java/com/example/firstDraft/service/RuleEngineService.java`

**What was added/updated:**
- Implemented rule-specific evaluators for:
  - `AMOUNT_THRESHOLD`
  - `VELOCITY`
  - `NEW_PAYEE`
  - `DAILY_LIMIT`
- `RuleEngineService` orchestrates active-rule retrieval, evaluator dispatch, and trigger handling.
- Triggered evaluations return reason + triggering transactions via `RuleEvaluationResult`.
- Existing alert creation integration remains unchanged.

**Why:**
- Keeps evaluation logic extensible and separated by rule type for future additions.

### 2026-08-02 - Feature 13 Implemented
**Scope:** Rule Engine Integration Tests

**Files changed:**
- `src/test/java/com/example/firstDraft/FirstDraftApplicationTests.java`

**What was added/updated:**
- Added integration tests for:
  - amount threshold trigger
  - amount threshold negative case
  - velocity trigger
  - new payee trigger
  - daily limit trigger
  - inactive rule not evaluated
- Tests validate end-to-end flow:
  - Transaction -> Rule Engine -> Rule Evaluation -> Alert creation

**Why:**
- Confirms monitoring-rule implementations work in real transaction processing flow.

### 2026-08-02 - Feature 14 Implemented
**Scope:** Exception Handling Improvements (Rules Management)

**Files changed:**
- `src/main/java/com/example/firstDraft/exception/GlobalExceptionHandler.java`
- `src/main/java/com/example/firstDraft/service/RuleService.java`
- `src/test/java/com/example/firstDraft/FirstDraftApplicationTests.java`

**What was added/updated:**
- Improved not-found messages to consistent format:
  - `Rule with id {id} not found`
- Added invalid status-operation handling:
  - returns `400 Bad Request` when attempting to set rule status to current value.
- Added malformed JSON/body handling:
  - `HttpMessageNotReadableException` mapped to `400 Bad Request`.
- Added tests for:
  - clear not-found message
  - invalid status operation

**Why:**
- Ensures consistent and clear rules-related error responses without changing successful behavior.

### 2026-08-02 - Feature 15 Implemented
**Scope:** Swagger/OpenAPI for Rules Management APIs

**Files changed:**
- `pom.xml`
- `src/main/java/com/example/firstDraft/config/OpenApiConfig.java`
- `src/main/java/com/example/firstDraft/controller/RuleController.java`
- `src/main/java/com/example/firstDraft/dto/RuleCreateRequest.java`
- `src/main/java/com/example/firstDraft/dto/RuleUpdateRequest.java`
- `src/main/java/com/example/firstDraft/dto/RuleStatusUpdateRequest.java`
- `src/main/java/com/example/firstDraft/dto/RuleStatsResponse.java`

**What was added/updated:**
- Added `springdoc-openapi-starter-webmvc-ui` dependency.
- Added OpenAPI metadata configuration (`OpenApiConfig`).
- Documented all Rules endpoints with summaries/descriptions/responses:
  - Create Rule
  - View Rules
  - Update Rule
  - Enable/Disable Rule
  - Delete Rule
  - View Rule History
  - View Rule Statistics
- Added schema-level documentation for key request/response DTO fields and validation expectations.

**Why:**
- Provides discoverable, testable API documentation and improves integration clarity for frontend/testing.

### 2026-08-02 - Feature 16 Implemented
**Scope:** Rules Management Frontend UI (V1)

**Files changed:**
- `src/main/resources/static/app.js`
- `src/main/resources/static/styles.css`

**What was added/updated:**
- Built Rules Management V1 section with:
  - clean rules list table
  - create/edit rule form
  - enable/disable action buttons
  - loading state
  - empty state
  - rules-specific API error state
- Added dynamic rule configuration form behavior:
  - `AMOUNT_THRESHOLD` and `DAILY_LIMIT`: amount field only
  - `VELOCITY`: transaction count + time window fields only
  - `NEW_PAYEE`: no extra config fields
- Integrated existing backend APIs:
  - `GET /api/rules`
  - `POST /api/rules`
  - `PUT /api/rules/{id}`
  - `PATCH /api/rules/{id}/status`

**Why:**
- Delivers a simple, professional Rules UI without introducing a separate frontend structure.

## Next Update Template
Use this section for next features as we proceed:
- Date:
- Feature:
- Files changed:
- Summary:
- Integration impact:

