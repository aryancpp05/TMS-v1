package com.example.firstDraft.controller;

import com.example.firstDraft.dto.RuleCreateRequest;
import com.example.firstDraft.dto.RuleAuditHistoryResponse;
import com.example.firstDraft.dto.RuleResponse;
import com.example.firstDraft.dto.RuleStatsResponse;
import com.example.firstDraft.dto.RuleStatusUpdateRequest;
import com.example.firstDraft.dto.RuleUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.firstDraft.model.AlertSeverity;
import com.example.firstDraft.model.RuleType;
import com.example.firstDraft.service.RuleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
@Tag(name = "Rules Management", description = "APIs for creating, viewing, updating, enabling/disabling, deleting, and reporting monitoring rules.")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    @Operation(summary = "View monitoring rules", description = "Returns rules with optional filtering by active flag, type, and severity.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rules retrieved"),
        @ApiResponse(responseCode = "400", description = "Invalid filter value", content = @Content(schema = @Schema(implementation = Object.class)))
    })
    public List<RuleResponse> list(
        @Parameter(description = "Filter by active status")
        @RequestParam(required = false) Boolean active,
        @Parameter(description = "Filter by rule type")
        @RequestParam(required = false) RuleType type,
        @Parameter(description = "Filter by rule severity")
        @RequestParam(required = false) AlertSeverity severity
    ) {
        return ruleService.getRules(active, type, severity);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create monitoring rule", description = "Creates a new monitoring rule with type-specific validation rules.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Rule created"),
        @ApiResponse(responseCode = "400", description = "Invalid rule configuration"),
        @ApiResponse(responseCode = "409", description = "Duplicate rule name")
    })
    public RuleResponse create(@Valid @RequestBody RuleCreateRequest request) {
        return ruleService.createRule(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update monitoring rule", description = "Updates rule configuration including name, type, severity, and thresholds.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rule updated"),
        @ApiResponse(responseCode = "400", description = "Invalid rule configuration"),
        @ApiResponse(responseCode = "404", description = "Rule not found"),
        @ApiResponse(responseCode = "409", description = "Duplicate rule name")
    })
    public RuleResponse update(@PathVariable Long id, @Valid @RequestBody RuleUpdateRequest request) {
        return ruleService.updateRule(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Enable/disable monitoring rule", description = "Changes only the active status of a rule.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rule status updated"),
        @ApiResponse(responseCode = "400", description = "Invalid status operation"),
        @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    public RuleResponse updateStatus(@PathVariable Long id, @Valid @RequestBody RuleStatusUpdateRequest request) {
        return ruleService.updateRuleStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete monitoring rule", description = "Marks a rule as inactive (soft delete) without removing it from database.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rule soft deleted"),
        @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    public RuleResponse delete(@PathVariable Long id) {
        return ruleService.softDeleteRule(id);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "View rule audit history", description = "Returns chronological history of changes for a specific rule.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rule history retrieved"),
        @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    public List<RuleAuditHistoryResponse> history(@PathVariable Long id) {
        return ruleService.getRuleHistory(id);
    }

    @GetMapping("/stats")
    @Operation(summary = "View rule statistics", description = "Returns totals and grouped counts by rule type and severity.")
    @ApiResponse(responseCode = "200", description = "Rule stats retrieved", content = @Content(
        mediaType = "application/json",
        examples = @ExampleObject(value = "{\"totalRules\":10,\"activeRules\":8,\"inactiveRules\":2,\"rulesByType\":{\"AMOUNT_THRESHOLD\":3,\"VELOCITY\":2,\"NEW_PAYEE\":3,\"DAILY_LIMIT\":2},\"rulesBySeverity\":{\"HIGH\":4,\"MEDIUM\":4,\"LOW\":2}}")
    ))
    public RuleStatsResponse stats() {
        return ruleService.getRuleStats();
    }
}

