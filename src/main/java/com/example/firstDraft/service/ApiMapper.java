package com.example.firstDraft.service;

import com.example.firstDraft.dto.AlertHistoryResponse;
import com.example.firstDraft.dto.AlertResponse;
import com.example.firstDraft.dto.RuleAuditHistoryResponse;
import com.example.firstDraft.dto.RuleResponse;
import com.example.firstDraft.dto.TransactionResponse;
import com.example.firstDraft.entity.Alert;
import com.example.firstDraft.entity.AlertHistory;
import com.example.firstDraft.entity.MonitoringRule;
import com.example.firstDraft.entity.RuleAuditHistory;
import com.example.firstDraft.entity.TransactionRecord;

import java.util.List;

public final class ApiMapper {

    private ApiMapper() {
    }

    public static TransactionResponse toTransactionResponse(TransactionRecord transaction) {
        return new TransactionResponse(
            transaction.getId(),
            transaction.getReference(),
            transaction.getAccountId(),
            transaction.getPayeeId(),
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getTimestamp(),
            transaction.getDescription(),
            transaction.getStatus(),
            transaction.getCreatedAt(),
            transaction.getUpdatedAt(),
            transaction.getReviewedBy(),
            transaction.getReviewedAt(),
            transaction.getReviewNote(),
            transaction.getRollbackReasonCode(),
            transaction.getRollbackReasonDetail(),
            transaction.getRollbackRequestedBy(),
            transaction.getRollbackRequestedAt(),
            transaction.getRollbackSupportingReference(),
            transaction.getRollbackReviewedBy(),
            transaction.getRollbackReviewedAt(),
            transaction.getRollbackReviewNote(),
            transaction.getRefundedAt(),
            transaction.getRefundTransactionId(),
            transaction.getRefundedForTransactionId()
        );
    }

    public static AlertHistoryResponse toHistoryResponse(AlertHistory history) {
        return new AlertHistoryResponse(
            history.getId(),
            history.getFromStatus(),
            history.getToStatus(),
            history.getNote(),
            history.getCreatedAt()
        );
    }

    public static AlertResponse toAlertResponse(Alert alert, List<AlertHistory> history) {
        List<TransactionResponse> transactions = alert.getTriggeringTransactions()
            .stream()
            .map(ApiMapper::toTransactionResponse)
            .toList();

        List<AlertHistoryResponse> historyRows = history.stream()
            .map(ApiMapper::toHistoryResponse)
            .toList();

        return new AlertResponse(
            alert.getId(),
            alert.getRuleName(),
            alert.getRuleType(),
            alert.getSeverity(),
            alert.getStatus(),
            alert.getMessage(),
            alert.getCreatedAt(),
            alert.getUpdatedAt(),
            transactions,
            historyRows
        );
    }

    public static RuleResponse toRuleResponse(MonitoringRule rule) {
        return new RuleResponse(
            rule.getId(),
            rule.getName(),
            rule.getType(),
            rule.getSeverity(),
            rule.isActive(),
            rule.getAmountThreshold(),
            rule.getTransactionCountThreshold(),
            rule.getTimeWindowMinutes()
        );
    }

    public static RuleAuditHistoryResponse toRuleAuditHistoryResponse(RuleAuditHistory history) {
        return new RuleAuditHistoryResponse(
            history.getId(),
            history.getRuleId(),
            history.getAction(),
            history.getPreviousValues(),
            history.getNewValues(),
            history.getChangedAt(),
            history.getChangedBy()
        );
    }
}

