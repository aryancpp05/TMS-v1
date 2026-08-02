package com.example.firstDraft.dto;

import com.example.firstDraft.model.RuleAuditAction;

import java.time.Instant;

public record RuleAuditHistoryResponse(
    Long id,
    Long ruleId,
    RuleAuditAction action,
    String previousValues,
    String newValues,
    Instant changedAt,
    String changedBy
) {
}

