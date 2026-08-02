package com.example.firstDraft.controller;

import com.example.firstDraft.dto.TransactionRequest;
import com.example.firstDraft.dto.TransactionDecisionRequest;
import com.example.firstDraft.dto.TransactionRollbackDecisionRequest;
import com.example.firstDraft.dto.TransactionRollbackRequest;
import com.example.firstDraft.dto.TransactionResponse;
import com.example.firstDraft.model.TransactionStatus;
import com.example.firstDraft.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody TransactionRequest request) {
        return transactionService.create(request);
    }

    @GetMapping
    public List<TransactionResponse> list(
        @RequestParam(required = false) TransactionStatus status,
        @RequestParam(required = false) String accountId,
        @RequestParam(required = false) String payeeId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(required = false) BigDecimal minAmount,
        @RequestParam(required = false) BigDecimal maxAmount,
        @RequestParam(required = false) String search
    ) {
        return transactionService.list(status, accountId, payeeId, from, to, minAmount, maxAmount, search);
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable Long id) {
        return transactionService.getById(id);
    }

    @PatchMapping("/{id}/approve")
    public TransactionResponse approve(@PathVariable Long id, @Valid @RequestBody TransactionDecisionRequest request) {
        return transactionService.approve(id, request);
    }

    @PatchMapping("/{id}/reject")
    public TransactionResponse reject(@PathVariable Long id, @Valid @RequestBody TransactionDecisionRequest request) {
        return transactionService.reject(id, request);
    }

    @PatchMapping("/{id}/rollback/request")
    public TransactionResponse requestRollback(
        @PathVariable Long id,
        @Valid @RequestBody TransactionRollbackRequest request
    ) {
        return transactionService.requestRollback(id, request);
    }

    @PatchMapping("/{id}/rollback/approve")
    public TransactionResponse approveRollback(
        @PathVariable Long id,
        @Valid @RequestBody TransactionRollbackDecisionRequest request
    ) {
        return transactionService.approveRollback(id, request);
    }

    @PatchMapping("/{id}/rollback/reject")
    public TransactionResponse rejectRollback(
        @PathVariable Long id,
        @Valid @RequestBody TransactionRollbackDecisionRequest request
    ) {
        return transactionService.rejectRollback(id, request);
    }
}

