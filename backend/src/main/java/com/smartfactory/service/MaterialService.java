package com.smartfactory.service;

import com.smartfactory.dto.MaterialDto;
import com.smartfactory.entity.Material;
import com.smartfactory.enums.MaterialCategory;
import com.smartfactory.exception.ResourceNotFoundException;
import com.smartfactory.repository.MaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MaterialService {

    private final MaterialRepository materialRepository;

    public MaterialService(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    public List<MaterialDto> getAllMaterials() {
        return materialRepository.findAllActive().stream()
            .map(this::toDto)
            .toList();
    }

    public MaterialDto getMaterialById(Long id) {
        Material material = materialRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + id));
        return toDto(material);
    }

    public MaterialDto getMaterialByCode(String code) {
        Material material = materialRepository.findByMaterialCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + code));
        return toDto(material);
    }

    public List<MaterialDto> getMaterialsByType(MaterialCategory type) {
        return materialRepository.findByMaterialType(type).stream()
            .map(this::toDto)
            .toList();
    }

    private MaterialDto toDto(Material m) {
        return new MaterialDto(
            m.getId(),
            m.getName(),
            m.getMaterialCode(),
            m.getMaterialType() != null ? m.getMaterialType().name() : null,
            m.getCategory(),
            m.getUnit(),
            m.getDescription(),
            m.getIsActive(),
            m.getDensity(),
            m.getConversionFactor()
        );
    }
}
