package com.example.firstDraft.service.ruleengine;

import com.example.firstDraft.entity.MonitoringRule;
import com.example.firstDraft.entity.TransactionRecord;
import com.example.firstDraft.model.RuleType;

import java.util.Optional;

public interface RuleEvaluator {

    RuleType supportedType();

    Optional<RuleEvaluationResult> evaluate(TransactionRecord transaction, MonitoringRule rule);
}

