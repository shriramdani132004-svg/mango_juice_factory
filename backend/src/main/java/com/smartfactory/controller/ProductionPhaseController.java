package com.smartfactory.controller;

import com.smartfactory.dto.*;
import com.smartfactory.service.PhaseExecutionService;
import com.smartfactory.service.ProductionEventService;
import com.smartfactory.service.SimulationEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/production-phases")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProductionPhaseController {

    private final PhaseExecutionService phaseExecutionService;
    private final SimulationEngine simulationEngine;
    private final ProductionEventService eventService;

    public ProductionPhaseController(PhaseExecutionService phaseExecutionService,
                                      SimulationEngine simulationEngine,
                                      ProductionEventService eventService) {
        this.phaseExecutionService = phaseExecutionService;
        this.simulationEngine = simulationEngine;
        this.eventService = eventService;
    }

    @PostMapping("/{phaseId}/start")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PhaseExecutionDto> startPhase(@PathVariable Long phaseId) {
        return ResponseEntity.ok(phaseExecutionService.startPhase(phaseId));
    }

    @PostMapping("/{phaseId}/pause")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> pausePhase(@PathVariable Long phaseId) {
        phaseExecutionService.pausePhase(phaseId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{phaseId}/resume")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resumePhase(@PathVariable Long phaseId) {
        phaseExecutionService.resumePhase(phaseId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{phaseId}/stop")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> stopPhase(@PathVariable Long phaseId) {
        phaseExecutionService.stopPhase(phaseId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{phaseId}/completion")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_WORKER')")
    public ResponseEntity<PhaseCompletionDto> getCompletion(@PathVariable Long phaseId) {
        return ResponseEntity.ok(phaseExecutionService.getPhaseCompletion(phaseId));
    }

    @PostMapping("/{phaseId}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PhaseVerifyResponse> verifyPhase(@PathVariable Long phaseId) {
        return ResponseEntity.ok(phaseExecutionService.verifyPhase(phaseId));
    }

    @GetMapping("/{phaseId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_WORKER')")
    public ResponseEntity<Map<String, Object>> getPhaseStatus(@PathVariable Long phaseId) {
        boolean executing = simulationEngine.isPhaseExecuting(phaseId);
        SimulationEngine.PhaseExecutionState state = simulationEngine.getExecutionState(phaseId);

        if (executing && state != null) {
            return ResponseEntity.ok(Map.of(
                "executing", true,
                "status", state.status,
                "progress", state.progress,
                "processedQuantity", state.processedQuantity,
                "elapsedSeconds", state.elapsedSeconds
            ));
        }
        return ResponseEntity.ok(Map.of("executing", false));
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<Long, Object>> getActiveExecutions() {
        Map<Long, Object> active = simulationEngine.getAllActiveExecutions().entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                e -> Map.of(
                    "phaseId", e.getValue().phaseId,
                    "status", e.getValue().status,
                    "progress", e.getValue().progress,
                    "phaseNumber", e.getValue().phaseNumber
                )
            ));
        return ResponseEntity.ok(active);
    }

    @GetMapping("/events/{orderId}")
    public SseEmitter streamEvents(@PathVariable Long orderId) {
        return eventService.subscribeToOrder(orderId);
    }

    @GetMapping("/events/global")
    public SseEmitter streamGlobalEvents() {
        return eventService.subscribeGlobal();
    }
}
