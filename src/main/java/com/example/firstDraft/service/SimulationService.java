package com.example.firstDraft.service;

import com.example.firstDraft.dto.TransactionRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class SimulationService {

    private static final List<String> ACCOUNTS = List.of("ACC-001", "ACC-002", "ACC-003");
    private static final List<String> PAYEES = List.of("PAYEE-A", "PAYEE-B", "PAYEE-C", "PAYEE-NEW");

    private final TransactionService transactionService;
    private final Random random = new Random();

    public SimulationService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public List<Long> generate(int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TransactionRequest request = new TransactionRequest(
                "SIM-" + UUID.randomUUID(),
                randomChoice(ACCOUNTS),
                randomChoice(PAYEES),
                randomAmount(),
                "USD",
                Instant.now(),
                "Simulated transaction " + (i + 1)
            );
            ids.add(transactionService.create(request).id());
        }
        return ids;
    }

    private String randomChoice(List<String> values) {
        return values.get(random.nextInt(values.size()));
    }

    private BigDecimal randomAmount() {
        BigDecimal value = BigDecimal.valueOf(10 + (20000 - 10) * random.nextDouble());
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}

