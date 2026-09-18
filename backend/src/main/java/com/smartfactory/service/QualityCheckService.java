package com.smartfactory.service;

import com.smartfactory.dto.*;
import com.smartfactory.entity.*;
import com.smartfactory.enums.*;
import com.smartfactory.exception.ResourceNotFoundException;
import com.smartfactory.exception.ValidationException;
import com.smartfactory.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class QualityCheckService {

    private static final Logger log = LoggerFactory.getLogger(QualityCheckService.class);

    private static final Map<Integer, Map<String, Object[]>> PHASE_QUALITY_RULES = new HashMap<>();

    static {
        PHASE_QUALITY_RULES.put(1, Map.of(
            "FRUIT_CONDITION", new Object[]{new BigDecimal("0"), new BigDecimal("100"), "Quality Score", "%", true},
            "INCOMING_QUANTITY", new Object[]{BigDecimal.ZERO, new BigDecimal("999999"), "Accepted Quantity", "kg", true}
        ));
        PHASE_QUALITY_RULES.put(2, Map.of(
            "SORTING_RESULT", new Object[]{new BigDecimal("90"), new BigDecimal("100"), "Sorting Efficiency", "%", true},
            "CLEANLINESS", new Object[]{new BigDecimal("95"), new BigDecimal("100"), "Cleanliness Score", "%", true}
        ));
        PHASE_QUALITY_RULES.put(3, Map.of(
            "YIELD", new Object[]{new BigDecimal("70"), new BigDecimal("100"), "Pulp Yield", "%", true},
            "PULP_CONDITION", new Object[]{new BigDecimal("0"), new BigDecimal("100"), "Pulp Quality Score", "%", true}
        ));
        PHASE_QUALITY_RULES.put(4, Map.of(
            "YIELD", new Object[]{new BigDecimal("90"), new BigDecimal("100"), "Filter Yield", "%", true},
            "PARTICLE_QUALITY", new Object[]{new BigDecimal("0"), new BigDecimal("10"), "Particle Count", "ppm", true}
        ));
        PHASE_QUALITY_RULES.put(5, Map.of(
            "RECIPE_COMPLIANCE", new Object[]{new BigDecimal("95"), new BigDecimal("100"), "Recipe Compliance", "%", true},
            "BATCH_QUANTITY", new Object[]{BigDecimal.ZERO, new BigDecimal("999999"), "Batch Quantity", "kg", true}
        ));
        PHASE_QUALITY_RULES.put(6, Map.of(
            "TEMPERATURE", new Object[]{new BigDecimal("72"), new BigDecimal("85"), "Pasteurization Temperature", "\u00B0C", true},
            "PROCESSING_TIME", new Object[]{new BigDecimal("15"), new BigDecimal("30"), "Processing Time", "min", true}
        ));
        PHASE_QUALITY_RULES.put(7, Map.of(
            "BATCH_QUANTITY", new Object[]{new BigDecimal("0"), new BigDecimal("100"), "Quality Score", "%", true}
        ));
        PHASE_QUALITY_RULES.put(8, Map.of(
            "FILL_QUANTITY", new Object[]{new BigDecimal("495"), new BigDecimal("505"), "Fill Volume", "mL", true},
            "CAP_STATUS", new Object[]{new BigDecimal("0"), new BigDecimal("100"), "Cap Seal Score", "%", true}
        ));
        PHASE_QUALITY_RULES.put(9, Map.of(
            "LABEL_STATUS", new Object[]{new BigDecimal("0"), new BigDecimal("100"), "Label Quality", "%", true},
            "PACKAGE_INTEGRITY", new Object[]{new BigDecimal("95"), new BigDecimal("100"), "Package Integrity", "%", true}
        ));
        PHASE_QUALITY_RULES.put(10, Map.of(
            "FINAL_QUANTITY", new Object[]{BigDecimal.ZERO, new BigDecimal("999999"), "Final Quantity", "kg", true},
            "FINISHED_GOODS_STATE", new Object[]{new BigDecimal("0"), new BigDecimal("100"), "Final Inspection Score", "%", true}
        ));
    }

    private final QualityCheckRepository qualityCheckRepository;
    private final BatchRepository batchRepository;
    private final ProductionOrderRepository orderRepository;
    private final ProductionPhaseRepository phaseRepository;
    private final MachineRepository machineRepository;
    private final QualityRuleEngine ruleEngine;
    private final AuditService auditService;
    private final InventoryService inventoryService;
    private final ProductionEventService eventService;

    public QualityCheckService(QualityCheckRepository qualityCheckRepository,
                               BatchRepository batchRepository,
                               ProductionOrderRepository orderRepository,
                               ProductionPhaseRepository phaseRepository,
                               MachineRepository machineRepository,
                               QualityRuleEngine ruleEngine,
                               AuditService auditService,
                               InventoryService inventoryService,
                               ProductionEventService eventService) {
        this.qualityCheckRepository = qualityCheckRepository;
        this.batchRepository = batchRepository;
        this.orderRepository = orderRepository;
        this.phaseRepository = phaseRepository;
        this.machineRepository = machineRepository;
        this.ruleEngine = ruleEngine;
        this.auditService = auditService;
        this.inventoryService = inventoryService;
        this.eventService = eventService;
    }

    public List<QualityCheckDto> createQualityChecksForPhase(Long batchId, Long phaseId) {
        Batch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));
        ProductionPhase phase = phaseRepository.findById(phaseId)
            .orElseThrow(() -> new ResourceNotFoundException("Phase not found: " + phaseId));
        ProductionOrder order = batch.getOrder();

        Map<String, Object[]> rules = PHASE_QUALITY_RULES.get(phase.getPhaseNumber());
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }

        List<QualityCheck> checks = new ArrayList<>();
        for (Map.Entry<String, Object[]> entry : rules.entrySet()) {
            QualityCheckType checkType = QualityCheckType.valueOf(entry.getKey());
            Object[] params = entry.getValue();
            QualityCheck qc = new QualityCheck(
                batch, order, phase, checkType, (String) params[2],
                (BigDecimal) params[0], (BigDecimal) params[1], (Boolean) params[4]
            );
            qc.setUnit((String) params[3]);
            qc.setStatus(QualityCheckStatus.PENDING);
            qualityCheckRepository.save(qc);
            checks.add(qc);
        }

        auditService.log(null, "QUALITY_CHECKS_CREATED", "BATCH", batchId);
        eventService.publishEvent(new ProductionEventDto(
            "QUALITY_CHECK_CREATED", order.getId(), order.getOrderNumber(),
            batch.getId(), batch.getBatchCode(),
            phaseId, phase.getPhaseNumber(), phase.getPhaseType(),
            null, null,
            "Quality checks created for phase " + phase.getPhaseNumber(),
            Map.of("checkCount", checks.size())
        ));

        log.info("Created {} quality checks for batch {} phase {}", checks.size(), batch.getBatchCode(), phase.getPhaseNumber());
        return checks.stream().map(this::toDto).toList();
    }

    public QualityCheckDto inspectQualityCheck(Long checkId, BigDecimal observedValue, QualityResult result, String notes, Long userId) {
        QualityCheck qc = qualityCheckRepository.findById(checkId)
            .orElseThrow(() -> new ResourceNotFoundException("Quality check not found: " + checkId));

        if (qc.getStatus() == QualityCheckStatus.VERIFIED || qc.getStatus() == QualityCheckStatus.REJECTED) {
            throw new ValidationException("Quality check already finalized: " + qc.getStatus());
        }

        qc.setObservedValue(observedValue);

        if (result == null && qc.getExpectedMin() != null && qc.getExpectedMax() != null) {
            result = ruleEngine.evaluate(observedValue, qc.getExpectedMin(), qc.getExpectedMax());
        }
        qc.setResult(result);

        if (notes != null && !notes.isBlank()) {
            qc.setNotes(notes);
        }

        if (result == QualityResult.PASS) {
            qc.setStatus(QualityCheckStatus.PASS);
        } else if (result == QualityResult.FAIL) {
            qc.setStatus(QualityCheckStatus.FAIL);
        } else {
            qc.setStatus(QualityCheckStatus.REVIEW);
        }
        qc.setCompletedAt(LocalDateTime.now());

        qualityCheckRepository.save(qc);

        Batch batch = qc.getBatch();
        ProductionPhase phase = qc.getPhase();
        ProductionOrder order = qc.getProductionOrder();

        String eventType = result == QualityResult.PASS ? "QUALITY_CHECK_PASSED" : "QUALITY_CHECK_FAILED";
        eventService.publishEvent(new ProductionEventDto(
            eventType, order.getId(), order.getOrderNumber(),
            batch.getId(), batch.getBatchCode(),
            phase.getId(), phase.getPhaseNumber(), phase.getPhaseType(),
            qc.getMachine() != null ? qc.getMachine().getId() : null,
            qc.getMachine() != null ? qc.getMachine().getMachineCode() : null,
            qc.getParameterName() + ": " + result,
            Map.of("checkId", qc.getId(), "observedValue", observedValue != null ? observedValue : "null", "result", result)
        ));

        log.info("Quality check {} inspected: {} = {} -> {}", checkId, qc.getParameterName(), observedValue, result);
        return toDto(qc);
    }

    public QualityCheckDto autoEvaluateQualityCheck(Long checkId) {
        QualityCheck qc = qualityCheckRepository.findById(checkId)
            .orElseThrow(() -> new ResourceNotFoundException("Quality check not found: " + checkId));

        if (qc.getObservedValue() == null) {
            throw new ValidationException("No observed value to evaluate. Set observed value first.");
        }

        QualityResult result = ruleEngine.evaluate(qc.getObservedValue(), qc.getExpectedMin(), qc.getExpectedMax());
        return inspectQualityCheck(checkId, qc.getObservedValue(), result, "Auto-evaluated by rule engine", null);
    }

    public QualityPhaseResult getPhaseQualityResult(Long batchId, Long phaseId) {
        List<QualityCheck> checks = qualityCheckRepository.findByBatchIdAndPhaseId(batchId, phaseId);
        List<QualityCheckDto> dtos = checks.stream().map(this::toDto).toList();

        long passed = checks.stream().filter(c -> c.getResult() == QualityResult.PASS).count();
        long failed = checks.stream().filter(c -> c.getResult() == QualityResult.FAIL).count();
        long pending = checks.stream().filter(c -> c.getStatus() == QualityCheckStatus.PENDING || c.getStatus() == QualityCheckStatus.IN_PROGRESS).count();

        QualityCheckStatus overallStatus;
        QualityResult overallResult;
        String message;

        if (failed > 0) {
            overallStatus = QualityCheckStatus.FAIL;
            overallResult = QualityResult.FAIL;
            message = failed + " mandatory check(s) failed";
        } else if (pending > 0) {
            overallStatus = QualityCheckStatus.PENDING;
            overallResult = null;
            message = pending + " checks still pending";
        } else if (passed > 0 && checks.size() == passed) {
            overallStatus = QualityCheckStatus.PASS;
            overallResult = QualityResult.PASS;
            message = "All " + passed + " checks passed";
        } else {
            overallStatus = QualityCheckStatus.PENDING;
            overallResult = null;
            message = "No quality checks recorded";
        }

        ProductionPhase phase = phaseRepository.findById(phaseId).orElse(null);
        Batch batch = batchRepository.findById(batchId).orElse(null);

        return new QualityPhaseResult(
            phaseId,
            phase != null ? phase.getPhaseNumber() : null,
            batchId,
            batch != null ? batch.getBatchCode() : null,
            overallStatus,
            overallResult,
            checks.size(),
            passed,
            failed,
            pending,
            dtos,
            message
        );
    }

    public BatchQualitySummary getBatchQualitySummary(Long batchId) {
        Batch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));
        ProductionOrder order = batch.getOrder();

        List<ProductionPhase> phases = phaseRepository.findByBatchIdOrderByPhaseNumberAsc(batchId);
        List<QualityPhaseResult> phaseResults = new ArrayList<>();
        List<QualityCheckDto> allFailed = new ArrayList<>();
        long totalChecks = 0, passedChecks = 0, failedChecks = 0;

        for (ProductionPhase phase : phases) {
            QualityPhaseResult pqr = getPhaseQualityResult(batchId, phase.getId());
            phaseResults.add(pqr);
            totalChecks += pqr.totalChecks();
            passedChecks += pqr.passedChecks();
            failedChecks += pqr.failedChecks();
            pqr.checks().stream()
                .filter(c -> c.result() == QualityResult.FAIL)
                .forEach(allFailed::add);
        }

        QualityCheckStatus overallStatus;
        QualityResult overallResult;
        if (failedChecks > 0) {
            overallStatus = QualityCheckStatus.FAIL;
            overallResult = QualityResult.FAIL;
        } else if (totalChecks > 0 && passedChecks == totalChecks) {
            overallStatus = QualityCheckStatus.PASS;
            overallResult = QualityResult.PASS;
        } else {
            overallStatus = QualityCheckStatus.PENDING;
            overallResult = null;
        }

        return new BatchQualitySummary(
            batchId, batch.getBatchCode(),
            order.getId(), order.getOrderNumber(),
            overallStatus, overallResult,
            phaseResults, allFailed,
            totalChecks, passedChecks, failedChecks,
            failedChecks > 0 ? failedChecks + " check(s) failed" : "All checks passed or pending"
        );
    }

    public QualityActionResponse quarantineBatch(Long batchId, String reason) {
        Batch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));

        batch.setStatus("QUARANTINED");
        batchRepository.save(batch);

        List<QualityCheck> pendingChecks = qualityCheckRepository.findByBatchIdOrderByCreatedAtDesc(batchId).stream()
            .filter(c -> c.getStatus() == QualityCheckStatus.PENDING || c.getStatus() == QualityCheckStatus.IN_PROGRESS)
            .toList();
        for (QualityCheck qc : pendingChecks) {
            qc.setStatus(QualityCheckStatus.QUARANTINED);
            qualityCheckRepository.save(qc);
        }

        ProductionOrder order = batch.getOrder();
        eventService.publishEvent(new ProductionEventDto(
            "BATCH_QUARANTINED", order.getId(), order.getOrderNumber(),
            batch.getId(), batch.getBatchCode(),
            null, null, null,
            null, null,
            "Batch " + batch.getBatchCode() + " quarantined: " + reason,
            Map.of("reason", reason)
        ));

        auditService.log(null, "BATCH_QUARANTINED", "BATCH", batchId);

        log.warn("Batch {} quarantined: {}", batch.getBatchCode(), reason);
        return new QualityActionResponse(batchId, batch.getBatchCode(), QualityCheckStatus.QUARANTINED, "QUARANTINE", "Batch quarantined: " + reason);
    }

    public QualityActionResponse rejectBatch(Long batchId, String reason) {
        Batch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));

        batch.setStatus("REJECTED");
        batch.setRejectedQuantity(batch.getOutputQuantity());
        batchRepository.save(batch);

        ProductionOrder order = batch.getOrder();
        ProductionPhase currentPhase = phaseRepository.findByBatchIdAndPhaseNumber(batchId, batch.getCurrentPhase()).orElse(null);
        if (currentPhase != null) {
            currentPhase.setStatus("QUALITY_HOLD");
            phaseRepository.save(currentPhase);
        }

        if (batch.getMaterial() != null && batch.getOutputQuantity().compareTo(BigDecimal.ZERO) > 0) {
            try {
                inventoryService.recordRejection(batch.getMaterial().getId(), batch.getOutputQuantity(),
                    "Quality rejection: " + reason, "BATCH", batchId);
            } catch (Exception e) {
                log.warn("Could not record rejection inventory: {}", e.getMessage());
            }
        }

        List<QualityCheck> checks = qualityCheckRepository.findByBatchIdOrderByCreatedAtDesc(batchId);
        for (QualityCheck qc : checks) {
            if (qc.getStatus() != QualityCheckStatus.PASS && qc.getStatus() != QualityCheckStatus.VERIFIED) {
                qc.setStatus(QualityCheckStatus.REJECTED);
                qualityCheckRepository.save(qc);
            }
        }

        eventService.publishEvent(new ProductionEventDto(
            "BATCH_REJECTED", order.getId(), order.getOrderNumber(),
            batch.getId(), batch.getBatchCode(),
            null, null, null,
            null, null,
            "Batch " + batch.getBatchCode() + " rejected: " + reason,
            Map.of("reason", reason, "rejectedQuantity", batch.getRejectedQuantity())
        ));

        auditService.log(null, "BATCH_REJECTED", "BATCH", batchId);

        log.warn("Batch {} rejected: {}", batch.getBatchCode(), reason);
        return new QualityActionResponse(batchId, batch.getBatchCode(), QualityCheckStatus.REJECTED, "REJECT", "Batch rejected: " + reason);
    }

    public QualityActionResponse reprocessBatch(Long batchId, String reason) {
        Batch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));

        batch.setStatus("REPROCESS_REQUESTED");
        batchRepository.save(batch);

        List<QualityCheck> checks = qualityCheckRepository.findByBatchIdOrderByCreatedAtDesc(batchId);
        for (QualityCheck qc : checks) {
            if (qc.getStatus() != QualityCheckStatus.PASS && qc.getStatus() != QualityCheckStatus.VERIFIED) {
                qc.setStatus(QualityCheckStatus.REPROCESS_REQUESTED);
                qualityCheckRepository.save(qc);
            }
        }

        ProductionOrder order = batch.getOrder();
        eventService.publishEvent(new ProductionEventDto(
            "BATCH_REPROCESS_REQUESTED", order.getId(), order.getOrderNumber(),
            batch.getId(), batch.getBatchCode(),
            null, null, null,
            null, null,
            "Batch " + batch.getBatchCode() + " reprocess requested: " + reason,
            Map.of("reason", reason)
        ));

        auditService.log(null, "BATCH_REPROCESS_REQUESTED", "BATCH", batchId);

        log.info("Batch {} reprocess requested: {}", batch.getBatchCode(), reason);
        return new QualityActionResponse(batchId, batch.getBatchCode(), QualityCheckStatus.REPROCESS_REQUESTED, "REPROCESS", "Reprocess requested: " + reason);
    }

    public QualityPhaseResult updatePhaseQualityStatus(Long batchId, Long phaseId) {
        QualityPhaseResult result = getPhaseQualityResult(batchId, phaseId);

        ProductionPhase phase = phaseRepository.findById(phaseId)
            .orElseThrow(() -> new ResourceNotFoundException("Phase not found: " + phaseId));

        if (result.overallStatus() != null) {
            phase.setQualityStatus(result.overallStatus().name());
            phaseRepository.save(phase);
        }

        if (result.overallResult() == QualityResult.PASS) {
            phase.setStatus("WAITING_FOR_VERIFICATION");
            phaseRepository.save(phase);
        } else if (result.overallResult() == QualityResult.FAIL) {
            phase.setStatus("QUALITY_HOLD");
            phaseRepository.save(phase);
        }

        return result;
    }

    public boolean allMandatoryChecksPassed(Long batchId, Long phaseId) {
        List<QualityCheck> mandatory = qualityCheckRepository.findMandatoryByBatchAndPhase(batchId, phaseId);
        if (mandatory.isEmpty()) return true;
        return mandatory.stream().allMatch(c -> c.getResult() == QualityResult.PASS);
    }

    public boolean anyMandatoryCheckFailed(Long batchId, Long phaseId) {
        List<QualityCheck> mandatory = qualityCheckRepository.findMandatoryByBatchAndPhase(batchId, phaseId);
        return mandatory.stream().anyMatch(c -> c.getResult() == QualityResult.FAIL);
    }

    @Transactional(readOnly = true)
    public List<QualityCheckDto> getQualityChecksByBatch(Long batchId) {
        return qualityCheckRepository.findByBatchIdOrderByCreatedAtDesc(batchId).stream()
            .map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<QualityCheckDto> getQualityChecksByPhase(Long phaseId) {
        return qualityCheckRepository.findByPhaseIdOrderByCreatedAtDesc(phaseId).stream()
            .map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<QualityCheckDto> getQualityChecksByOrder(Long orderId) {
        return qualityCheckRepository.findByProductionOrderIdOrderByCreatedAtDesc(orderId).stream()
            .map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public QualityCheckDto getQualityCheckById(Long id) {
        QualityCheck qc = qualityCheckRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Quality check not found: " + id));
        return toDto(qc);
    }

    private QualityCheckDto toDto(QualityCheck qc) {
        return new QualityCheckDto(
            qc.getId(),
            qc.getBatch() != null ? qc.getBatch().getId() : null,
            qc.getBatch() != null ? qc.getBatch().getBatchCode() : null,
            qc.getProductionOrder() != null ? qc.getProductionOrder().getId() : null,
            qc.getProductionOrder() != null ? qc.getProductionOrder().getOrderNumber() : null,
            qc.getPhase() != null ? qc.getPhase().getId() : null,
            qc.getPhase() != null ? qc.getPhase().getPhaseNumber() : null,
            qc.getPhase() != null ? qc.getPhase().getPhaseType() : null,
            qc.getCheckType(),
            qc.getParameterName(),
            qc.getObservedValue(),
            qc.getExpectedMin(),
            qc.getExpectedMax(),
            qc.getExpectedValue(),
            qc.getUnit(),
            qc.getStatus(),
            qc.getResult(),
            qc.getMandatory(),
            qc.getNotes(),
            qc.getInspector() != null ? qc.getInspector().getDisplayName() : null,
            qc.getMachine() != null ? qc.getMachine().getId() : null,
            qc.getMachine() != null ? qc.getMachine().getMachineCode() : null,
            qc.getCreatedAt(),
            qc.getCompletedAt(),
            qc.getVerifiedAt()
        );
    }
}
