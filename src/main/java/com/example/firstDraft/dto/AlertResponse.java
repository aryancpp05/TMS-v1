package com.example.firstDraft.dto;

import com.example.firstDraft.model.AlertSeverity;
import com.example.firstDraft.model.AlertStatus;
import com.example.firstDraft.model.RuleType;

import java.time.Instant;
import java.util.List;

public record AlertResponse(
    Long id,
    String ruleName,
    RuleType ruleType,
    AlertSeverity severity,
    AlertStatus status,
    String message,
    Instant createdAt,
    Instant updatedAt,
    List<TransactionResponse> triggeringTransactions,
    List<AlertHistoryResponse> history
) {
}

