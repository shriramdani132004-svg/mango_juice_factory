package com.smartfactory.repository;

import com.smartfactory.entity.MachineType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MachineTypeRepository extends JpaRepository<MachineType, Long> {

    Optional<MachineType> findByName(String name);
}
