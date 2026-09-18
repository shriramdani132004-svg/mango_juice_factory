package com.smartfactory.repository;

import com.smartfactory.entity.ProductionOrderRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionOrderRequirementRepository extends JpaRepository<ProductionOrderRequirement, Long> {

    List<ProductionOrderRequirement> findByOrderIdOrderByRequirementTypeAsc(Long orderId);

    List<ProductionOrderRequirement> findByOrderIdAndStatusOrderByRequirementTypeAsc(Long orderId, String status);

    boolean existsByOrderIdAndStatus(Long orderId, String status);
}
