package com.example.firstDraft.controller;

import com.example.firstDraft.dto.AlertHistoryResponse;
import com.example.firstDraft.dto.AlertResponse;
import com.example.firstDraft.dto.AlertStatusUpdateRequest;
import com.example.firstDraft.model.AlertSeverity;
import com.example.firstDraft.model.AlertStatus;
import com.example.firstDraft.service.AlertService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public List<AlertResponse> list(
        @RequestParam(required = false) AlertStatus status,
        @RequestParam(required = false) AlertSeverity severity,
        @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        return alertService.getAlerts(status, severity, activeOnly);
    }

    @GetMapping("/{id}")
    public AlertResponse get(@PathVariable Long id) {
        return alertService.getAlert(id);
    }

    @GetMapping("/{id}/history")
    public List<AlertHistoryResponse> history(@PathVariable Long id) {
        return alertService.getHistory(id);
    }

    @PatchMapping("/{id}/status")
    public AlertResponse updateStatus(@PathVariable Long id, @Valid @RequestBody AlertStatusUpdateRequest request) {
        return alertService.updateStatus(id, request);
    }
}

