package com.example.firstDraft.service.ruleengine;

import com.example.firstDraft.entity.MonitoringRule;
import com.example.firstDraft.entity.TransactionRecord;
import com.example.firstDraft.model.RuleType;
import com.example.firstDraft.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class NewPayeeRuleEvaluator implements RuleEvaluator {

    private final TransactionRepository transactionRepository;

    public NewPayeeRuleEvaluator(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public RuleType supportedType() {
        return RuleType.NEW_PAYEE;
    }

    @Override
    public Optional<RuleEvaluationResult> evaluate(TransactionRecord transaction, MonitoringRule rule) {
        long previousCount = transactionRepository.countByAccountIdAndPayeeIdAndTimestampBefore(
            transaction.getAccountId(),
            transaction.getPayeeId(),
            transaction.getTimestamp()
        );

        if (previousCount == 0) {
            String reason = "First transaction to new payee " + transaction.getPayeeId() +
                " for account " + transaction.getAccountId();
            return Optional.of(new RuleEvaluationResult(reason, List.of(transaction)));
        }

        return Optional.empty();
    }
}

