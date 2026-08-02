package com.example.firstDraft.controller;

import com.example.firstDraft.dto.SimulationRequest;
import com.example.firstDraft.service.SimulationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/simulator")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping("/generate")
    public Map<String, Object> generate(@Valid @RequestBody SimulationRequest request) {
        return Map.of(
            "createdTransactionIds", simulationService.generate(request.count()),
            "count", request.count()
        );
    }
}

