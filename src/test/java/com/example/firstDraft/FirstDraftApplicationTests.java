package com.example.firstDraft;

import com.example.firstDraft.entity.Alert;
import com.example.firstDraft.exception.BadRequestException;
import com.example.firstDraft.model.AlertStatus;
import com.example.firstDraft.model.TransactionStatus;
import com.example.firstDraft.repository.AlertRepository;
import com.example.firstDraft.service.AlertService;
import com.example.firstDraft.service.TransactionService;
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

}
