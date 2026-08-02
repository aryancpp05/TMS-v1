package com.example.firstDraft.service;

import com.example.firstDraft.entity.Alert;
import com.example.firstDraft.entity.AlertHistory;
import com.example.firstDraft.entity.MonitoringRule;
import com.example.firstDraft.entity.TransactionRecord;
import com.example.firstDraft.model.AlertStatus;
import com.example.firstDraft.repository.AlertHistoryRepository;
import com.example.firstDraft.repository.AlertRepository;
import com.example.firstDraft.repository.MonitoringRuleRepository;
import com.example.firstDraft.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class RuleEngineService {

    private final MonitoringRuleRepository ruleRepository;
    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;
    private final AlertHistoryRepository historyRepository;

    public RuleEngineService(
        MonitoringRuleRepository ruleRepository,
        TransactionRepository transactionRepository,
        AlertRepository alertRepository,
        AlertHistoryRepository historyRepository
    ) {
        this.ruleRepository = ruleRepository;
        this.transactionRepository = transactionRepository;
        this.alertRepository = alertRepository;
        this.historyRepository = historyRepository;
    }

    public boolean evaluate(TransactionRecord transaction) {
        List<MonitoringRule> activeRules = ruleRepository.findByActiveTrue();
        boolean violated = false;

        for (MonitoringRule rule : activeRules) {
            violated = switch (rule.getType()) {
                case AMOUNT_THRESHOLD -> evaluateAmountRule(transaction, rule) || violated;
                case VELOCITY -> evaluateVelocityRule(transaction, rule) || violated;
                case NEW_PAYEE -> evaluateNewPayeeRule(transaction, rule) || violated;
                case DAILY_LIMIT -> evaluateDailyLimitRule(transaction, rule) || violated;
                default -> violated;
            };
        }

        return violated;
    }

    private boolean evaluateAmountRule(TransactionRecord transaction, MonitoringRule rule) {
        if (rule.getAmountThreshold() == null) {
            return false;
        }

        if (transaction.getAmount().compareTo(rule.getAmountThreshold()) > 0) {
            createAlert(rule, "Transaction amount exceeded threshold", List.of(transaction));
            return true;
        }

        return false;
    }

    private boolean evaluateVelocityRule(TransactionRecord transaction, MonitoringRule rule) {
        if (rule.getTransactionCountThreshold() == null || rule.getTimeWindowMinutes() == null) {
            return false;
        }

        Instant to = transaction.getTimestamp();
        Instant from = to.minusSeconds(rule.getTimeWindowMinutes() * 60L);
        long count = transactionRepository.countByAccountIdAndTimestampBetween(transaction.getAccountId(), from, to);

        if (count > rule.getTransactionCountThreshold()) {
            List<TransactionRecord> related = transactionRepository
                .findByAccountIdAndTimestampBetween(transaction.getAccountId(), from, to);
            createAlert(rule, "Velocity threshold exceeded for account " + transaction.getAccountId(), related);
            return true;
        }

        return false;
    }

    private boolean evaluateNewPayeeRule(TransactionRecord transaction, MonitoringRule rule) {
        long previousCount = transactionRepository.countByAccountIdAndPayeeIdAndTimestampBefore(
            transaction.getAccountId(),
            transaction.getPayeeId(),
            transaction.getTimestamp()
        );

        if (previousCount == 0) {
            createAlert(rule, "First transaction to new payee " + transaction.getPayeeId(), List.of(transaction));
            return true;
        }

        return false;
    }

    private boolean evaluateDailyLimitRule(TransactionRecord transaction, MonitoringRule rule) {
        if (rule.getAmountThreshold() == null) {
            return false;
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
            createAlert(rule, "Daily limit exceeded for account " + transaction.getAccountId(), List.of(transaction));
            return true;
        }

        return false;
    }

    private void createAlert(MonitoringRule rule, String message, List<TransactionRecord> transactions) {
        Alert alert = new Alert();
        alert.setRuleName(rule.getName());
        alert.setRuleType(rule.getType());
        alert.setSeverity(rule.getSeverity());
        alert.setStatus(AlertStatus.OPEN);
        alert.setMessage(message);
        alert.setCreatedAt(Instant.now());
        alert.setUpdatedAt(Instant.now());
        alert.setTriggeringTransactions(new ArrayList<>(transactions));
        Alert saved = alertRepository.save(alert);

        AlertHistory createdEvent = new AlertHistory();
        createdEvent.setAlert(saved);
        createdEvent.setFromStatus(null);
        createdEvent.setToStatus(AlertStatus.OPEN);
        createdEvent.setNote("Alert generated by rule " + rule.getName() + " (" + rule.getType() + ")");
        createdEvent.setChangedBy("system");
        createdEvent.setCreatedAt(Instant.now());
        historyRepository.save(createdEvent);
    }
}

