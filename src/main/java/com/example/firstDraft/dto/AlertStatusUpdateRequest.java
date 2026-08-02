package com.example.firstDraft.dto;

import com.example.firstDraft.model.AlertStatus;
import jakarta.validation.constraints.NotNull;

public record AlertStatusUpdateRequest(
    @NotNull AlertStatus status,
    String note
) {
}

