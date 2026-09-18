package com.smartfactory.repository;

import com.smartfactory.entity.InventoryTransaction;
import com.smartfactory.enums.InventoryTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    List<InventoryTransaction> findByMaterialIdOrderByCreatedAtDesc(Long materialId);

    List<InventoryTransaction> findByTransactionType(InventoryTransactionType type);

    @Query("SELECT t FROM InventoryTransaction t WHERE t.referenceType = :refType AND t.referenceId = :refId")
    List<InventoryTransaction> findByReference(@Param("refType") String refType, @Param("refId") Long refId);

    @Query("SELECT t FROM InventoryTransaction t ORDER BY t.createdAt DESC LIMIT :limit")
    List<InventoryTransaction> findRecentTransactions(@Param("limit") int limit);
}
