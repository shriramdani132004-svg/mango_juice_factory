package com.smartfactory.repository;

import com.smartfactory.entity.ProductionPhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionPhaseRepository extends JpaRepository<ProductionPhase, Long> {

    List<ProductionPhase> findByBatchIdOrderByPhaseNumberAsc(Long batchId);

    Optional<ProductionPhase> findByBatchIdAndPhaseNumber(Long batchId, Integer phaseNumber);
}
