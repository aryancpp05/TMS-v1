package com.example.firstDraft.controller;

import com.example.firstDraft.model.AlertStatus;
import com.example.firstDraft.repository.AlertRepository;
import com.example.firstDraft.repository.TransactionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Set<AlertStatus> ACTIVE_STATUSES = Set.of(
        AlertStatus.OPEN,
        AlertStatus.ACKNOWLEDGED,
        AlertStatus.INVESTIGATING
    );

    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;

    public DashboardController(TransactionRepository transactionRepository, AlertRepository alertRepository) {
        this.transactionRepository = transactionRepository;
        this.alertRepository = alertRepository;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        long totalTransactions = transactionRepository.count();
        long totalAlerts = alertRepository.count();
        long activeAlerts = alertRepository.search(null, null, true, ACTIVE_STATUSES).size();
        long openAlerts = alertRepository.search(AlertStatus.OPEN, null, false, ACTIVE_STATUSES).size();

        return Map.of(
            "totalTransactions", totalTransactions,
            "totalAlerts", totalAlerts,
            "activeAlerts", activeAlerts,
            "openAlerts", openAlerts
        );
    }
}

