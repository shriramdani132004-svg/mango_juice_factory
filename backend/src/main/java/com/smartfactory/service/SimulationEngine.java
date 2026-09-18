package com.smartfactory.service;

import com.smartfactory.dto.*;
import com.smartfactory.entity.*;
import com.smartfactory.enums.MaterialType;
import com.smartfactory.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
public class SimulationEngine {

    private static final Logger log = LoggerFactory.getLogger(SimulationEngine.class);

    @Value("${simulation.speed-multiplier:20}")
    private int speedMultiplier;

    @Value("${simulation.tick-interval-ms:500}")
    private int tickIntervalMs;

    private final Map<Long, PhaseExecutionState> activeExecutions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "sim-engine");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean running = false;

    private final MachineRepository machineRepository;
    private final ProductionPhaseRepository phaseRepository;
    private final ProductionOrderRepository orderRepository;
    private final BatchRepository batchRepository;
    private final ProductionJobRepository jobRepository;
    private final MachineEventRepository eventRepository;
    private final InventoryService inventoryService;
    private final MaterialRepository materialRepository;
    private final ProductionEventService eventService;
    private final QualityCheckService qualityCheckService;

    public SimulationEngine(MachineRepository machineRepository,
                             ProductionPhaseRepository phaseRepository,
                             ProductionOrderRepository orderRepository,
                             BatchRepository batchRepository,
                             ProductionJobRepository jobRepository,
                             MachineEventRepository eventRepository,
                             InventoryService inventoryService,
                             MaterialRepository materialRepository,
                             ProductionEventService eventService,
                             QualityCheckService qualityCheckService) {
        this.machineRepository = machineRepository;
        this.phaseRepository = phaseRepository;
        this.orderRepository = orderRepository;
        this.batchRepository = batchRepository;
        this.jobRepository = jobRepository;
        this.eventRepository = eventRepository;
        this.inventoryService = inventoryService;
        this.materialRepository = materialRepository;
        this.eventService = eventService;
        this.qualityCheckService = qualityCheckService;
    }

    @PostConstruct
    public void init() {
        running = true;
        scheduler.scheduleAtFixedRate(this::tick, tickIntervalMs, tickIntervalMs, TimeUnit.MILLISECONDS);
        log.info("SimulationEngine started: speed={}x, tick={}ms", speedMultiplier, tickIntervalMs);
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        scheduler.shutdownNow();
        log.info("SimulationEngine stopped");
    }

    public PhaseExecutionState startPhaseExecution(ProductionPhase phase, List<Machine> machines) {
        Long phaseId = phase.getId();
        if (activeExecutions.containsKey(phaseId)) {
            throw new IllegalStateException("Phase " + phaseId + " is already executing");
        }

        Batch batch = phase.getBatch();
        ProductionOrder order = batch.getOrder();

        BigDecimal inputQty = phase.getInputQuantity();
        BigDecimal wasteRate = getWasteRate(phase.getPhaseNumber());
        BigDecimal wasteQty = inputQty.multiply(wasteRate).setScale(4, RoundingMode.HALF_UP);
        BigDecimal outputQty = inputQty.subtract(wasteQty).setScale(4, RoundingMode.HALF_UP);

        BigDecimal totalRate = machines.stream()
            .map(Machine::getProductionRate)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalRate.compareTo(BigDecimal.ZERO) == 0) {
            totalRate = BigDecimal.valueOf(200);
        }

        long totalTicksNeeded = inputQty.multiply(BigDecimal.valueOf(1000))
            .divide(totalRate.multiply(BigDecimal.valueOf(speedMultiplier)), 0, RoundingMode.CEILING).longValue();
        if (totalTicksNeeded < 1) totalTicksNeeded = 1;

        PhaseExecutionState state = new PhaseExecutionState();
        state.phaseId = phaseId;
        state.orderId = order.getId();
        state.batchId = batch.getId();
        state.phaseNumber = phase.getPhaseNumber();
        state.phaseType = phase.getPhaseType();
        state.batchCode = batch.getBatchCode();
        state.orderNumber = order.getOrderNumber();
        state.inputQuantity = inputQty;
        state.outputQuantity = outputQty;
        state.wasteQuantity = wasteQty;
        state.totalRate = totalRate;
        state.totalTicks = totalTicksNeeded;
        state.currentTick = 0;
        state.machines = machines;
        state.machineIds = machines.stream().map(Machine::getId).toList();
        state.status = "RUNNING";
        state.startedAt = LocalDateTime.now();
        state.elapsedSeconds = 0;

        for (Machine m : machines) {
            String from = m.getStatus();
            m.setStatus("RUNNING");
            machineRepository.save(m);
            recordMachineEvent(m, "MACHINE_ASSIGNED", from, "RUNNING",
                "Assigned to phase " + phase.getPhaseNumber() + " (" + phase.getPhaseType() + ")");
        }

        phase.setStatus("RUNNING");
        phase.setStartedAt(LocalDateTime.now());
        phaseRepository.save(phase);

        order.setStatus("RUNNING");
        if (order.getStartedAt() == null) order.setStartedAt(LocalDateTime.now());
        orderRepository.save(order);

        batch.setStatus("IN_PROGRESS");
        batch.setCurrentPhase(phase.getPhaseNumber());
        batchRepository.save(batch);

        for (Machine m : machines) {
            ProductionJob job = new ProductionJob(batch, phase, m,
                inputQty.divide(BigDecimal.valueOf(machines.size()), 4, RoundingMode.HALF_UP),
                outputQty.divide(BigDecimal.valueOf(machines.size()), 4, RoundingMode.HALF_UP),
                m.getProductionRate());
            job.setStatus("RUNNING");
            job.setStartedAt(LocalDateTime.now());
            job.setProgress(BigDecimal.ZERO);
            jobRepository.save(job);
        }

        activeExecutions.put(phaseId, state);

        eventService.publishEvent(new ProductionEventDto(
            "PHASE_STARTED", order.getId(), order.getOrderNumber(),
            batch.getId(), batch.getBatchCode(),
            phaseId, phase.getPhaseNumber(), phase.getPhaseType(),
            null, null,
            "Phase " + phase.getPhaseNumber() + " started with " + machines.size() + " machines",
            null
        ));

        log.info("Started phase execution: phaseId={}, batch={}, machines={}, input={}kg, rate={}/s",
            phaseId, batch.getBatchCode(), machines.size(), inputQty, totalRate);

        return state;
    }

    private void tick() {
        if (!running) return;

        for (Map.Entry<Long, PhaseExecutionState> entry : activeExecutions.entrySet()) {
            PhaseExecutionState state = entry.getValue();
            if (!"RUNNING".equals(state.status)) continue;

            try {
                processTick(state);
            } catch (Exception e) {
                log.error("Error processing tick for phase {}: {}", state.phaseId, e.getMessage(), e);
            }
        }
    }

    private void processTick(PhaseExecutionState state) {
        state.currentTick++;
        state.elapsedSeconds += (tickIntervalMs / 1000);

        BigDecimal processedSoFar = state.inputQuantity
            .multiply(BigDecimal.valueOf(state.currentTick))
            .divide(BigDecimal.valueOf(state.totalTicks), 4, RoundingMode.HALF_UP);

        if (processedSoFar.compareTo(state.inputQuantity) > 0) {
            processedSoFar = state.inputQuantity;
        }

        BigDecimal progressPct = processedSoFar.multiply(BigDecimal.valueOf(100))
            .divide(state.inputQuantity, 1, RoundingMode.HALF_UP);

        state.processedQuantity = processedSoFar;
        state.progress = progressPct;

        for (Machine m : state.machines) {
            updateMachineTelemetry(m);
        }

        if (state.currentTick >= state.totalTicks) {
            completePhase(state);
        } else {
            PhaseExecutionDto dto = buildProgressDto(state);
            eventService.publishPhaseProgress(state.orderId, dto);
        }
    }

    private void completePhase(PhaseExecutionState state) {
        state.status = "COMPLETED";

        ProductionPhase phase = phaseRepository.findById(state.phaseId).orElseThrow();
        Batch batch = batchRepository.findById(state.batchId).orElseThrow();
        ProductionOrder order = orderRepository.findById(state.orderId).orElseThrow();

        phase.setInputQuantity(state.inputQuantity);
        phase.setOutputQuantity(state.outputQuantity);
        phase.setWasteQuantity(state.wasteQuantity);
        phase.setActualDurationMinutes(state.elapsedSeconds / 60);
        phase.setStatus("WAITING_FOR_QUALITY");
        phase.setQualityStatus("PENDING");
        phase.setCompletedAt(LocalDateTime.now());
        phaseRepository.save(phase);

        for (Machine m : state.machines) {
            String from = m.getStatus();
            m.setStatus("IDLE");
            machineRepository.save(m);
            recordMachineEvent(m, "MACHINE_STOPPED", from, "IDLE",
                "Phase " + phase.getPhaseNumber() + " completed");

            List<ProductionJob> jobs = jobRepository.findByPhaseId(state.phaseId);
            for (ProductionJob job : jobs) {
                if (job.getMachine().getId().equals(m.getId()) && "RUNNING".equals(job.getStatus())) {
                    job.setStatus("COMPLETED");
                    job.setCompletedAt(LocalDateTime.now());
                    job.setProgress(BigDecimal.valueOf(100));
                    job.setActualOutput(state.outputQuantity.divide(BigDecimal.valueOf(state.machines.size()), 4, RoundingMode.HALF_UP));
                    jobRepository.save(job);
                }
            }
        }

        batch.setStatus("IN_PROGRESS");
        batchRepository.save(batch);

        createOutputInventory(phase, state);

        try {
            qualityCheckService.createQualityChecksForPhase(batch.getId(), phase.getId());
        } catch (Exception e) {
            log.warn("Could not create quality checks for phase {}: {}", phase.getPhaseNumber(), e.getMessage());
        }

        eventService.publishEvent(new ProductionEventDto(
            "PHASE_COMPLETED", order.getId(), order.getOrderNumber(),
            batch.getId(), batch.getBatchCode(),
            state.phaseId, phase.getPhaseNumber(), phase.getPhaseType(),
            null, null,
            "Phase " + phase.getPhaseNumber() + " completed. Output: " + state.outputQuantity + "kg, Waste: " + state.wasteQuantity + "kg. Quality checks required.",
            buildCompletionSummary(state)
        ));

        activeExecutions.remove(state.phaseId);

        log.info("Phase {} completed: input={}kg, output={}kg, waste={}kg, time={}s. Quality checks created.",
            phase.getPhaseNumber(), state.inputQuantity, state.outputQuantity, state.wasteQuantity, state.elapsedSeconds);
    }

    private void createOutputInventory(ProductionPhase phase, PhaseExecutionState state) {
        try {
            Material finishedMaterial = findOrCreateFinishedMaterial(phase.getPhaseType());
            inventoryService.recordProduction(finishedMaterial.getId(), state.outputQuantity,
                "PHASE", phase.getId());

            if (state.wasteQuantity.compareTo(BigDecimal.ZERO) > 0) {
                Material wasteMaterial = findOrCreateWasteMaterial(phase.getPhaseType());
                inventoryService.recordWaste(wasteMaterial.getId(), state.wasteQuantity,
                    "Phase " + phase.getPhaseNumber() + " waste",
                    "PHASE", phase.getId());
            }
        } catch (Exception e) {
            log.warn("Could not record inventory for phase {}: {}", phase.getPhaseNumber(), e.getMessage());
        }
    }

    private Material findOrCreateFinishedMaterial(String phaseType) {
        String materialCode = "INTERM-" + phaseType;
        return materialRepository.findByMaterialCode(materialCode)
            .orElseGet(() -> {
                Material m = new Material();
                m.setName("Intermediate: " + phaseType);
                m.setMaterialCode(materialCode);
                m.setMaterialType(MaterialType.FINISHED);
                m.setUnit("kg");
                m.setIsActive(true);
                return materialRepository.save(m);
            });
    }

    private Material findOrCreateWasteMaterial(String phaseType) {
        String materialCode = "WASTE-" + phaseType;
        return materialRepository.findByMaterialCode(materialCode)
            .orElseGet(() -> {
                Material m = new Material();
                m.setName("Waste: " + phaseType);
                m.setMaterialCode(materialCode);
                m.setMaterialType(MaterialType.RAW);
                m.setUnit("kg");
                m.setIsActive(true);
                return materialRepository.save(m);
            });
    }

    private void updateMachineTelemetry(Machine machine) {
        BigDecimal baseTemp = BigDecimal.valueOf(35);
        BigDecimal baseRpm = BigDecimal.valueOf(1200);
        BigDecimal baseVibration = BigDecimal.valueOf(0.5);
        BigDecimal basePower = BigDecimal.valueOf(45);

        BigDecimal temp = baseTemp.add(randomVariation(5))
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal rpm = baseRpm.add(randomVariation(100))
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal vibration = baseVibration.add(randomVariation(0.3))
            .max(BigDecimal.ZERO)
            .setScale(3, RoundingMode.HALF_UP);
        BigDecimal power = basePower.add(randomVariation(10))
            .max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP);

        machine.setTemperature(temp);
        machine.setRpm(rpm);
        machine.setVibration(vibration);
        machine.setPowerConsumption(power);
        machineRepository.save(machine);
    }

    private BigDecimal randomVariation(double range) {
        return BigDecimal.valueOf((Math.random() - 0.5) * 2 * range);
    }

    private void recordMachineEvent(Machine machine, String eventType, String from, String to, String message) {
        MachineEvent event = new MachineEvent(machine, eventType, message);
        event.setStateFrom(from);
        event.setStateTo(to);
        event.setTemperature(machine.getTemperature());
        event.setRpm(machine.getRpm());
        event.setVibration(machine.getVibration());
        event.setPowerConsumption(machine.getPowerConsumption());
        eventRepository.save(event);
    }

    public void pauseExecution(Long phaseId) {
        PhaseExecutionState state = activeExecutions.get(phaseId);
        if (state == null) throw new IllegalStateException("No active execution for phase: " + phaseId);
        if (!"RUNNING".equals(state.status)) throw new IllegalStateException("Phase is not running: " + state.status);

        state.status = "PAUSED";

        ProductionPhase phase = phaseRepository.findById(phaseId).orElseThrow();
        phase.setStatus("PAUSED");
        phaseRepository.save(phase);

        for (Machine m : state.machines) {
            String from = m.getStatus();
            m.setStatus("PAUSED");
            machineRepository.save(m);
            recordMachineEvent(m, "MACHINE_PAUSED", from, "PAUSED", "Phase paused");

            List<ProductionJob> jobs = jobRepository.findByPhaseId(phaseId);
            for (ProductionJob job : jobs) {
                if (job.getMachine().getId().equals(m.getId()) && "RUNNING".equals(job.getStatus())) {
                    job.setStatus("PAUSED");
                    jobRepository.save(job);
                }
            }
        }

        ProductionOrder order = orderRepository.findById(state.orderId).orElseThrow();
        order.setStatus("PAUSED");
        orderRepository.save(order);

        eventService.publishEvent(new ProductionEventDto(
            "PHASE_PAUSED", order.getId(), order.getOrderNumber(),
            state.batchId, state.batchCode,
            phaseId, state.phaseNumber, state.phaseType,
            null, null, "Phase " + state.phaseNumber + " paused", null
        ));

        log.info("Phase {} paused at progress {}%", phaseId, state.progress);
    }

    public void resumeExecution(Long phaseId) {
        PhaseExecutionState state = activeExecutions.get(phaseId);
        if (state == null) throw new IllegalStateException("No active execution for phase: " + phaseId);
        if (!"PAUSED".equals(state.status)) throw new IllegalStateException("Phase is not paused: " + state.status);

        state.status = "RUNNING";

        ProductionPhase phase = phaseRepository.findById(phaseId).orElseThrow();
        phase.setStatus("RUNNING");
        phaseRepository.save(phase);

        for (Machine m : state.machines) {
            String from = m.getStatus();
            m.setStatus("RUNNING");
            machineRepository.save(m);
            recordMachineEvent(m, "MACHINE_RESUMED", from, "RUNNING", "Phase resumed");

            List<ProductionJob> jobs = jobRepository.findByPhaseId(phaseId);
            for (ProductionJob job : jobs) {
                if (job.getMachine().getId().equals(m.getId()) && "PAUSED".equals(job.getStatus())) {
                    job.setStatus("RUNNING");
                    jobRepository.save(job);
                }
            }
        }

        ProductionOrder order = orderRepository.findById(state.orderId).orElseThrow();
        order.setStatus("RUNNING");
        orderRepository.save(order);

        eventService.publishEvent(new ProductionEventDto(
            "PHASE_RESUMED", order.getId(), order.getOrderNumber(),
            state.batchId, state.batchCode,
            phaseId, state.phaseNumber, state.phaseType,
            null, null, "Phase " + state.phaseNumber + " resumed", null
        ));

        log.info("Phase {} resumed at progress {}%", phaseId, state.progress);
    }

    public void emergencyStop(Long phaseId) {
        PhaseExecutionState state = activeExecutions.get(phaseId);
        if (state == null) throw new IllegalStateException("No active execution for phase: " + phaseId);

        state.status = "STOPPED";

        ProductionPhase phase = phaseRepository.findById(phaseId).orElseThrow();
        phase.setStatus("FAILED");
        phase.setCompletedAt(LocalDateTime.now());
        phase.setActualDurationMinutes(state.elapsedSeconds / 60);
        phaseRepository.save(phase);

        for (Machine m : state.machines) {
            String from = m.getStatus();
            m.setStatus("EMERGENCY_STOP");
            machineRepository.save(m);
            recordMachineEvent(m, "MACHINE_EMERGENCY_STOP", from, "EMERGENCY_STOP", "Emergency stop during phase " + phase.getPhaseNumber());

            List<ProductionJob> jobs = jobRepository.findByPhaseId(phaseId);
            for (ProductionJob job : jobs) {
                if (job.getMachine().getId().equals(m.getId())) {
                    job.setStatus("CANCELLED");
                    job.setErrorMessage("Emergency stop");
                    job.setCompletedAt(LocalDateTime.now());
                    jobRepository.save(job);
                }
            }
        }

        ProductionOrder order = orderRepository.findById(state.orderId).orElseThrow();
        order.setStatus("PAUSED");
        orderRepository.save(order);

        activeExecutions.remove(phaseId);

        eventService.publishEvent(new ProductionEventDto(
            "PHASE_EMERGENCY_STOP", order.getId(), order.getOrderNumber(),
            state.batchId, state.batchCode,
            phaseId, state.phaseNumber, state.phaseType,
            null, null, "EMERGENCY STOP at phase " + state.phaseNumber, null
        ));

        log.warn("EMERGENCY STOP at phase {}: progress={}%", phaseId, state.progress);
    }

    public boolean isPhaseExecuting(Long phaseId) {
        return activeExecutions.containsKey(phaseId);
    }

    public PhaseExecutionState getExecutionState(Long phaseId) {
        return activeExecutions.get(phaseId);
    }

    public Map<Long, PhaseExecutionState> getAllActiveExecutions() {
        return Collections.unmodifiableMap(activeExecutions);
    }

    private BigDecimal getWasteRate(int phaseNumber) {
        BigDecimal[] rates = {
            new BigDecimal("0.02"), new BigDecimal("0.03"), new BigDecimal("0.15"),
            new BigDecimal("0.02"), new BigDecimal("0.01"), new BigDecimal("0.005"),
            new BigDecimal("0.03"), new BigDecimal("0.01"), new BigDecimal("0.005"),
            new BigDecimal("0.002")
        };
        return rates[Math.min(phaseNumber - 1, rates.length - 1)];
    }

    private PhaseExecutionDto buildProgressDto(PhaseExecutionState state) {
        List<MachineInfoDto> machineInfos = state.machines.stream()
            .map(m -> new MachineInfoDto(
                m.getId(), m.getMachineCode(), m.getName(), m.getCapability(),
                m.getStatus(), m.getCapacity(), m.getProductionRate(), m.getHealthScore(),
                m.getTemperature(), m.getRpm(), m.getVibration(), m.getPowerConsumption(),
                state.progress
            ))
            .toList();

        List<TelemetrySnapshotDto> telemetry = state.machines.stream()
            .map(m -> new TelemetrySnapshotDto(
                m.getId(), m.getMachineCode(),
                m.getTemperature(), m.getRpm(), m.getVibration(), m.getPowerConsumption(),
                m.getHealthScore(), m.getStatus(), state.progress
            ))
            .toList();

        int remaining = (int) ((state.totalTicks - state.currentTick) * (tickIntervalMs / 1000) / speedMultiplier);

        String[] phaseNames = {
            "Receive & Inspect Mangoes", "Wash & Sort", "Peel & Pulp",
            "Filter", "Blend", "Pasteurize", "Quality Inspection",
            "Fill & Cap", "Label & Package", "Warehouse"
        };

        return new PhaseExecutionDto(
            state.phaseId, state.orderId, state.orderNumber, null,
            state.phaseNumber,
            phaseNames[Math.min(state.phaseNumber - 1, phaseNames.length - 1)],
            state.phaseType, state.status,
            state.inputQuantity, state.processedQuantity,
            null, state.wasteQuantity,
            state.totalRate, state.progress,
            null, state.elapsedSeconds, Math.max(0, remaining),
            null, machineInfos, telemetry, "DEMO"
        );
    }

    private Map<String, Object> buildCompletionSummary(PhaseExecutionState state) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("inputQuantity", state.inputQuantity);
        summary.put("outputQuantity", state.outputQuantity);
        summary.put("wasteQuantity", state.wasteQuantity);
        summary.put("processingTimeSeconds", state.elapsedSeconds);
        summary.put("machinesUsed", state.machines.size());
        summary.put("averageProductionRate", state.totalRate);
        return summary;
    }

    public static class PhaseExecutionState {
        public Long phaseId;
        public Long orderId;
        public Long batchId;
        public Integer phaseNumber;
        public String phaseType;
        public String batchCode;
        public String orderNumber;
        public BigDecimal inputQuantity;
        public BigDecimal outputQuantity;
        public BigDecimal wasteQuantity;
        public BigDecimal totalRate;
        public long totalTicks;
        public long currentTick;
        public List<Machine> machines;
        public List<Long> machineIds;
        public String status;
        public BigDecimal processedQuantity = BigDecimal.ZERO;
        public BigDecimal progress = BigDecimal.ZERO;
        public LocalDateTime startedAt;
        public int elapsedSeconds;
    }
}
