package com.example.firstDraft.repository;

import com.example.firstDraft.entity.MonitoringRule;
import com.example.firstDraft.model.AlertSeverity;
import com.example.firstDraft.model.RuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MonitoringRuleRepository extends JpaRepository<MonitoringRule, Long> {

    List<MonitoringRule> findByActiveTrue();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    long countByActiveTrue();

    long countByActiveFalse();

    @Query("""
        select r from MonitoringRule r
        where (:active is null or r.active = :active)
          and (:type is null or r.type = :type)
          and (:severity is null or r.severity = :severity)
        order by r.id asc
        """)
    List<MonitoringRule> search(
        @Param("active") Boolean active,
        @Param("type") RuleType type,
        @Param("severity") AlertSeverity severity
    );

    @Query("""
        select r.type, count(r)
        from MonitoringRule r
        group by r.type
        """)
    List<Object[]> countGroupedByType();

    @Query("""
        select r.severity, count(r)
        from MonitoringRule r
        group by r.severity
        """)
    List<Object[]> countGroupedBySeverity();
}

