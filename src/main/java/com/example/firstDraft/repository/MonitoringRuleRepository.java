package com.example.firstDraft.repository;

import com.example.firstDraft.entity.MonitoringRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonitoringRuleRepository extends JpaRepository<MonitoringRule, Long> {

    List<MonitoringRule> findByActiveTrue();
}

