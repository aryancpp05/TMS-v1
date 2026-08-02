package com.example.firstDraft.dto;

import com.example.firstDraft.model.AlertSeverity;
import com.example.firstDraft.model.RuleType;

import java.math.BigDecimal;

public record RuleResponse(
    Long id,
    String name,
    RuleType type,
    AlertSeverity severity,
    boolean active,
    BigDecimal amountThreshold,
    Integer transactionCountThreshold,
    Integer timeWindowMinutes
) {
}

