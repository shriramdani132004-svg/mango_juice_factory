package com.smartfactory.repository;

import com.smartfactory.entity.ProductionOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {

    Optional<ProductionOrder> findByOrderNumber(String orderNumber);

    List<ProductionOrder> findByStatusOrderByCreatedAtDesc(String status);

    List<ProductionOrder> findAllByOrderByCreatedAtDesc();

    boolean existsByOrderNumber(String orderNumber);
}
