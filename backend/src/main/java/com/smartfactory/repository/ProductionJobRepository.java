package com.smartfactory.repository;

import com.smartfactory.entity.ProductionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionJobRepository extends JpaRepository<ProductionJob, Long> {

    List<ProductionJob> findByPhaseId(Long phaseId);

    List<ProductionJob> findByBatchId(Long batchId);

    List<ProductionJob> findByMachineIdAndStatusIn(Long machineId, List<String> statuses);

    long countByMachineIdAndStatus(Long machineId, String status);
}
