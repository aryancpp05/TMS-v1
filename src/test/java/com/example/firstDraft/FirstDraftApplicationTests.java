package com.example.firstDraft;

import com.example.firstDraft.entity.Alert;
import com.example.firstDraft.exception.BadRequestException;
import com.example.firstDraft.model.AlertStatus;
import com.example.firstDraft.repository.AlertRepository;
import com.example.firstDraft.service.AlertService;
import com.example.firstDraft.service.TransactionService;
import com.example.firstDraft.dto.TransactionRequest;
import com.example.firstDraft.dto.AlertStatusUpdateRequest;
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

		transactionService.create(request);

		List<Alert> alerts = alertRepository.findAll();
		assertThat(alerts).isNotEmpty();
		assertThat(alerts.stream().anyMatch(a -> a.getRuleName().equals("High Amount Threshold"))).isTrue();
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
