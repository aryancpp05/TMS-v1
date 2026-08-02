package com.example.firstDraft.service.ruleengine;

import com.example.firstDraft.entity.MonitoringRule;
import com.example.firstDraft.entity.TransactionRecord;
import com.example.firstDraft.model.RuleType;
import com.example.firstDraft.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class VelocityRuleEvaluator implements RuleEvaluator {

    private final TransactionRepository transactionRepository;

    public VelocityRuleEvaluator(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public RuleType supportedType() {
        return RuleType.VELOCITY;
    }

    @Override
    public Optional<RuleEvaluationResult> evaluate(TransactionRecord transaction, MonitoringRule rule) {
        if (rule.getTransactionCountThreshold() == null || rule.getTimeWindowMinutes() == null) {
            return Optional.empty();
        }

        Instant to = transaction.getTimestamp();
        Instant from = to.minusSeconds(rule.getTimeWindowMinutes() * 60L);
        long count = transactionRepository.countByAccountIdAndTimestampBetween(transaction.getAccountId(), from, to);

        if (count > rule.getTransactionCountThreshold()) {
            List<TransactionRecord> related = transactionRepository
                .findByAccountIdAndTimestampBetween(transaction.getAccountId(), from, to);
            String reason = "Velocity threshold exceeded for account " + transaction.getAccountId() +
                ": count=" + count + ", threshold=" + rule.getTransactionCountThreshold() +
                ", windowMinutes=" + rule.getTimeWindowMinutes();
            return Optional.of(new RuleEvaluationResult(reason, related));
        }

        return Optional.empty();
    }
}

