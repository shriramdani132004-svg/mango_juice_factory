package com.smartfactory.repository;

import com.smartfactory.entity.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface MachineRepository extends JpaRepository<Machine, Long> {

    java.util.Optional<Machine> findByMachineCode(String machineCode);

    List<Machine> findByCapabilityAndStatusInAndIsActiveTrue(String capability, List<String> statuses);

    long countByCapabilityAndStatusInAndIsActiveTrue(String capability, List<String> statuses);

    @Query("SELECT COALESCE(SUM(m.capacity), 0) FROM Machine m WHERE m.capability = :capability AND m.status IN :statuses AND m.isActive = true")
    BigDecimal sumCapacityByCapabilityAndStatusIn(@Param("capability") String capability, @Param("statuses") List<String> statuses);

    @Query("SELECT COALESCE(SUM(m.productionRate), 0) FROM Machine m WHERE m.capability = :capability AND m.status IN :statuses AND m.isActive = true")
    BigDecimal sumProductionRateByCapabilityAndStatusIn(@Param("capability") String capability, @Param("statuses") List<String> statuses);

    @Query("SELECT m FROM Machine m WHERE m.capability = :capability AND m.status IN :statuses AND m.isActive = true ORDER BY m.healthScore DESC, m.productionRate DESC, m.machineCode ASC")
    List<Machine> findAvailableMachinesByCapability(@Param("capability") String capability, @Param("statuses") List<String> statuses);
}
