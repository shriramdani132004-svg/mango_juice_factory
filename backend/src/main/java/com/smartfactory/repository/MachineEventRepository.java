package com.smartfactory.repository;

import com.smartfactory.entity.MachineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MachineEventRepository extends JpaRepository<MachineEvent, Long> {

    List<MachineEvent> findByMachineIdOrderByCreatedAtDesc(Long machineId);

    List<MachineEvent> findTop50ByOrderByCreatedAtDesc();
}
