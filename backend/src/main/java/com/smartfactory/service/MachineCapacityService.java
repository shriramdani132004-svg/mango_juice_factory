package com.smartfactory.service;

import com.smartfactory.dto.MachineCapacityResponse;
import com.smartfactory.repository.MachineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MachineCapacityService {

    private final MachineRepository machineRepository;

    private static final List<String> USABLE_STATUSES = List.of("IDLE", "READY");

    private static final List<String> PHASE_CAPABILITIES = List.of(
        "RECEIVING", "WASHING", "PEELING", "FILTERING",
        "BLENDING", "PASTEURIZING", "QUALITY_INSPECTION",
        "FILLING", "LABELING", "WAREHOUSING"
    );

    private static final String[] PHASE_NAMES = {
        "RECEIVE_AND_INSPECT", "WASH_AND_SORT", "PEEL_AND_PULP",
        "FILTER", "BLEND", "PASTEURIZE",
        "QUALITY_INSPECTION", "FILL_AND_CAP", "LABEL_AND_PACKAGE", "WAREHOUSE"
    };

    public MachineCapacityService(MachineRepository machineRepository) {
        this.machineRepository = machineRepository;
    }

    public List<MachineCapacityResponse> getAllPhaseCapacities() {
        List<MachineCapacityResponse> capacities = new java.util.ArrayList<>();

        for (int i = 0; i < PHASE_CAPABILITIES.size(); i++) {
            String cap = PHASE_CAPABILITIES.get(i);
            capacities.add(getPhaseCapacity(cap, PHASE_NAMES[i]));
        }

        return capacities;
    }

    public MachineCapacityResponse getPhaseCapacity(String capability, String phaseName) {
        long totalCount = machineRepository.countByCapabilityAndStatusInAndIsActiveTrue(capability, USABLE_STATUSES);
        long totalAll = machineRepository.countByCapabilityAndStatusInAndIsActiveTrue(capability, List.of("IDLE", "READY", "RUNNING", "PAUSED"));

        BigDecimal totalRate = machineRepository.sumProductionRateByCapabilityAndStatusIn(capability, List.of("IDLE", "READY", "RUNNING", "PAUSED"));

        BigDecimal availableRate = machineRepository.sumProductionRateByCapabilityAndStatusIn(capability, USABLE_STATUSES);
        BigDecimal availableCapacity = machineRepository.sumCapacityByCapabilityAndStatusIn(capability, USABLE_STATUSES);

        String status;
        String explanation;
        if (totalCount > 0) {
            status = "GREEN";
            explanation = totalCount + " machines available for " + capability;
        } else {
            status = "RED";
            explanation = "No machines available for " + capability;
        }

        return new MachineCapacityResponse(
            capability,
            phaseName,
            totalAll,
            totalCount,
            totalRate,
            availableCapacity,
            availableRate,
            status,
            explanation
        );
    }

    public boolean hasAvailableMachine(String capability) {
        return machineRepository.countByCapabilityAndStatusInAndIsActiveTrue(capability, USABLE_STATUSES) > 0;
    }
}
