package com.example.firstDraft.service.ruleengine;

import com.example.firstDraft.entity.MonitoringRule;
import com.example.firstDraft.entity.TransactionRecord;
import com.example.firstDraft.model.RuleType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AmountThresholdRuleEvaluator implements RuleEvaluator {

    @Override
    public RuleType supportedType() {
        return RuleType.AMOUNT_THRESHOLD;
    }

    @Override
    public Optional<RuleEvaluationResult> evaluate(TransactionRecord transaction, MonitoringRule rule) {
        if (rule.getAmountThreshold() == null) {
            return Optional.empty();
        }

        if (transaction.getAmount().compareTo(rule.getAmountThreshold()) > 0) {
            String reason = "Transaction amount " + transaction.getAmount() +
                " exceeded threshold " + rule.getAmountThreshold();
            return Optional.of(new RuleEvaluationResult(reason, List.of(transaction)));
        }

        return Optional.empty();
    }
}

