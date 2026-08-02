package com.example.firstDraft.repository;

import com.example.firstDraft.entity.RuleAuditHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RuleAuditHistoryRepository extends JpaRepository<RuleAuditHistory, Long> {

    List<RuleAuditHistory> findByRuleIdOrderByChangedAtAsc(Long ruleId);
}

