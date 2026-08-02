package com.example.firstDraft.repository;

import com.example.firstDraft.entity.Alert;
import com.example.firstDraft.model.AlertSeverity;
import com.example.firstDraft.model.AlertStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    @EntityGraph(attributePaths = {"triggeringTransactions"})
    Optional<Alert> findWithTriggeringTransactionsById(Long id);

    @Query("""
        select a from Alert a
        where (:status is null or a.status = :status)
          and (:severity is null or a.severity = :severity)
          and (:activeOnly = false or a.status in :activeStatuses)
        order by a.createdAt desc
        """)
    List<Alert> search(
        @Param("status") AlertStatus status,
        @Param("severity") AlertSeverity severity,
        @Param("activeOnly") boolean activeOnly,
        @Param("activeStatuses") Set<AlertStatus> activeStatuses
    );

    @EntityGraph(attributePaths = {"triggeringTransactions"})
    @Query("""
        select distinct a from Alert a
        join a.triggeringTransactions t
        where t.id = :transactionId
          and a.status in :activeStatuses
        order by a.createdAt desc
        """)
    List<Alert> findActiveByTriggeringTransactionId(
        @Param("transactionId") Long transactionId,
        @Param("activeStatuses") Set<AlertStatus> activeStatuses
    );
}

