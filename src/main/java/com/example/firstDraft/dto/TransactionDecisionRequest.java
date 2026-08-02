package com.example.firstDraft.dto;

import jakarta.validation.constraints.NotBlank;

public record TransactionDecisionRequest(
    @NotBlank String operatorId,
    String note
) {
}

