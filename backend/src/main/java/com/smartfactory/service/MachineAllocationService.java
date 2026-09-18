package com.smartfactory.service;

import com.smartfactory.entity.Machine;
import com.smartfactory.repository.MachineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MachineAllocationService {

    private static final Logger log = LoggerFactory.getLogger(MachineAllocationService.class);

    private static final List<String> USABLE_STATUSES = List.of("IDLE", "READY");

    private final MachineRepository machineRepository;

    public MachineAllocationService(MachineRepository machineRepository) {
        this.machineRepository = machineRepository;
    }

    /**
     * Deterministic machine allocation strategy:
     * 1. Capability match
     * 2. READY over IDLE (prefer machines already warmed up)
     * 3. Higher health score first (healthier machines)
     * 4. Higher production rate first
     * 5. Lower machine code (stable tie-breaker)
     *
     * Returns machines sorted by this priority.
     */
    public List<Machine> allocateMachines(String capability, int count) {
        List<Machine> candidates = machineRepository.findAvailableMachinesByCapability(capability, USABLE_STATUSES);

        candidates.sort(Comparator
            .comparing((Machine m) -> "READY".equals(m.getStatus()) ? 0 : 1)
            .thenComparing(Comparator.comparing(Machine::getHealthScore).reversed())
            .thenComparing(Comparator.comparing(Machine::getProductionRate).reversed())
            .thenComparing(Machine::getMachineCode)
        );

        List<Machine> selected = new ArrayList<>();
        for (Machine m : candidates) {
            if (selected.size() >= count) break;
            if (m.getHealthScore() >= 50) {
                selected.add(m);
            }
        }

        if (selected.isEmpty() && !candidates.isEmpty()) {
            selected.add(candidates.get(0));
        }

        log.info("Allocated {} machines for capability '{}' from {} candidates",
            selected.size(), capability, candidates.size());

        return selected;
    }

    public boolean hasAvailableMachines(String capability) {
        return machineRepository.countByCapabilityAndStatusInAndIsActiveTrue(capability, USABLE_STATUSES) > 0;
    }

    public long getAvailableMachineCount(String capability) {
        return machineRepository.countByCapabilityAndStatusInAndIsActiveTrue(capability, USABLE_STATUSES);
    }
}
