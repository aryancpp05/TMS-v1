package com.example.firstDraft.dto;

import com.example.firstDraft.model.TransactionStatus;

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
    String description,
    TransactionStatus status,
    Instant createdAt,
    Instant updatedAt,
    String reviewedBy,
    Instant reviewedAt,
    String reviewNote,
    String rollbackReasonCode,
    String rollbackReasonDetail,
    String rollbackRequestedBy,
    Instant rollbackRequestedAt,
    String rollbackSupportingReference,
    String rollbackReviewedBy,
    Instant rollbackReviewedAt,
    String rollbackReviewNote,
    Instant refundedAt,
    Long refundTransactionId,
    Long refundedForTransactionId
) {
}

