package com.smartfactory.controller;

import com.smartfactory.dto.*;
import com.smartfactory.service.QualityCheckService;
import com.smartfactory.service.SimulationEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quality-checks")
@CrossOrigin(origins = "*", maxAge = 3600)
public class QualityCheckController {

    private final QualityCheckService qualityCheckService;

    public QualityCheckController(QualityCheckService qualityCheckService) {
        this.qualityCheckService = qualityCheckService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_WORKER')")
    public ResponseEntity<QualityCheckDto> createQualityCheck(@RequestBody QualityCheckCreateRequest request) {
        List<QualityCheckDto> created = qualityCheckService.createQualityChecksForPhase(request.batchId(), request.phaseId());
        if (!created.isEmpty()) {
            return ResponseEntity.ok(created.get(0));
        }
        return ResponseEntity.ok(null);
    }

    @PostMapping("/batch/{batchId}/phase/{phaseId}/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_WORKER')")
    public ResponseEntity<List<QualityCheckDto>> createChecksForPhase(
            @PathVariable Long batchId, @PathVariable Long phaseId) {
        return ResponseEntity.ok(qualityCheckService.createQualityChecksForPhase(batchId, phaseId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_WORKER', 'PRODUCTION_WORKER')")
    public ResponseEntity<QualityCheckDto> getQualityCheck(@PathVariable Long id) {
        return ResponseEntity.ok(qualityCheckService.getQualityCheckById(id));
    }

    @PostMapping("/{id}/inspect")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_WORKER')")
    public ResponseEntity<QualityCheckDto> inspectQualityCheck(
            @PathVariable Long id,
            @RequestBody QualityCheckInspectionRequest request) {
        return ResponseEntity.ok(qualityCheckService.inspectQualityCheck(
            id, request.observedValue(), request.result(), request.notes(), null));
    }

    @PostMapping("/{id}/auto-evaluate")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_WORKER')")
    public ResponseEntity<QualityCheckDto> autoEvaluateQualityCheck(@PathVariable Long id) {
        return ResponseEntity.ok(qualityCheckService.autoEvaluateQualityCheck(id));
    }

    @GetMapping("/batch/{batchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_WORKER', 'PRODUCTION_WORKER')")
    public ResponseEntity<List<QualityCheckDto>> getChecksByBatch(@PathVariable Long batchId) {
        return ResponseEntity.ok(qualityCheckService.getQualityChecksByBatch(batchId));
    }

    @GetMapping("/phase/{phaseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_WORKER', 'PRODUCTION_WORKER')")
    public ResponseEntity<List<QualityCheckDto>> getChecksByPhase(@PathVariable Long phaseId) {
        return ResponseEntity.ok(qualityCheckService.getQualityChecksByPhase(phaseId));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_WORKER', 'PRODUCTION_WORKER')")
    public ResponseEntity<List<QualityCheckDto>> getChecksByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(qualityCheckService.getQualityChecksByOrder(orderId));
    }

    @GetMapping("/batch/{batchId}/phase/{phaseId}/result")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_WORKER', 'PRODUCTION_WORKER')")
    public ResponseEntity<QualityPhaseResult> getPhaseQualityResult(
            @PathVariable Long batchId, @PathVariable Long phaseId) {
        return ResponseEntity.ok(qualityCheckService.getPhaseQualityResult(batchId, phaseId));
    }

    @GetMapping("/batch/{batchId}/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_WORKER', 'PRODUCTION_WORKER')")
    public ResponseEntity<BatchQualitySummary> getBatchQualitySummary(@PathVariable Long batchId) {
        return ResponseEntity.ok(qualityCheckService.getBatchQualitySummary(batchId));
    }

    @PostMapping("/batch/{batchId}/quarantine")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QualityActionResponse> quarantineBatch(
            @PathVariable Long batchId, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(qualityCheckService.quarantineBatch(batchId, body.getOrDefault("reason", "Quality hold")));
    }

    @PostMapping("/batch/{batchId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QualityActionResponse> rejectBatch(
            @PathVariable Long batchId, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(qualityCheckService.rejectBatch(batchId, body.getOrDefault("reason", "Quality failure")));
    }

    @PostMapping("/batch/{batchId}/reprocess")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QualityActionResponse> reprocessBatch(
            @PathVariable Long batchId, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(qualityCheckService.reprocessBatch(batchId, body.getOrDefault("reason", "Reprocessing requested")));
    }

    @PostMapping("/batch/{batchId}/phase/{phaseId}/update-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QualityPhaseResult> updatePhaseQualityStatus(
            @PathVariable Long batchId, @PathVariable Long phaseId) {
        return ResponseEntity.ok(qualityCheckService.updatePhaseQualityStatus(batchId, phaseId));
    }
}
