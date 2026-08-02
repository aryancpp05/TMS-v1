package com.example.firstDraft.service.ruleengine;

import com.example.firstDraft.entity.TransactionRecord;

import java.util.List;

public record RuleEvaluationResult(
    String reason,
    List<TransactionRecord> triggeringTransactions
) {
}

