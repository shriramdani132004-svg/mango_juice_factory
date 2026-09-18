package com.smartfactory.service;

import com.smartfactory.dto.*;
import com.smartfactory.entity.*;
import com.smartfactory.exception.ResourceNotFoundException;
import com.smartfactory.exception.ValidationException;
import com.smartfactory.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class PhaseExecutionService {

    private static final Logger log = LoggerFactory.getLogger(PhaseExecutionService.class);

    private static final String[] PHASE_NAMES = {
        "Receive & Inspect Mangoes", "Wash & Sort", "Peel & Pulp",
        "Filter", "Blend", "Pasteurize", "Quality Inspection",
        "Fill & Cap", "Label & Package", "Warehouse"
    };

    private static final String[] PHASE_TYPES = {
        "RECEIVE_AND_INSPECT", "WASH_AND_SORT", "PEEL_AND_PULP",
        "FILTER", "BLEND", "PASTEURIZE",
        "QUALITY_INSPECTION", "FILL_AND_CAP", "LABEL_AND_PACKAGE", "WAREHOUSE"
    };

    private final ProductionPhaseRepository phaseRepository;
    private final ProductionOrderRepository orderRepository;
    private final BatchRepository batchRepository;
    private final MachineRepository machineRepository;
    private final MachineAllocationService allocationService;
    private final SimulationEngine simulationEngine;
    private final ProductionEventService eventService;
    private final ProductionJobRepository jobRepository;
    private final QualityCheckService qualityCheckService;

    public PhaseExecutionService(ProductionPhaseRepository phaseRepository,
                                  ProductionOrderRepository orderRepository,
                                  BatchRepository batchRepository,
                                  MachineRepository machineRepository,
                                  MachineAllocationService allocationService,
                                  SimulationEngine simulationEngine,
                                  ProductionEventService eventService,
                                  ProductionJobRepository jobRepository,
                                  QualityCheckService qualityCheckService) {
        this.phaseRepository = phaseRepository;
        this.orderRepository = orderRepository;
        this.batchRepository = batchRepository;
        this.machineRepository = machineRepository;
        this.allocationService = allocationService;
        this.simulationEngine = simulationEngine;
        this.eventService = eventService;
        this.jobRepository = jobRepository;
        this.qualityCheckService = qualityCheckService;
    }

    public PhaseExecutionDto startPhase(Long phaseId) {
        ProductionPhase phase = phaseRepository.findById(phaseId)
            .orElseThrow(() -> new ResourceNotFoundException("Phase not found: " + phaseId));

        Batch batch = phase.getBatch();
        ProductionOrder order = batch.getOrder();

        if (!"READY".equals(phase.getStatus()) && !"PENDING".equals(phase.getStatus())) {
            throw new ValidationException("Phase must be READY or PENDING to start. Current: " + phase.getStatus());
        }

        if (!"READY".equals(order.getStatus()) && !"RUNNING".equals(order.getStatus())) {
            throw new ValidationException("Order must be READY or RUNNING to start phase. Current: " + order.getStatus());
        }

        if (phase.getPhaseNumber() > 1) {
            ProductionPhase prevPhase = phaseRepository.findByBatchIdAndPhaseNumber(batch.getId(), phase.getPhaseNumber() - 1)
                .orElseThrow(() -> new ValidationException("Previous phase not found"));
            if (!"VERIFIED".equals(prevPhase.getStatus())) {
                throw new ValidationException("Previous phase must be VERIFIED before starting this phase. Phase " + (phase.getPhaseNumber() - 1) + " is " + prevPhase.getStatus());
            }
        }

        if (simulationEngine.isPhaseExecuting(phaseId)) {
            throw new ValidationException("Phase is already executing");
        }

        if (phase.getInputQuantity() == null || phase.getInputQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Phase has no input quantity configured");
        }

        String capability = phase.getRequiredMachineCapability();
        if (!allocationService.hasAvailableMachines(capability)) {
            throw new ValidationException("No compatible machines available for capability: " + capability);
        }

        int machineCount = Math.min(3, (int) allocationService.getAvailableMachineCount(capability));
        List<Machine> machines = allocationService.allocateMachines(capability, machineCount);
        if (machines.isEmpty()) {
            throw new ValidationException("Could not allocate any machines for capability: " + capability);
        }

        log.info("Starting phase {} for batch {} with {} machines", phase.getPhaseNumber(), batch.getBatchCode(), machines.size());

        SimulationEngine.PhaseExecutionState state = simulationEngine.startPhaseExecution(phase, machines);

        return buildExecutionDto(state, phase);
    }

    public void pausePhase(Long phaseId) {
        validatePhaseExists(phaseId);
        simulationEngine.pauseExecution(phaseId);
    }

    public void resumePhase(Long phaseId) {
        validatePhaseExists(phaseId);
        simulationEngine.resumeExecution(phaseId);
    }

    public void stopPhase(Long phaseId) {
        validatePhaseExists(phaseId);
        simulationEngine.emergencyStop(phaseId);
    }

    public PhaseCompletionDto getPhaseCompletion(Long phaseId) {
        ProductionPhase phase = phaseRepository.findById(phaseId)
            .orElseThrow(() -> new ResourceNotFoundException("Phase not found: " + phaseId));

        if (!"WAITING_FOR_VERIFICATION".equals(phase.getStatus()) && !"VERIFIED".equals(phase.getStatus()) && !"WAITING_FOR_QUALITY".equals(phase.getStatus())) {
            throw new ValidationException("Phase has not completed yet. Current status: " + phase.getStatus());
        }

        Batch batch = phase.getBatch();
        List<ProductionJob> jobs = findByPhaseId(phaseId);
        long machinesUsed = jobs.stream().map(j -> j.getMachine().getId()).distinct().count();

        BigDecimal avgRate = jobs.stream()
            .map(ProductionJob::getProductionRate)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(Math.max(1, machinesUsed)), 4, java.math.RoundingMode.HALF_UP);

        return new PhaseCompletionDto(
            phaseId,
            phase.getPhaseNumber(),
            PHASE_NAMES[Math.min(phase.getPhaseNumber() - 1, PHASE_NAMES.length - 1)],
            phase.getPhaseType(),
            phase.getInputQuantity(),
            phase.getInputQuantity(),
            phase.getOutputQuantity(),
            phase.getWasteQuantity(),
            BigDecimal.ZERO,
            machinesUsed,
            avgRate,
            phase.getActualDurationMinutes() != null ? phase.getActualDurationMinutes() * 60 : 0,
            batch.getBatchCode(),
            phase.getStatus()
        );
    }

    public PhaseVerifyResponse verifyPhase(Long phaseId) {
        ProductionPhase phase = phaseRepository.findById(phaseId)
            .orElseThrow(() -> new ResourceNotFoundException("Phase not found: " + phaseId));

        if (!"WAITING_FOR_VERIFICATION".equals(phase.getStatus())) {
            throw new ValidationException("Phase must be WAITING_FOR_VERIFICATION to verify. Current: " + phase.getStatus());
        }

        Batch batch = phase.getBatch();

        boolean allPassed = qualityCheckService.allMandatoryChecksPassed(batch.getId(), phaseId);
        boolean anyFailed = qualityCheckService.anyMandatoryCheckFailed(batch.getId(), phaseId);

        if (!allPassed) {
            throw new ValidationException("Cannot verify phase: not all mandatory quality checks have passed. Failed checks must be addressed first.");
        }

        if (anyFailed) {
            throw new ValidationException("Cannot verify phase: mandatory quality check(s) have failed. Use quarantine/reprocess/reject action first.");
        }

        String batchStatus = batch.getStatus();
        if ("QUARANTINED".equals(batchStatus) || "REJECTED".equals(batchStatus)) {
            throw new ValidationException("Cannot verify phase: batch is " + batchStatus);
        }

        phase.setStatus("VERIFIED");
        phase.setVerificationStatus("VERIFIED");
        phase.setVerifiedAt(java.time.LocalDateTime.now());
        phaseRepository.save(phase);

        ProductionOrder order = batch.getOrder();

        ProductionPhase nextPhase = phaseRepository.findByBatchIdAndPhaseNumber(batch.getId(), phase.getPhaseNumber() + 1)
            .orElse(null);

        String nextPhaseStatus = null;
        Long nextPhaseId = null;
        Integer nextPhaseNumber = null;

        if (nextPhase != null) {
            nextPhase.setStatus("READY");
            phaseRepository.save(nextPhase);
            nextPhaseStatus = "READY";
            nextPhaseId = nextPhase.getId();
            nextPhaseNumber = nextPhase.getPhaseNumber();
        } else {
            order.setStatus("COMPLETED");
            order.setCompletedAt(java.time.LocalDateTime.now());
            orderRepository.save(order);

            batch.setStatus("COMPLETED");
            batch.setCompletedAt(java.time.LocalDateTime.now());
            batchRepository.save(batch);
        }

        eventService.publishEvent(new ProductionEventDto(
            "PHASE_VERIFIED", order.getId(), order.getOrderNumber(),
            batch.getId(), batch.getBatchCode(),
            phaseId, phase.getPhaseNumber(), phase.getPhaseType(),
            null, null,
            "Phase " + phase.getPhaseNumber() + " verified" + (nextPhase != null ? ". Phase " + nextPhase.getPhaseNumber() + " unlocked" : ". Order completed!"),
            null
        ));

        log.info("Phase {} verified. Next phase: {}", phase.getPhaseNumber(),
            nextPhase != null ? nextPhase.getPhaseNumber() : "ORDER COMPLETED");

        return new PhaseVerifyResponse(
            phaseId, phase.getPhaseNumber(), phase.getPhaseType(), "VERIFIED",
            nextPhaseId, nextPhaseNumber, nextPhaseStatus,
            nextPhase != null ? order.getStatus() : "COMPLETED",
            nextPhase != null ? "Phase " + nextPhase.getPhaseNumber() + " is now ready" : "All phases complete! Order marked COMPLETED."
        );
    }

    private void validatePhaseExists(Long phaseId) {
        if (!phaseRepository.existsById(phaseId)) {
            throw new ResourceNotFoundException("Phase not found: " + phaseId);
        }
    }

    private List<ProductionJob> findByPhaseId(Long phaseId) {
        return jobRepository.findByPhaseId(phaseId);
    }

    private PhaseExecutionDto buildExecutionDto(SimulationEngine.PhaseExecutionState state, ProductionPhase phase) {
        String[] names = {
            "Receive & Inspect Mangoes", "Wash & Sort", "Peel & Pulp",
            "Filter", "Blend", "Pasteurize", "Quality Inspection",
            "Fill & Cap", "Label & Package", "Warehouse"
        };

        List<MachineInfoDto> machines = state.machines.stream()
            .map(m -> new MachineInfoDto(
                m.getId(), m.getMachineCode(), m.getName(), m.getCapability(),
                m.getStatus(), m.getCapacity(), m.getProductionRate(), m.getHealthScore(),
                m.getTemperature(), m.getRpm(), m.getVibration(), m.getPowerConsumption(),
                BigDecimal.ZERO
            ))
            .toList();

        return new PhaseExecutionDto(
            state.phaseId, state.orderId, state.orderNumber, null,
            state.phaseNumber,
            names[Math.min(state.phaseNumber - 1, names.length - 1)],
            state.phaseType, state.status,
            state.inputQuantity, BigDecimal.ZERO, null, BigDecimal.ZERO,
            state.totalRate, BigDecimal.ZERO,
            phase.getEstimatedDurationMinutes(), 0, phase.getEstimatedDurationMinutes() * 60,
            phase.getRequiredMachineCapability(), machines, List.of(), "DEMO"
        );
    }
}
