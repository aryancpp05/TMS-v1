package com.example.firstDraft.service;

import com.example.firstDraft.dto.RuleCreateRequest;
import com.example.firstDraft.dto.RuleAuditHistoryResponse;
import com.example.firstDraft.dto.RuleResponse;
import com.example.firstDraft.dto.RuleStatsResponse;
import com.example.firstDraft.dto.RuleStatusUpdateRequest;
import com.example.firstDraft.dto.RuleUpdateRequest;
import com.example.firstDraft.entity.MonitoringRule;
import com.example.firstDraft.entity.RuleAuditHistory;
import com.example.firstDraft.exception.BadRequestException;
import com.example.firstDraft.exception.NotFoundException;
import com.example.firstDraft.exception.ConflictException;
import com.example.firstDraft.model.AlertSeverity;
import com.example.firstDraft.model.RuleAuditAction;
import com.example.firstDraft.model.RuleType;
import com.example.firstDraft.repository.MonitoringRuleRepository;
import com.example.firstDraft.repository.RuleAuditHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class RuleService {

    private final MonitoringRuleRepository ruleRepository;
    private final RuleAuditHistoryRepository ruleAuditHistoryRepository;
    private final RuleValidationService ruleValidationService;

    public RuleService(
        MonitoringRuleRepository ruleRepository,
        RuleAuditHistoryRepository ruleAuditHistoryRepository,
        RuleValidationService ruleValidationService
    ) {
        this.ruleRepository = ruleRepository;
        this.ruleAuditHistoryRepository = ruleAuditHistoryRepository;
        this.ruleValidationService = ruleValidationService;
    }

    public List<RuleResponse> getRules() {
        return getRules(null, null, null);
    }

    public List<RuleResponse> getRules(Boolean active, RuleType type, AlertSeverity severity) {
        return ruleRepository.search(active, type, severity).stream()
            .map(ApiMapper::toRuleResponse)
            .toList();
    }

    public RuleStatsResponse getRuleStats() {
        long totalRules = ruleRepository.count();
        long activeRules = ruleRepository.countByActiveTrue();
        long inactiveRules = ruleRepository.countByActiveFalse();

        Map<RuleType, Long> rulesByType = new EnumMap<>(RuleType.class);
        for (RuleType type : RuleType.values()) {
            rulesByType.put(type, 0L);
        }
        for (Object[] row : ruleRepository.countGroupedByType()) {
            RuleType type = (RuleType) row[0];
            Long count = (Long) row[1];
            rulesByType.put(type, count);
        }

        Map<AlertSeverity, Long> rulesBySeverity = new EnumMap<>(AlertSeverity.class);
        for (AlertSeverity severity : AlertSeverity.values()) {
            rulesBySeverity.put(severity, 0L);
        }
        for (Object[] row : ruleRepository.countGroupedBySeverity()) {
            AlertSeverity severity = (AlertSeverity) row[0];
            Long count = (Long) row[1];
            rulesBySeverity.put(severity, count);
        }

        return new RuleStatsResponse(
            totalRules,
            activeRules,
            inactiveRules,
            rulesByType,
            rulesBySeverity
        );
    }

    public List<RuleAuditHistoryResponse> getRuleHistory(Long id) {
        if (!ruleRepository.existsById(id)) {
            throw new NotFoundException("Rule with id " + id + " not found");
        }

        return ruleAuditHistoryRepository.findByRuleIdOrderByChangedAtAsc(id).stream()
            .map(ApiMapper::toRuleAuditHistoryResponse)
            .toList();
    }

    public RuleResponse createRule(RuleCreateRequest request) {
        ruleValidationService.validateForCreate(request);

        String normalizedName = request.name().trim();
        if (ruleRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ConflictException("Rule name already exists: " + normalizedName);
        }

        MonitoringRule rule = new MonitoringRule();
        rule.setName(normalizedName);
        rule.setType(request.type());
        rule.setSeverity(request.severity());
        rule.setActive(request.active() == null || request.active());

        switch (request.type()) {
            case AMOUNT_THRESHOLD, DAILY_LIMIT -> {
                rule.setAmountThreshold(request.amountThreshold());
                rule.setTransactionCountThreshold(null);
                rule.setTimeWindowMinutes(null);
            }
            case VELOCITY -> {
                rule.setAmountThreshold(null);
                rule.setTransactionCountThreshold(request.transactionCountThreshold());
                rule.setTimeWindowMinutes(request.timeWindowMinutes());
            }
            case NEW_PAYEE -> {
                rule.setAmountThreshold(null);
                rule.setTransactionCountThreshold(null);
                rule.setTimeWindowMinutes(null);
            }
            default -> throw new IllegalStateException("Unexpected rule type: " + request.type());
        }
        MonitoringRule saved = ruleRepository.save(rule);
        recordAudit(saved.getId(), RuleAuditAction.CREATED, null, snapshot(saved));
        return ApiMapper.toRuleResponse(saved);
    }

    public RuleResponse updateRule(Long id, RuleUpdateRequest request) {
        MonitoringRule rule = ruleRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Rule with id " + id + " not found"));
        String previousValues = snapshot(rule);

        ruleValidationService.validateForUpdate(request);

        String normalizedName = request.name().trim();
        if (ruleRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new ConflictException("Rule name already exists: " + normalizedName);
        }

        rule.setName(normalizedName);
        rule.setType(request.type());
        rule.setSeverity(request.severity());
        rule.setActive(Boolean.TRUE.equals(request.active()));

        switch (request.type()) {
            case AMOUNT_THRESHOLD, DAILY_LIMIT -> {
                rule.setAmountThreshold(request.amountThreshold());
                rule.setTransactionCountThreshold(null);
                rule.setTimeWindowMinutes(null);
            }
            case VELOCITY -> {
                rule.setAmountThreshold(null);
                rule.setTransactionCountThreshold(request.transactionCountThreshold());
                rule.setTimeWindowMinutes(request.timeWindowMinutes());
            }
            case NEW_PAYEE -> {
                rule.setAmountThreshold(null);
                rule.setTransactionCountThreshold(null);
                rule.setTimeWindowMinutes(null);
            }
            default -> throw new IllegalStateException("Unexpected rule type: " + request.type());
        }
        MonitoringRule saved = ruleRepository.save(rule);
        recordAudit(saved.getId(), RuleAuditAction.UPDATED, previousValues, snapshot(saved));
        return ApiMapper.toRuleResponse(saved);
    }

    public RuleResponse updateRuleStatus(Long id, RuleStatusUpdateRequest request) {
        MonitoringRule rule = ruleRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Rule with id " + id + " not found"));
        String previousValues = snapshot(rule);

        if (rule.isActive() == request.active()) {
            String status = request.active() ? "active" : "inactive";
            throw new BadRequestException("Rule with id " + id + " is already " + status);
        }

        rule.setActive(request.active());
        MonitoringRule saved = ruleRepository.save(rule);
        RuleAuditAction action = request.active() ? RuleAuditAction.ACTIVATED : RuleAuditAction.DEACTIVATED;
        recordAudit(saved.getId(), action, previousValues, snapshot(saved));
        return ApiMapper.toRuleResponse(saved);
    }

    public RuleResponse softDeleteRule(Long id) {
        MonitoringRule rule = ruleRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Rule with id " + id + " not found"));
        String previousValues = snapshot(rule);

        rule.setActive(false);
        MonitoringRule saved = ruleRepository.save(rule);
        recordAudit(saved.getId(), RuleAuditAction.DELETED, previousValues, snapshot(saved));
        return ApiMapper.toRuleResponse(saved);
    }

    private void recordAudit(Long ruleId, RuleAuditAction action, String previousValues, String newValues) {
        RuleAuditHistory history = new RuleAuditHistory();
        history.setRuleId(ruleId);
        history.setAction(action);
        history.setPreviousValues(previousValues);
        history.setNewValues(newValues);
        history.setChangedAt(Instant.now());
        history.setChangedBy("SYSTEM");
        ruleAuditHistoryRepository.save(history);
    }

    private String snapshot(MonitoringRule rule) {
        return "{" +
            "id=" + rule.getId() +
            ",name='" + rule.getName() + "'" +
            ",type=" + rule.getType() +
            ",severity=" + rule.getSeverity() +
            ",active=" + rule.isActive() +
            ",amountThreshold=" + rule.getAmountThreshold() +
            ",transactionCountThreshold=" + rule.getTransactionCountThreshold() +
            ",timeWindowMinutes=" + rule.getTimeWindowMinutes() +
            ",createdAt=" + rule.getCreatedAt() +
            "}";
    }
}

