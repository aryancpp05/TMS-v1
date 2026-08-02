package com.example.firstDraft.repository;

import com.example.firstDraft.entity.AlertHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {

    List<AlertHistory> findByAlertIdOrderByCreatedAtAsc(Long alertId);
}

