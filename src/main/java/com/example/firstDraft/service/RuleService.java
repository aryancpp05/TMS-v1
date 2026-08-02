package com.example.firstDraft.service;

import com.example.firstDraft.dto.RuleResponse;
import com.example.firstDraft.dto.RuleUpdateRequest;
import com.example.firstDraft.entity.MonitoringRule;
import com.example.firstDraft.exception.NotFoundException;
import com.example.firstDraft.repository.MonitoringRuleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RuleService {

    private final MonitoringRuleRepository ruleRepository;

    public RuleService(MonitoringRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public List<RuleResponse> getRules() {
        return ruleRepository.findAll().stream()
            .map(ApiMapper::toRuleResponse)
            .toList();
    }

    public RuleResponse updateRule(Long id, RuleUpdateRequest request) {
        MonitoringRule rule = ruleRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Rule not found: " + id));

        rule.setSeverity(request.severity());
        rule.setActive(Boolean.TRUE.equals(request.active()));
        rule.setAmountThreshold(request.amountThreshold());
        rule.setTransactionCountThreshold(request.transactionCountThreshold());
        rule.setTimeWindowMinutes(request.timeWindowMinutes());

        return ApiMapper.toRuleResponse(ruleRepository.save(rule));
    }
}

