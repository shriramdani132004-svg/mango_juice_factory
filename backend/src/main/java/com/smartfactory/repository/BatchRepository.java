package com.smartfactory.repository;

import com.smartfactory.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {

    List<Batch> findByOrderId(Long orderId);

    Optional<Batch> findByBatchCode(String batchCode);
}
