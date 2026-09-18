package com.smartfactory.controller;

import com.smartfactory.dto.MaterialDto;
import com.smartfactory.enums.MaterialCategory;
import com.smartfactory.service.MaterialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materials")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    public ResponseEntity<List<MaterialDto>> getAllMaterials() {
        return ResponseEntity.ok(materialService.getAllMaterials());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialDto> getMaterialById(@PathVariable Long id) {
        return ResponseEntity.ok(materialService.getMaterialById(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<MaterialDto> getMaterialByCode(@PathVariable String code) {
        return ResponseEntity.ok(materialService.getMaterialByCode(code));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<MaterialDto>> getMaterialsByType(@PathVariable MaterialCategory type) {
        return ResponseEntity.ok(materialService.getMaterialsByType(type));
    }
}
