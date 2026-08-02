package com.example.firstDraft.service;

import com.example.firstDraft.dto.RuleCreateRequest;
import com.example.firstDraft.dto.RuleUpdateRequest;
import com.example.firstDraft.exception.BadRequestException;
import com.example.firstDraft.model.AlertSeverity;
import com.example.firstDraft.model.RuleType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RuleValidationService {

    public void validateForCreate(RuleCreateRequest request) {
        validateCommonFields(request.name(), request.type(), request.severity());
        validateRuleConfiguration(
            request.type(),
            request.amountThreshold(),
            request.transactionCountThreshold(),
            request.timeWindowMinutes()
        );
    }

    public void validateForUpdate(RuleUpdateRequest request) {
        validateCommonFields(request.name(), request.type(), request.severity());
        validateRuleConfiguration(
            request.type(),
            request.amountThreshold(),
            request.transactionCountThreshold(),
            request.timeWindowMinutes()
        );
    }

    private void validateCommonFields(String name, RuleType type, AlertSeverity severity) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("name is required");
        }
        if (type == null) {
            throw new BadRequestException("type is required");
        }
        if (severity == null) {
            throw new BadRequestException("severity is required");
        }
    }

    private void validateRuleConfiguration(
        RuleType type,
        BigDecimal amountThreshold,
        Integer transactionCountThreshold,
        Integer timeWindowMinutes
    ) {
        switch (type) {
            case AMOUNT_THRESHOLD, DAILY_LIMIT -> requirePositiveAmount(amountThreshold, "amountThreshold");
            case VELOCITY -> {
                requirePositiveInteger(transactionCountThreshold, "transactionCountThreshold");
                requirePositiveInteger(timeWindowMinutes, "timeWindowMinutes");
            }
            case NEW_PAYEE -> {
                // No extra required fields for NEW_PAYEE in current project design.
            }
            default -> throw new BadRequestException("Unsupported rule type: " + type);
        }
    }

    private void requirePositiveAmount(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new BadRequestException(field + " must be a positive value");
        }
    }

    private void requirePositiveInteger(Integer value, String field) {
        if (value == null || value <= 0) {
            throw new BadRequestException(field + " must be a positive value");
        }
    }
}

