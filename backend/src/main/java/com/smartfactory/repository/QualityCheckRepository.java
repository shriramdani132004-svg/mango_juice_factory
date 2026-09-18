package com.smartfactory.repository;

import com.smartfactory.entity.QualityCheck;
import com.smartfactory.enums.QualityCheckStatus;
import com.smartfactory.enums.QualityResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QualityCheckRepository extends JpaRepository<QualityCheck, Long> {

    List<QualityCheck> findByBatchIdOrderByCreatedAtDesc(Long batchId);

    List<QualityCheck> findByPhaseIdOrderByCreatedAtDesc(Long phaseId);

    List<QualityCheck> findByProductionOrderIdOrderByCreatedAtDesc(Long orderId);

    List<QualityCheck> findByBatchIdAndPhaseId(Long batchId, Long phaseId);

    @Query("SELECT qc FROM QualityCheck qc WHERE qc.batch.id = :batchId AND qc.phase.id = :phaseId AND qc.mandatory = true")
    List<QualityCheck> findMandatoryByBatchAndPhase(@Param("batchId") Long batchId, @Param("phaseId") Long phaseId);

    @Query("SELECT qc FROM QualityCheck qc WHERE qc.batch.id = :batchId AND qc.mandatory = true AND qc.result = 'FAIL'")
    List<QualityCheck> findFailedMandatoryByBatch(@Param("batchId") Long batchId);

    @Query("SELECT qc FROM QualityCheck qc WHERE qc.batch.id = :batchId AND qc.phase.id = :phaseId AND qc.mandatory = true AND qc.result = 'PASS'")
    List<QualityCheck> findPassedMandatoryByBatchAndPhase(@Param("batchId") Long batchId, @Param("phaseId") Long phaseId);

    @Query("SELECT qc FROM QualityCheck qc WHERE qc.batch.id = :batchId AND qc.phase.id = :phaseId AND qc.status IN ('PENDING', 'IN_PROGRESS')")
    List<QualityCheck> findPendingByBatchAndPhase(@Param("batchId") Long batchId, @Param("phaseId") Long phaseId);

    long countByBatchIdAndPhaseIdAndResult(Long batchId, Long phaseId, QualityResult result);

    long countByBatchIdAndPhaseIdAndMandatoryAndResult(Long batchId, Long phaseId, Boolean mandatory, QualityResult result);
}
