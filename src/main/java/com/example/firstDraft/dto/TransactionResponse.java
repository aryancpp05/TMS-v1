package com.example.firstDraft.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
    Long id,
    String reference,
    String accountId,
    String payeeId,
    BigDecimal amount,
    String currency,
    Instant timestamp,
    String description
) {
}

