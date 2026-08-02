package com.example.firstDraft.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionRequest(
    @NotBlank String reference,
    @NotBlank String accountId,
    @NotBlank String payeeId,
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
    @NotBlank String currency,
    @NotNull Instant timestamp,
    @NotBlank String description
) {
}

