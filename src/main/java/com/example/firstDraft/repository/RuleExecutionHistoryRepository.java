package com.example.firstDraft.repository;

import com.example.firstDraft.entity.RuleExecutionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RuleExecutionHistoryRepository extends JpaRepository<RuleExecutionHistory, Long> {

    List<RuleExecutionHistory> findByTransactionId(Long transactionId);
}

