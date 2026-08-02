package com.example.firstDraft.service;

import com.example.firstDraft.dto.AlertResponse;
import com.example.firstDraft.dto.AlertStatusUpdateRequest;
import com.example.firstDraft.entity.Alert;
import com.example.firstDraft.entity.AlertHistory;
import com.example.firstDraft.exception.BadRequestException;
import com.example.firstDraft.exception.NotFoundException;
import com.example.firstDraft.model.AlertSeverity;
import com.example.firstDraft.model.AlertStatus;
import com.example.firstDraft.repository.AlertHistoryRepository;
import com.example.firstDraft.repository.AlertRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AlertService {

    private static final Set<AlertStatus> ACTIVE_STATUSES = Set.of(
        AlertStatus.OPEN,
        AlertStatus.ACKNOWLEDGED,
        AlertStatus.INVESTIGATING
    );

    private static final Map<AlertStatus, Set<AlertStatus>> VALID_TRANSITIONS = Map.of(
        AlertStatus.OPEN, Set.of(AlertStatus.ACKNOWLEDGED, AlertStatus.INVESTIGATING, AlertStatus.CLOSED, AlertStatus.DISMISSED),
        AlertStatus.ACKNOWLEDGED, Set.of(AlertStatus.INVESTIGATING, AlertStatus.CLOSED, AlertStatus.DISMISSED),
        AlertStatus.INVESTIGATING, Set.of(AlertStatus.CLOSED, AlertStatus.DISMISSED),
        AlertStatus.CLOSED, Set.of(),
        AlertStatus.DISMISSED, Set.of()
    );

    private final AlertRepository alertRepository;
    private final AlertHistoryRepository alertHistoryRepository;

    public AlertService(AlertRepository alertRepository, AlertHistoryRepository alertHistoryRepository) {
        this.alertRepository = alertRepository;
        this.alertHistoryRepository = alertHistoryRepository;
    }

    public List<AlertResponse> getAlerts(AlertStatus status, AlertSeverity severity, boolean activeOnly) {
        return alertRepository.search(status, severity, activeOnly, ACTIVE_STATUSES).stream()
            .map(this::toAlertResponse)
            .toList();
    }

    public AlertResponse getAlert(Long id) {
        Alert alert = alertRepository.findWithTriggeringTransactionsById(id)
            .orElseThrow(() -> new NotFoundException("Alert not found: " + id));
        return toAlertResponse(alert);
    }

    @Transactional
    public AlertResponse updateStatus(Long id, AlertStatusUpdateRequest request) {
        Alert alert = alertRepository.findWithTriggeringTransactionsById(id)
            .orElseThrow(() -> new NotFoundException("Alert not found: " + id));

        if (alert.getStatus() == request.status()) {
            throw new BadRequestException("Alert is already in status " + request.status());
        }

        Set<AlertStatus> allowed = VALID_TRANSITIONS.getOrDefault(alert.getStatus(), Set.of());
        if (!allowed.contains(request.status())) {
            throw new BadRequestException(
                "Invalid transition from " + alert.getStatus() + " to " + request.status()
            );
        }

        transitionAlert(alert, request.status(), request.operatorId().trim(), normalizeNote(request.note(), "Status updated"));

        return toAlertResponse(alert);
    }

    @Transactional
    public void resolveAlertsForTransactionDecision(Long transactionId, String operatorId, boolean approved, String note) {
        String actor = operatorId == null || operatorId.isBlank() ? "system" : operatorId.trim();
        AlertStatus nextStatus = approved ? AlertStatus.CLOSED : AlertStatus.DISMISSED;
        String defaultNote = approved
            ? "Transaction approved. Alert closed by decision workflow."
            : "Transaction rejected. Alert dismissed by decision workflow.";

        List<Alert> alerts = alertRepository.findActiveByTriggeringTransactionId(transactionId, ACTIVE_STATUSES);
        for (Alert alert : alerts) {
            transitionAlert(alert, nextStatus, actor, normalizeNote(note, defaultNote));
        }
    }

    public List<com.example.firstDraft.dto.AlertHistoryResponse> getHistory(Long id) {
        if (!alertRepository.existsById(id)) {
            throw new NotFoundException("Alert not found: " + id);
        }

        return alertHistoryRepository.findByAlertIdOrderByCreatedAtAsc(id).stream()
            .map(ApiMapper::toHistoryResponse)
            .toList();
    }

    private AlertResponse toAlertResponse(Alert alert) {
        List<AlertHistory> history = alertHistoryRepository.findByAlertIdOrderByCreatedAtAsc(alert.getId());
        return ApiMapper.toAlertResponse(alert, history);
    }

    private void transitionAlert(Alert alert, AlertStatus nextStatus, String changedBy, String note) {
        AlertStatus previousStatus = alert.getStatus();
        alert.setStatus(nextStatus);
        alert.setUpdatedAt(Instant.now());
        alertRepository.save(alert);

        AlertHistory history = new AlertHistory();
        history.setAlert(alert);
        history.setFromStatus(previousStatus);
        history.setToStatus(nextStatus);
        history.setNote(note);
        history.setChangedBy(changedBy);
        history.setCreatedAt(Instant.now());
        alertHistoryRepository.save(history);
    }

    private String normalizeNote(String note, String defaultNote) {
        if (note == null || note.isBlank()) {
            return defaultNote;
        }
        return note.trim();
    }
}

