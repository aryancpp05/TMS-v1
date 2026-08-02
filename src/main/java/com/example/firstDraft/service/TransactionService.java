package com.example.firstDraft.service;

import com.example.firstDraft.dto.TransactionRequest;
import com.example.firstDraft.dto.TransactionResponse;
import com.example.firstDraft.entity.TransactionRecord;
import com.example.firstDraft.exception.BadRequestException;
import com.example.firstDraft.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final RuleEngineService ruleEngineService;

    public TransactionService(TransactionRepository transactionRepository, RuleEngineService ruleEngineService) {
        this.transactionRepository = transactionRepository;
        this.ruleEngineService = ruleEngineService;
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        if (transactionRepository.existsByReference(request.reference())) {
            throw new BadRequestException("Transaction reference already exists: " + request.reference());
        }

        TransactionRecord record = new TransactionRecord();
        record.setReference(request.reference());
        record.setAccountId(request.accountId());
        record.setPayeeId(request.payeeId());
        record.setAmount(request.amount());
        record.setCurrency(request.currency().toUpperCase());
        record.setTimestamp(request.timestamp());
        record.setDescription(request.description());

        TransactionRecord saved = transactionRepository.save(record);
        ruleEngineService.evaluate(saved);

        return ApiMapper.toTransactionResponse(saved);
    }

    public List<TransactionResponse> list(
        String accountId,
        String payeeId,
        Instant fromTime,
        Instant toTime,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        String search
    ) {
        return transactionRepository
            .search(accountId, payeeId, fromTime, toTime, minAmount, maxAmount, search)
            .stream()
            .map(ApiMapper::toTransactionResponse)
            .toList();
    }
}

