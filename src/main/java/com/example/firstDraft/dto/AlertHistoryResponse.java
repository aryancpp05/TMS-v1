package com.example.firstDraft.dto;

import com.example.firstDraft.model.AlertStatus;

import java.time.Instant;

public record AlertHistoryResponse(
    Long id,
    AlertStatus fromStatus,
    AlertStatus toStatus,
    String note,
    Instant createdAt
) {
}

