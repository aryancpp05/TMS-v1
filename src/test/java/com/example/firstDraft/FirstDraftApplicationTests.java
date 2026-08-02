package com.example.firstDraft;

import com.example.firstDraft.entity.Alert;
import com.example.firstDraft.entity.RuleExecutionHistory;
import com.example.firstDraft.exception.BadRequestException;
import com.example.firstDraft.exception.ConflictException;
import com.example.firstDraft.exception.NotFoundException;
import com.example.firstDraft.model.AlertStatus;
import com.example.firstDraft.model.AlertSeverity;
import com.example.firstDraft.model.RuleAuditAction;
import com.example.firstDraft.model.RuleExecutionOutcome;
import com.example.firstDraft.model.RuleType;
import com.example.firstDraft.model.TransactionStatus;
import com.example.firstDraft.service.RuleService;
import com.example.firstDraft.repository.AlertRepository;
import com.example.firstDraft.repository.RuleExecutionHistoryRepository;
import com.example.firstDraft.service.AlertService;
import com.example.firstDraft.service.TransactionService;
import com.example.firstDraft.dto.RuleCreateRequest;
import com.example.firstDraft.dto.RuleAuditHistoryResponse;
import com.example.firstDraft.dto.RuleResponse;
import com.example.firstDraft.dto.RuleStatsResponse;
import com.example.firstDraft.dto.RuleStatusUpdateRequest;
import com.example.firstDraft.dto.RuleUpdateRequest;
import com.example.firstDraft.dto.TransactionRequest;
import com.example.firstDraft.dto.AlertStatusUpdateRequest;
import com.example.firstDraft.dto.TransactionDecisionRequest;
import com.example.firstDraft.dto.TransactionRollbackDecisionRequest;
import com.example.firstDraft.dto.TransactionRollbackRequest;
import com.example.firstDraft.dto.TransactionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class FirstDraftApplicationTests {

	@Autowired
	private TransactionService transactionService;

	@Autowired
	private AlertService alertService;

	@Autowired
	private AlertRepository alertRepository;

	@Autowired
	private RuleService ruleService;

	@Autowired
	private RuleExecutionHistoryRepository ruleExecutionHistoryRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void createsAlertForHighAmountTransaction() throws Exception {
		TransactionRequest request = new TransactionRequest(
			"TEST-TXN-1",
			"ACC-001",
			"PAYEE-999",
			new BigDecimal("15000.00"),
			"USD",
			Instant.now(),
			"Integration test high amount"
		);

		TransactionResponse response = transactionService.create(request);

		List<Alert> alerts = alertRepository.findAll();
		assertThat(alerts).isNotEmpty();
		assertThat(alerts.stream().anyMatch(a -> a.getRuleName().equals("High Amount Threshold"))).isTrue();
		assertThat(response.status()).isEqualTo(TransactionStatus.PENDING_APPROVAL);
	}

	@Test
	void approvesPendingTransactionAfterRuleViolation() {
		TransactionResponse created = transactionService.create(new TransactionRequest(
			"TEST-TXN-APPROVAL-1",
			"ACC-003",
			"PAYEE-NEW-XYZ",
			new BigDecimal("12000.00"),
			"USD",
			Instant.now(),
			"Needs operator approval"
		));

		assertThat(created.status()).isEqualTo(TransactionStatus.PENDING_APPROVAL);

		TransactionResponse approved = transactionService.approve(
			created.id(),
			new TransactionDecisionRequest("op-001", "Approved by test")
		);

		assertThat(approved.status()).isEqualTo(TransactionStatus.COMPLETED);
		assertThat(approved.reviewedBy()).isEqualTo("op-001");
	}

	@Test
	void rejectsPendingTransactionAfterRuleViolation() {
		TransactionResponse created = transactionService.create(new TransactionRequest(
			"TEST-TXN-APPROVAL-2",
			"ACC-004",
			"PAYEE-NEW-ABC",
			new BigDecimal("13000.00"),
			"USD",
			Instant.now(),
			"Will be rejected"
		));

		TransactionResponse rejected = transactionService.reject(
			created.id(),
			new TransactionDecisionRequest("op-001", "Rejected by test")
		);

		assertThat(rejected.status()).isEqualTo(TransactionStatus.REJECTED);
		assertThat(rejected.reviewedBy()).isEqualTo("op-001");
	}

	@Test
	void approvesRollbackAndCreatesRefundTransaction() {
		TransactionResponse created = transactionService.create(new TransactionRequest(
			"TEST-TXN-ROLLBACK-1",
			"ACC-005",
			"PAYEE-NEW-RB1",
			new BigDecimal("14000.00"),
			"USD",
			Instant.now(),
			"Rollback approval flow"
		));

		TransactionResponse approved = transactionService.approve(
			created.id(),
			new TransactionDecisionRequest("op-001", "Complete transaction first")
		);

		TransactionResponse rollbackRequested = transactionService.requestRollback(
			approved.id(),
			new TransactionRollbackRequest("DUPLICATE", "Duplicate transfer", "customer-123", "CASE-100")
		);

		assertThat(rollbackRequested.status()).isEqualTo(TransactionStatus.ROLLBACK_REQUESTED);

		TransactionResponse refunded = transactionService.approveRollback(
			approved.id(),
			new TransactionRollbackDecisionRequest("op-001", "Approved refund")
		);

		assertThat(refunded.status()).isEqualTo(TransactionStatus.REFUNDED);
		assertThat(refunded.refundTransactionId()).isNotNull();
		assertThat(refunded.rollbackReviewedBy()).isEqualTo("op-001");

		TransactionResponse refundTransaction = transactionService.getById(refunded.refundTransactionId());
		assertThat(refundTransaction.amount()).isEqualByComparingTo(new BigDecimal("-14000.00"));
		assertThat(refundTransaction.refundedForTransactionId()).isEqualTo(approved.id());
	}

	@Test
	void rejectsRollbackRequest() {
		TransactionResponse created = transactionService.create(new TransactionRequest(
			"TEST-TXN-ROLLBACK-2",
			"ACC-006",
			"PAYEE-NEW-RB2",
			new BigDecimal("15000.00"),
			"USD",
			Instant.now(),
			"Rollback rejection flow"
		));

		TransactionResponse approved = transactionService.approve(
			created.id(),
			new TransactionDecisionRequest("op-001", "Approve for rollback test")
		);

		transactionService.requestRollback(
			approved.id(),
			new TransactionRollbackRequest("CUSTOMER_REQUEST", "No longer needed", "customer-222", "CASE-200")
		);

		TransactionResponse rejectedRollback = transactionService.rejectRollback(
			approved.id(),
			new TransactionRollbackDecisionRequest("op-001", "Insufficient reason")
		);

		assertThat(rejectedRollback.status()).isEqualTo(TransactionStatus.ROLLBACK_REJECTED);
	}

	@Test
	void rejectsRollbackRequestWhenNotCompleted() {
		TransactionResponse created = transactionService.create(new TransactionRequest(
			"TEST-TXN-ROLLBACK-3",
			"ACC-007",
			"PAYEE-NEW-RB3",
			new BigDecimal("16000.00"),
			"USD",
			Instant.now(),
			"Should remain pending"
		));

		assertThatThrownBy(() -> transactionService.requestRollback(
			created.id(),
			new TransactionRollbackRequest("DUPLICATE", "Not completed yet", "customer-333", null)
		)).isInstanceOf(BadRequestException.class);
	}

	@Test
	void rejectsInvalidLifecycleTransition() throws Exception {
		transactionService.create(new TransactionRequest(
			"TEST-TXN-2",
			"ACC-002",
			"PAYEE-NEW",
			new BigDecimal("12000.00"),
			"USD",
			Instant.now(),
			"Integration test lifecycle"
		));

		Alert alert = alertRepository.findAll().stream()
			.filter(a -> a.getStatus() == AlertStatus.OPEN)
			.findFirst()
			.orElseThrow();

		assertThatThrownBy(() -> alertService.updateStatus(
			alert.getId(),
			new AlertStatusUpdateRequest(AlertStatus.CLOSED, "invalid direct close")
		)).isInstanceOf(BadRequestException.class);
	}

	@Test
	void createsMonitoringRuleForVelocityType() {
		String suffix = String.valueOf(System.nanoTime());
		RuleCreateRequest request = new RuleCreateRequest(
			"Velocity Rule " + suffix,
			RuleType.VELOCITY,
			AlertSeverity.MEDIUM,
			true,
			null,
			6,
			10
		);

		var created = ruleService.createRule(request);

		assertThat(created.id()).isNotNull();
		assertThat(created.name()).isEqualTo("Velocity Rule " + suffix);
		assertThat(created.type()).isEqualTo(RuleType.VELOCITY);
		assertThat(created.transactionCountThreshold()).isEqualTo(6);
		assertThat(created.timeWindowMinutes()).isEqualTo(10);
		assertThat(created.amountThreshold()).isNull();
	}

	@Test
	void rejectsCreateRuleWhenRequiredThresholdMissing() {
		RuleCreateRequest request = new RuleCreateRequest(
			"Missing Daily Threshold " + System.nanoTime(),
			RuleType.DAILY_LIMIT,
			AlertSeverity.HIGH,
			true,
			null,
			null,
			null
		);

		assertThatThrownBy(() -> ruleService.createRule(request))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("amountThreshold");
	}

	@Test
	void rejectsCreateRuleWhenNameIsBlank() {
		assertThatThrownBy(() -> ruleService.createRule(new RuleCreateRequest(
			"   ",
			RuleType.NEW_PAYEE,
			AlertSeverity.LOW,
			true,
			null,
			null,
			null
		)))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("name is required");
	}

	@Test
	void rejectsCreateRuleWhenNameAlreadyExists() {
		String name = "Duplicate Name Rule " + System.nanoTime();

		ruleService.createRule(new RuleCreateRequest(
			name,
			RuleType.NEW_PAYEE,
			AlertSeverity.LOW,
			true,
			null,
			null,
			null
		));

		assertThatThrownBy(() -> ruleService.createRule(new RuleCreateRequest(
			name.toUpperCase(),
			RuleType.NEW_PAYEE,
			AlertSeverity.LOW,
			true,
			null,
			null,
			null
		)))
			.isInstanceOf(ConflictException.class);
	}

	@Test
	void filtersRulesByActiveFlag() {
		String name = "Inactive Rule " + System.nanoTime();
		RuleResponse created = ruleService.createRule(new RuleCreateRequest(
			name,
			RuleType.NEW_PAYEE,
			AlertSeverity.LOW,
			false,
			null,
			null,
			null
		));

		List<RuleResponse> inactiveRules = ruleService.getRules(false, null, null);

		assertThat(inactiveRules).anyMatch(rule -> rule.id().equals(created.id()));
		assertThat(inactiveRules).allMatch(rule -> !rule.active());
	}

	@Test
	void filtersRulesByTypeAndSeverity() {
		String name = "Velocity Low Rule " + System.nanoTime();
		RuleResponse created = ruleService.createRule(new RuleCreateRequest(
			name,
			RuleType.VELOCITY,
			AlertSeverity.LOW,
			true,
			null,
			4,
			15
		));

		List<RuleResponse> filtered = ruleService.getRules(null, RuleType.VELOCITY, AlertSeverity.LOW);

		assertThat(filtered).anyMatch(rule -> rule.id().equals(created.id()));
		assertThat(filtered).allMatch(rule -> rule.type() == RuleType.VELOCITY);
		assertThat(filtered).allMatch(rule -> rule.severity() == AlertSeverity.LOW);
	}

	@Test
	void updatesExistingRuleWithNewTypeAndThresholds() {
		RuleResponse created = ruleService.createRule(new RuleCreateRequest(
			"Update Target " + System.nanoTime(),
			RuleType.NEW_PAYEE,
			AlertSeverity.MEDIUM,
			true,
			null,
			null,
			null
		));

		RuleResponse updated = ruleService.updateRule(created.id(), new RuleUpdateRequest(
			created.name() + " Renamed",
			RuleType.VELOCITY,
			AlertSeverity.HIGH,
			false,
			null,
			8,
			20
		));

		assertThat(updated.name()).isEqualTo(created.name() + " Renamed");
		assertThat(updated.type()).isEqualTo(RuleType.VELOCITY);
		assertThat(updated.severity()).isEqualTo(AlertSeverity.HIGH);
		assertThat(updated.active()).isFalse();
		assertThat(updated.transactionCountThreshold()).isEqualTo(8);
		assertThat(updated.timeWindowMinutes()).isEqualTo(20);
		assertThat(updated.amountThreshold()).isNull();
	}

	@Test
	void rejectsUpdateWhenRuleTypeRequiresMissingThreshold() {
		RuleResponse created = ruleService.createRule(new RuleCreateRequest(
			"Update Validation " + System.nanoTime(),
			RuleType.NEW_PAYEE,
			AlertSeverity.MEDIUM,
			true,
			null,
			null,
			null
		));

		assertThatThrownBy(() -> ruleService.updateRule(created.id(), new RuleUpdateRequest(
			created.name(),
			RuleType.DAILY_LIMIT,
			AlertSeverity.MEDIUM,
			true,
			null,
			null,
			null
		)))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("amountThreshold");
	}

	@Test
	void rejectsUpdateWhenRuleNameAlreadyExists() {
		String firstName = "Name First " + System.nanoTime();
		String secondName = "Name Second " + System.nanoTime();

		ruleService.createRule(new RuleCreateRequest(
			firstName,
			RuleType.NEW_PAYEE,
			AlertSeverity.LOW,
			true,
			null,
			null,
			null
		));

		RuleResponse second = ruleService.createRule(new RuleCreateRequest(
			secondName,
			RuleType.NEW_PAYEE,
			AlertSeverity.LOW,
			true,
			null,
			null,
			null
		));

		assertThatThrownBy(() -> ruleService.updateRule(second.id(), new RuleUpdateRequest(
			firstName.toUpperCase(),
			RuleType.NEW_PAYEE,
			AlertSeverity.LOW,
			true,
			null,
			null,
			null
		)))
			.isInstanceOf(ConflictException.class);
	}

	@Test
	void returnsNotFoundWhenUpdatingMissingRule() {
		assertThatThrownBy(() -> ruleService.updateRule(999999L, new RuleUpdateRequest(
			"Missing Rule",
			RuleType.NEW_PAYEE,
			AlertSeverity.LOW,
			true,
			null,
			null,
			null
		)))
			.isInstanceOf(NotFoundException.class);
	}

	@Test
	void rejectsUpdateRuleWhenTypeIsMissing() {
		RuleResponse created = ruleService.createRule(new RuleCreateRequest(
			"Missing Type Update " + System.nanoTime(),
			RuleType.NEW_PAYEE,
			AlertSeverity.MEDIUM,
			true,
			null,
			null,
			null
		));

		assertThatThrownBy(() -> ruleService.updateRule(created.id(), new RuleUpdateRequest(
			created.name(),
			null,
			AlertSeverity.MEDIUM,
			true,
			null,
			null,
			null
		)))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("type is required");
	}

	@Test
	void updatesOnlyRuleActiveStatus() {
		RuleResponse created = ruleService.createRule(new RuleCreateRequest(
			"Status Target " + System.nanoTime(),
			RuleType.VELOCITY,
			AlertSeverity.MEDIUM,
			true,
			null,
			6,
			12
		));

		RuleResponse updated = ruleService.updateRuleStatus(
			created.id(),
			new RuleStatusUpdateRequest(false)
		);

		assertThat(updated.active()).isFalse();
		assertThat(updated.name()).isEqualTo(created.name());
		assertThat(updated.type()).isEqualTo(created.type());
		assertThat(updated.severity()).isEqualTo(created.severity());
		assertThat(updated.amountThreshold()).isEqualTo(created.amountThreshold());
		assertThat(updated.transactionCountThreshold()).isEqualTo(created.transactionCountThreshold());
		assertThat(updated.timeWindowMinutes()).isEqualTo(created.timeWindowMinutes());
	}

	@Test
	void returnsNotFoundWhenUpdatingMissingRuleStatus() {
		assertThatThrownBy(() -> ruleService.updateRuleStatus(999999L, new RuleStatusUpdateRequest(true)))
			.isInstanceOf(NotFoundException.class);
	}

	@Test
	void softDeletesRuleByDisablingIt() {
		RuleResponse created = ruleService.createRule(new RuleCreateRequest(
			"Soft Delete Target " + System.nanoTime(),
			RuleType.AMOUNT_THRESHOLD,
			AlertSeverity.HIGH,
			true,
			new BigDecimal("25000.00"),
			null,
			null
		));

		RuleResponse deleted = ruleService.softDeleteRule(created.id());

		assertThat(deleted.active()).isFalse();
		assertThat(deleted.name()).isEqualTo(created.name());
		assertThat(deleted.type()).isEqualTo(created.type());
		assertThat(deleted.severity()).isEqualTo(created.severity());

		List<RuleResponse> activeRules = ruleService.getRules(true, null, null);
		assertThat(activeRules).noneMatch(rule -> rule.id().equals(created.id()));

		List<RuleResponse> inactiveRules = ruleService.getRules(false, null, null);
		assertThat(inactiveRules).anyMatch(rule -> rule.id().equals(created.id()));
	}

	@Test
	void returnsNotFoundWhenSoftDeletingMissingRule() {
		assertThatThrownBy(() -> ruleService.softDeleteRule(999999L))
			.isInstanceOf(NotFoundException.class);
	}

	@Test
	void storesRuleExecutionHistoryForEachActiveRuleEvaluation() {
		TransactionResponse response = transactionService.create(new TransactionRequest(
			"TEST-TXN-HISTORY-" + System.nanoTime(),
			"ACC-HISTORY-001",
			"PAYEE-HISTORY-NEW",
			new BigDecimal("20000.00"),
			"USD",
			Instant.now(),
			"Rule history validation transaction"
		));

		List<RuleExecutionHistory> histories = ruleExecutionHistoryRepository.findByTransactionId(response.id());

		assertThat(histories).isNotEmpty();
		assertThat(histories).allMatch(h -> h.getRuleId() != null);
		assertThat(histories).allMatch(h -> h.getTransactionId().equals(response.id()));
		assertThat(histories).allMatch(h -> h.getCreatedAt() != null);
		assertThat(histories).allMatch(h -> h.getMessage() != null && !h.getMessage().isBlank());
		assertThat(histories).anyMatch(h -> h.getOutcome() == RuleExecutionOutcome.TRIGGERED);
		assertThat(histories).anyMatch(h -> h.getOutcome() == RuleExecutionOutcome.NOT_TRIGGERED);
		assertThat(histories.stream().map(RuleExecutionHistory::getExecutionId).distinct().count()).isEqualTo(1);
	}

	@Test
	void storesRuleAuditHistoryAcrossRuleMutations() {
		RuleResponse created = ruleService.createRule(new RuleCreateRequest(
			"Audit Trail Rule " + System.nanoTime(),
			RuleType.NEW_PAYEE,
			AlertSeverity.MEDIUM,
			true,
			null,
			null,
			null
		));

		ruleService.updateRule(created.id(), new RuleUpdateRequest(
			created.name() + " Updated",
			RuleType.AMOUNT_THRESHOLD,
			AlertSeverity.HIGH,
			true,
			new BigDecimal("10000.00"),
			null,
			null
		));

		ruleService.updateRuleStatus(created.id(), new RuleStatusUpdateRequest(false));
		ruleService.softDeleteRule(created.id());

		List<RuleAuditHistoryResponse> history = ruleService.getRuleHistory(created.id());

		assertThat(history).isNotEmpty();
		assertThat(history).anyMatch(h -> h.action() == RuleAuditAction.CREATED);
		assertThat(history).anyMatch(h -> h.action() == RuleAuditAction.UPDATED);
		assertThat(history).anyMatch(h -> h.action() == RuleAuditAction.DEACTIVATED);
		assertThat(history).anyMatch(h -> h.action() == RuleAuditAction.DELETED);
		assertThat(history).allMatch(h -> h.ruleId().equals(created.id()));
		assertThat(history).allMatch(h -> h.changedAt() != null);
		assertThat(history).allMatch(h -> h.changedBy() != null && !h.changedBy().isBlank());
	}

	@Test
	void returnsNotFoundWhenReadingRuleAuditHistoryForMissingRule() {
		assertThatThrownBy(() -> ruleService.getRuleHistory(999999L))
			.isInstanceOf(NotFoundException.class);
	}

	@Test
	void returnsClearNotFoundMessageForMissingRule() {
		assertThatThrownBy(() -> ruleService.updateRule(999999L, new RuleUpdateRequest(
			"Missing Rule",
			RuleType.NEW_PAYEE,
			AlertSeverity.LOW,
			true,
			null,
			null,
			null
		)))
			.isInstanceOf(NotFoundException.class)
			.hasMessage("Rule with id 999999 not found");
	}

	@Test
	void rejectsInvalidRuleStatusOperationWhenStatusIsUnchanged() {
		RuleResponse created = ruleService.createRule(new RuleCreateRequest(
			"Status Error Rule " + System.nanoTime(),
			RuleType.NEW_PAYEE,
			AlertSeverity.MEDIUM,
			true,
			null,
			null,
			null
		));

		assertThatThrownBy(() -> ruleService.updateRuleStatus(created.id(), new RuleStatusUpdateRequest(true)))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("already active");
	}

	@Test
	void returnsRuleStatisticsWithTotalsAndGroupings() {
		RuleStatsResponse before = ruleService.getRuleStats();

		ruleService.createRule(new RuleCreateRequest(
			"Stats Rule Amount " + System.nanoTime(),
			RuleType.AMOUNT_THRESHOLD,
			AlertSeverity.HIGH,
			true,
			new BigDecimal("15000.00"),
			null,
			null
		));

		ruleService.createRule(new RuleCreateRequest(
			"Stats Rule New Payee " + System.nanoTime(),
			RuleType.NEW_PAYEE,
			AlertSeverity.LOW,
			false,
			null,
			null,
			null
		));

		RuleStatsResponse after = ruleService.getRuleStats();

		assertThat(after.totalRules()).isEqualTo(before.totalRules() + 2);
		assertThat(after.activeRules()).isEqualTo(before.activeRules() + 1);
		assertThat(after.inactiveRules()).isEqualTo(before.inactiveRules() + 1);

		assertThat(after.rulesByType().get(RuleType.AMOUNT_THRESHOLD))
			.isEqualTo(before.rulesByType().get(RuleType.AMOUNT_THRESHOLD) + 1);
		assertThat(after.rulesByType().get(RuleType.NEW_PAYEE))
			.isEqualTo(before.rulesByType().get(RuleType.NEW_PAYEE) + 1);

		assertThat(after.rulesBySeverity().get(AlertSeverity.HIGH))
			.isEqualTo(before.rulesBySeverity().get(AlertSeverity.HIGH) + 1);
		assertThat(after.rulesBySeverity().get(AlertSeverity.LOW))
			.isEqualTo(before.rulesBySeverity().get(AlertSeverity.LOW) + 1);

		assertThat(after.rulesByType()).containsKeys(RuleType.AMOUNT_THRESHOLD, RuleType.VELOCITY, RuleType.NEW_PAYEE, RuleType.DAILY_LIMIT);
		assertThat(after.rulesBySeverity()).containsKeys(AlertSeverity.HIGH, AlertSeverity.MEDIUM, AlertSeverity.LOW);
	}

	@Test
	void ruleEngineTriggersAmountThresholdRuleForLargeTransaction() {
		String suffix = String.valueOf(System.nanoTime());
		String ruleName = "IT-Amount-Rule-" + suffix;
		long alertsBefore = countAlertsByRuleName(ruleName);

		ruleService.createRule(new RuleCreateRequest(
			ruleName,
			RuleType.AMOUNT_THRESHOLD,
			AlertSeverity.HIGH,
			true,
			new BigDecimal("10000.00"),
			null,
			null
		));

		transactionService.create(new TransactionRequest(
			"IT-TXN-AMT-" + suffix,
			"ACC-IT-AMT-" + suffix,
			"PAYEE-IT-AMT-" + suffix,
			new BigDecimal("15000.00"),
			"USD",
			Instant.now(),
			"Amount threshold integration test"
		));

		long alertsAfter = countAlertsByRuleName(ruleName);
		assertThat(alertsAfter).isEqualTo(alertsBefore + 1);
		assertThat(latestAlertByRuleName(ruleName)).isPresent();
		assertThat(latestAlertByRuleName(ruleName).orElseThrow().getMessage()).contains("exceeded threshold");
	}

	@Test
	void ruleEngineDoesNotTriggerAmountThresholdRuleBelowThreshold() {
		String suffix = String.valueOf(System.nanoTime());
		String ruleName = "IT-Amount-Neg-Rule-" + suffix;
		String account = "ACC-IT-AMT-NEG-" + suffix;
		String payee = "PAYEE-IT-AMT-NEG-" + suffix;

		ruleService.createRule(new RuleCreateRequest(
			ruleName,
			RuleType.AMOUNT_THRESHOLD,
			AlertSeverity.HIGH,
			true,
			new BigDecimal("50000.00"),
			null,
			null
		));

		transactionService.create(new TransactionRequest(
			"IT-TXN-AMT-NEG-SEED-" + suffix,
			account,
			payee,
			new BigDecimal("20.00"),
			"USD",
			Instant.now(),
			"Seed payee usage"
		));

		long alertsBefore = countAlertsByRuleName(ruleName);

		transactionService.create(new TransactionRequest(
			"IT-TXN-AMT-NEG-" + suffix,
			account,
			payee,
			new BigDecimal("100.00"),
			"USD",
			Instant.now(),
			"Below amount threshold"
		));

		long alertsAfter = countAlertsByRuleName(ruleName);
		assertThat(alertsAfter).isEqualTo(alertsBefore);
	}

	@Test
	void ruleEngineTriggersVelocityRuleWhenFrequencyExceeded() {
		String suffix = String.valueOf(System.nanoTime());
		String ruleName = "IT-Velocity-Rule-" + suffix;
		String account = "ACC-IT-VEL-" + suffix;
		Instant base = Instant.now();

		RuleResponse velocityRule = ruleService.createRule(new RuleCreateRequest(
			ruleName,
			RuleType.VELOCITY,
			AlertSeverity.MEDIUM,
			true,
			null,
			1,
			10
		));

		transactionService.create(new TransactionRequest(
			"IT-TXN-VEL-SEED-A-" + suffix,
			account,
			"PAYEE-IT-VEL-A",
			new BigDecimal("10.00"),
			"USD",
			base.minusSeconds(4),
			"Seed velocity payee A"
		));
		transactionService.create(new TransactionRequest(
			"IT-TXN-VEL-SEED-B-" + suffix,
			account,
			"PAYEE-IT-VEL-B",
			new BigDecimal("10.00"),
			"USD",
			base.minusSeconds(4),
			"Seed velocity payee B"
		));

		transactionService.create(new TransactionRequest(
			"IT-TXN-VEL-1-" + suffix,
			account,
			"PAYEE-IT-VEL-A",
			new BigDecimal("40.00"),
			"USD",
			base.minusSeconds(2),
			"Velocity tx 1"
		));

		TransactionResponse last = transactionService.create(new TransactionRequest(
			"IT-TXN-VEL-2-" + suffix,
			account,
			"PAYEE-IT-VEL-B",
			new BigDecimal("45.00"),
			"USD",
			base.minusSeconds(1),
			"Velocity tx 2"
		));

		List<RuleExecutionHistory> histories = ruleExecutionHistoryRepository.findByTransactionId(last.id());
		assertThat(histories)
			.anyMatch(h -> h.getRuleId().equals(velocityRule.id()) && h.getOutcome() == RuleExecutionOutcome.TRIGGERED);
	}

	@Test
	void ruleEngineTriggersNewPayeeRuleForUnseenPayee() {
		String suffix = String.valueOf(System.nanoTime());
		String ruleName = "IT-NewPayee-Rule-" + suffix;
		String account = "ACC-IT-NP-" + suffix;
		String payee = "PAYEE-IT-NP-" + suffix;

		RuleResponse newPayeeRule = ruleService.createRule(new RuleCreateRequest(
			ruleName,
			RuleType.NEW_PAYEE,
			AlertSeverity.MEDIUM,
			true,
			null,
			null,
			null
		));

		TransactionResponse created = transactionService.create(new TransactionRequest(
			"IT-TXN-NP-" + suffix,
			account,
			payee,
			new BigDecimal("80.00"),
			"USD",
			Instant.now(),
			"New payee test"
		));

		List<RuleExecutionHistory> histories = ruleExecutionHistoryRepository.findByTransactionId(created.id());
		assertThat(histories)
			.anyMatch(h -> h.getRuleId().equals(newPayeeRule.id()) && h.getOutcome() == RuleExecutionOutcome.TRIGGERED);
	}

	@Test
	void ruleEngineTriggersDailyLimitRuleWhenDailyTotalExceeded() {
		String suffix = String.valueOf(System.nanoTime());
		String ruleName = "IT-DailyLimit-Rule-" + suffix;
		String account = "ACC-IT-DL-" + suffix;

		ruleService.createRule(new RuleCreateRequest(
			ruleName,
			RuleType.DAILY_LIMIT,
			AlertSeverity.HIGH,
			true,
			new BigDecimal("1000.00"),
			null,
			null
		));

		long alertsBefore = countAlertsByRuleName(ruleName);

		transactionService.create(new TransactionRequest(
			"IT-TXN-DL-1-" + suffix,
			account,
			"PAYEE-IT-DL-A",
			new BigDecimal("600.00"),
			"USD",
			Instant.now(),
			"Daily limit tx 1"
		));

		transactionService.create(new TransactionRequest(
			"IT-TXN-DL-2-" + suffix,
			account,
			"PAYEE-IT-DL-B",
			new BigDecimal("500.00"),
			"USD",
			Instant.now(),
			"Daily limit tx 2"
		));

		long alertsAfter = countAlertsByRuleName(ruleName);
		assertThat(alertsAfter).isGreaterThan(alertsBefore);
		assertThat(latestAlertByRuleName(ruleName)).isPresent();
		assertThat(latestAlertByRuleName(ruleName).orElseThrow().getMessage()).contains("Daily limit exceeded");
	}

	@Test
	void inactiveRuleIsNotEvaluatedByRuleEngine() {
		String suffix = String.valueOf(System.nanoTime());
		String ruleName = "IT-Inactive-Rule-" + suffix;

		ruleService.createRule(new RuleCreateRequest(
			ruleName,
			RuleType.AMOUNT_THRESHOLD,
			AlertSeverity.HIGH,
			false,
			new BigDecimal("1000.00"),
			null,
			null
		));

		long alertsBefore = countAlertsByRuleName(ruleName);

		transactionService.create(new TransactionRequest(
			"IT-TXN-INACTIVE-" + suffix,
			"ACC-IT-INACTIVE-" + suffix,
			"PAYEE-IT-INACTIVE-" + suffix,
			new BigDecimal("50000.00"),
			"USD",
			Instant.now(),
			"Inactive rule should not fire"
		));

		long alertsAfter = countAlertsByRuleName(ruleName);
		assertThat(alertsAfter).isEqualTo(alertsBefore);
	}

	private long countAlertsByRuleName(String ruleName) {
		return alertRepository.findAll().stream()
			.filter(alert -> ruleName.equals(alert.getRuleName()))
			.count();
	}

	private Optional<Alert> latestAlertByRuleName(String ruleName) {
		return alertRepository.findAll().stream()
			.filter(alert -> ruleName.equals(alert.getRuleName()))
			.max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
	}

	private long countAlertsByRuleType(RuleType type) {
		return alertRepository.findAll().stream()
			.filter(alert -> alert.getRuleType() == type)
			.count();
	}

	private Optional<Alert> latestAlertByRuleType(RuleType type) {
		return alertRepository.findAll().stream()
			.filter(alert -> alert.getRuleType() == type)
			.max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
	}

}
