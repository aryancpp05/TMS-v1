package com.example.firstDraft.dto;

import com.example.firstDraft.model.AlertSeverity;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RuleUpdateRequest(
    @NotNull AlertSeverity severity,
    @NotNull Boolean active,
    BigDecimal amountThreshold,
    Integer transactionCountThreshold,
    Integer timeWindowMinutes
) {
}

