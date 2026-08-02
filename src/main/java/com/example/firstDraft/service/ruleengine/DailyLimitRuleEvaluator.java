package com.example.firstDraft.service.ruleengine;

import com.example.firstDraft.entity.MonitoringRule;
import com.example.firstDraft.entity.TransactionRecord;
import com.example.firstDraft.model.RuleType;
import com.example.firstDraft.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Component
public class DailyLimitRuleEvaluator implements RuleEvaluator {

    private final TransactionRepository transactionRepository;

    public DailyLimitRuleEvaluator(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public RuleType supportedType() {
        return RuleType.DAILY_LIMIT;
    }

    @Override
    public Optional<RuleEvaluationResult> evaluate(TransactionRecord transaction, MonitoringRule rule) {
        if (rule.getAmountThreshold() == null) {
            return Optional.empty();
        }

        LocalDate day = LocalDate.ofInstant(transaction.getTimestamp(), ZoneOffset.UTC);
        Instant startOfDay = day.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = day.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1);

        BigDecimal total = transactionRepository.sumAmountByAccountAndTimestampRange(
            transaction.getAccountId(),
            startOfDay,
            endOfDay
        );

        if (total.compareTo(rule.getAmountThreshold()) > 0) {
            String reason = "Daily limit exceeded for account " + transaction.getAccountId() +
                ": total=" + total + ", limit=" + rule.getAmountThreshold();
            return Optional.of(new RuleEvaluationResult(reason, List.of(transaction)));
        }

        return Optional.empty();
    }
}

