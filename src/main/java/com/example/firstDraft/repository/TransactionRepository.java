package com.example.firstDraft.repository;

import com.example.firstDraft.entity.TransactionRecord;
import com.example.firstDraft.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {

    boolean existsByReference(String reference);

    long countByAccountIdAndPayeeIdAndTimestampBefore(String accountId, String payeeId, Instant timestamp);

    long countByAccountIdAndTimestampBetween(String accountId, Instant from, Instant to);

    List<TransactionRecord> findByAccountIdAndTimestampBetween(String accountId, Instant from, Instant to);

    @Query("""
        select coalesce(sum(t.amount), 0)
        from TransactionRecord t
        where t.accountId = :accountId
          and t.timestamp between :from and :to
        """)
    BigDecimal sumAmountByAccountAndTimestampRange(
        @Param("accountId") String accountId,
        @Param("from") Instant from,
        @Param("to") Instant to
    );

    @Query("""
        select t from TransactionRecord t
        where (:status is null or t.status = :status)
          and (:accountId is null or t.accountId = :accountId)
          and (:payeeId is null or t.payeeId = :payeeId)
          and (:fromTime is null or t.timestamp >= :fromTime)
          and (:toTime is null or t.timestamp <= :toTime)
          and (:minAmount is null or t.amount >= :minAmount)
          and (:maxAmount is null or t.amount <= :maxAmount)
          and (:searchText is null or lower(t.reference) like lower(concat('%', :searchText, '%'))
               or lower(t.description) like lower(concat('%', :searchText, '%')))
        order by t.timestamp desc
        """)
    List<TransactionRecord> search(
        @Param("status") TransactionStatus status,
        @Param("accountId") String accountId,
        @Param("payeeId") String payeeId,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime,
        @Param("minAmount") BigDecimal minAmount,
        @Param("maxAmount") BigDecimal maxAmount,
        @Param("searchText") String searchText
    );
}

