package com.example.firstDraft.dto;

import jakarta.validation.constraints.NotBlank;

public record TransactionRollbackDecisionRequest(
    @NotBlank String operatorId,
    String note
) {
}

