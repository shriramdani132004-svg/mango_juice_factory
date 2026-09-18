package com.smartfactory.repository;

import com.smartfactory.entity.Material;
import com.smartfactory.enums.MaterialType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {

    Optional<Material> findByMaterialCode(String materialCode);

    List<Material> findByMaterialType(MaterialType materialType);

    List<Material> findByIsActiveTrue();

    @Query("SELECT m FROM Material m WHERE m.isActive = true ORDER BY m.name")
    List<Material> findAllActive();
}
