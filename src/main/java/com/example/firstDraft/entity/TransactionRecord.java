package com.example.firstDraft.entity;

import com.example.firstDraft.model.TransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions")
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String payeeId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private String reviewedBy;

    private Instant reviewedAt;

    @Column(length = 1000)
    private String reviewNote;

    private String rollbackReasonCode;

    @Column(length = 1000)
    private String rollbackReasonDetail;

    private String rollbackRequestedBy;

    private Instant rollbackRequestedAt;

    private String rollbackSupportingReference;

    private String rollbackReviewedBy;

    private Instant rollbackReviewedAt;

    @Column(length = 1000)
    private String rollbackReviewNote;

    private Instant refundedAt;

    private Long refundTransactionId;

    private Long refundedForTransactionId;

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getPayeeId() {
        return payeeId;
    }

    public void setPayeeId(String payeeId) {
        this.payeeId = payeeId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public String getRollbackReasonCode() {
        return rollbackReasonCode;
    }

    public void setRollbackReasonCode(String rollbackReasonCode) {
        this.rollbackReasonCode = rollbackReasonCode;
    }

    public String getRollbackReasonDetail() {
        return rollbackReasonDetail;
    }

    public void setRollbackReasonDetail(String rollbackReasonDetail) {
        this.rollbackReasonDetail = rollbackReasonDetail;
    }

    public String getRollbackRequestedBy() {
        return rollbackRequestedBy;
    }

    public void setRollbackRequestedBy(String rollbackRequestedBy) {
        this.rollbackRequestedBy = rollbackRequestedBy;
    }

    public Instant getRollbackRequestedAt() {
        return rollbackRequestedAt;
    }

    public void setRollbackRequestedAt(Instant rollbackRequestedAt) {
        this.rollbackRequestedAt = rollbackRequestedAt;
    }

    public String getRollbackSupportingReference() {
        return rollbackSupportingReference;
    }

    public void setRollbackSupportingReference(String rollbackSupportingReference) {
        this.rollbackSupportingReference = rollbackSupportingReference;
    }

    public String getRollbackReviewedBy() {
        return rollbackReviewedBy;
    }

    public void setRollbackReviewedBy(String rollbackReviewedBy) {
        this.rollbackReviewedBy = rollbackReviewedBy;
    }

    public Instant getRollbackReviewedAt() {
        return rollbackReviewedAt;
    }

    public void setRollbackReviewedAt(Instant rollbackReviewedAt) {
        this.rollbackReviewedAt = rollbackReviewedAt;
    }

    public String getRollbackReviewNote() {
        return rollbackReviewNote;
    }

    public void setRollbackReviewNote(String rollbackReviewNote) {
        this.rollbackReviewNote = rollbackReviewNote;
    }

    public Instant getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(Instant refundedAt) {
        this.refundedAt = refundedAt;
    }

    public Long getRefundTransactionId() {
        return refundTransactionId;
    }

    public void setRefundTransactionId(Long refundTransactionId) {
        this.refundTransactionId = refundTransactionId;
    }

    public Long getRefundedForTransactionId() {
        return refundedForTransactionId;
    }

    public void setRefundedForTransactionId(Long refundedForTransactionId) {
        this.refundedForTransactionId = refundedForTransactionId;
    }
}

