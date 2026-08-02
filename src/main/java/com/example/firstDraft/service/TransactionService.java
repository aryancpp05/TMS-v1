package com.example.firstDraft.service;

import com.example.firstDraft.dto.TransactionRequest;
import com.example.firstDraft.dto.TransactionDecisionRequest;
import com.example.firstDraft.dto.TransactionRollbackDecisionRequest;
import com.example.firstDraft.dto.TransactionRollbackRequest;
import com.example.firstDraft.dto.TransactionResponse;
import com.example.firstDraft.entity.TransactionRecord;
import com.example.firstDraft.exception.BadRequestException;
import com.example.firstDraft.exception.NotFoundException;
import com.example.firstDraft.model.TransactionStatus;
import com.example.firstDraft.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
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

        Instant now = Instant.now();
        TransactionRecord record = new TransactionRecord();
        record.setReference(request.reference());
        record.setAccountId(request.accountId());
        record.setPayeeId(request.payeeId());
        record.setAmount(request.amount());
        record.setCurrency(request.currency().toUpperCase());
        record.setTimestamp(request.timestamp());
        record.setDescription(request.description());
        record.setStatus(TransactionStatus.COMPLETED);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);

        TransactionRecord saved = transactionRepository.save(record);
        boolean violated = ruleEngineService.evaluate(saved);
        if (violated) {
            saved.setStatus(TransactionStatus.PENDING_APPROVAL);
            saved.setReviewNote("Rule violation detected. Operator approval required.");
            saved.setUpdatedAt(Instant.now());
            saved = transactionRepository.save(saved);
        }

        return ApiMapper.toTransactionResponse(saved);
    }

    public List<TransactionResponse> list(
        TransactionStatus status,
        String accountId,
        String payeeId,
        Instant fromTime,
        Instant toTime,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        String search
    ) {
        return transactionRepository
            .search(status, accountId, payeeId, fromTime, toTime, minAmount, maxAmount, search)
            .stream()
            .map(ApiMapper::toTransactionResponse)
            .toList();
    }

    @Transactional
    public TransactionResponse approve(Long id, TransactionDecisionRequest request) {
        TransactionRecord transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));

        if (transaction.getStatus() != TransactionStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only pending transactions can be approved");
        }

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setReviewedBy(request.operatorId());
        transaction.setReviewedAt(Instant.now());
        transaction.setReviewNote(normalizeNote(request.note(), "Approved by operator"));
        transaction.setUpdatedAt(Instant.now());

        return ApiMapper.toTransactionResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse reject(Long id, TransactionDecisionRequest request) {
        TransactionRecord transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));

        if (transaction.getStatus() != TransactionStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only pending transactions can be rejected");
        }

        transaction.setStatus(TransactionStatus.REJECTED);
        transaction.setReviewedBy(request.operatorId());
        transaction.setReviewedAt(Instant.now());
        transaction.setReviewNote(normalizeNote(request.note(), "Rejected by operator"));
        transaction.setUpdatedAt(Instant.now());

        return ApiMapper.toTransactionResponse(transactionRepository.save(transaction));
    }

    public TransactionResponse getById(Long id) {
        TransactionRecord transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));
        return ApiMapper.toTransactionResponse(transaction);
    }

    @Transactional
    public TransactionResponse requestRollback(Long id, TransactionRollbackRequest request) {
        TransactionRecord transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));

        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new BadRequestException("Rollback can only be requested for completed transactions");
        }

        transaction.setStatus(TransactionStatus.ROLLBACK_REQUESTED);
        transaction.setRollbackReasonCode(request.reasonCode().trim().toUpperCase(Locale.ROOT));
        transaction.setRollbackReasonDetail(request.reasonDetail().trim());
        transaction.setRollbackRequestedBy(request.requestedBy().trim());
        transaction.setRollbackSupportingReference(normalizeOptional(request.supportingReference()));
        transaction.setRollbackRequestedAt(Instant.now());
        transaction.setUpdatedAt(Instant.now());

        return ApiMapper.toTransactionResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse approveRollback(Long id, TransactionRollbackDecisionRequest request) {
        TransactionRecord transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));

        if (transaction.getStatus() != TransactionStatus.ROLLBACK_REQUESTED) {
            throw new BadRequestException("Only rollback-requested transactions can be approved for refund");
        }

        Instant now = Instant.now();
        transaction.setRollbackReviewedBy(request.operatorId().trim());
        transaction.setRollbackReviewedAt(now);
        transaction.setRollbackReviewNote(normalizeNote(request.note(), "Rollback approved and refunded"));
        transaction.setStatus(TransactionStatus.REFUNDED);
        transaction.setRefundedAt(now);
        transaction.setUpdatedAt(now);

        TransactionRecord refund = buildRefundTransaction(transaction, now);
        TransactionRecord savedRefund = transactionRepository.save(refund);
        transaction.setRefundTransactionId(savedRefund.getId());

        return ApiMapper.toTransactionResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse rejectRollback(Long id, TransactionRollbackDecisionRequest request) {
        TransactionRecord transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));

        if (transaction.getStatus() != TransactionStatus.ROLLBACK_REQUESTED) {
            throw new BadRequestException("Only rollback-requested transactions can be rejected");
        }

        transaction.setStatus(TransactionStatus.ROLLBACK_REJECTED);
        transaction.setRollbackReviewedBy(request.operatorId().trim());
        transaction.setRollbackReviewedAt(Instant.now());
        transaction.setRollbackReviewNote(normalizeNote(request.note(), "Rollback rejected"));
        transaction.setUpdatedAt(Instant.now());

        return ApiMapper.toTransactionResponse(transactionRepository.save(transaction));
    }

    private String normalizeNote(String note, String defaultText) {
        if (note == null || note.isBlank()) {
            return defaultText;
        }
        return note;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private TransactionRecord buildRefundTransaction(TransactionRecord original, Instant now) {
        TransactionRecord refund = new TransactionRecord();
        refund.setReference("REFUND-" + original.getReference() + "-" + now.toEpochMilli());
        refund.setAccountId(original.getAccountId());
        refund.setPayeeId(original.getPayeeId());
        refund.setAmount(original.getAmount().negate());
        refund.setCurrency(original.getCurrency());
        refund.setTimestamp(now);
        refund.setDescription("Refund for " + original.getReference());
        refund.setStatus(TransactionStatus.COMPLETED);
        refund.setCreatedAt(now);
        refund.setUpdatedAt(now);
        refund.setReviewNote("System-generated refund");
        refund.setRefundedForTransactionId(original.getId());
        return refund;
    }
}

