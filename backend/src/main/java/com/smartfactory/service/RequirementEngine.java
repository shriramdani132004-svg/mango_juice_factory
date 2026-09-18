package com.smartfactory.service;

import com.smartfactory.dto.RequirementItemDto;
import com.smartfactory.entity.*;
import com.smartfactory.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RequirementEngine {

    private final RecipeRepository recipeRepository;
    private final InventoryRepository inventoryRepository;
    private final MachineRepository machineRepository;

    public RequirementEngine(RecipeRepository recipeRepository,
                             InventoryRepository inventoryRepository,
                             MachineRepository machineRepository) {
        this.recipeRepository = recipeRepository;
        this.inventoryRepository = inventoryRepository;
        this.machineRepository = machineRepository;
    }

    public List<RequirementItemDto> calculateRequirements(ProductionOrder order) {
        Recipe recipe = recipeRepository.findByIdWithMaterials(order.getRecipe().getId())
            .orElseThrow(() -> new RuntimeException("Recipe not found: " + order.getRecipe().getId()));

        BigDecimal inputQty = order.getInputQuantity();

        BigDecimal batchSize = recipe.getBatchSize();
        if (batchSize == null || batchSize.compareTo(BigDecimal.ZERO) == 0) {
            batchSize = recipe.getOutputQuantity() != null ? recipe.getOutputQuantity() : new BigDecimal("1000.00");
        }

        BigDecimal batchSizeKg = batchSize;
        BigDecimal scaleFactor = inputQty.divide(batchSizeKg, 6, RoundingMode.HALF_UP);

        List<RequirementItemDto> requirements = new ArrayList<>();

        for (RecipeMaterial rm : recipe.getRecipeMaterials()) {
            Material material = rm.getMaterial();
            BigDecimal requiredQty = rm.getQuantityPerBatch().multiply(scaleFactor)
                .setScale(4, RoundingMode.HALF_UP);

            Inventory inv = inventoryRepository.findByMaterialId(material.getId()).orElse(null);
            BigDecimal availableQty = inv != null ? inv.getAvailableQuantity() : BigDecimal.ZERO;

            String status;
            String explanation;
            if (rm.getIsOptional()) {
                status = "YELLOW";
                explanation = "Optional material — not required for production";
            } else if (availableQty.compareTo(requiredQty) >= 0) {
                status = "GREEN";
                explanation = "Sufficient stock: " + availableQty + " " + rm.getUnit() + " available, " + requiredQty + " " + rm.getUnit() + " required";
            } else if (availableQty.compareTo(BigDecimal.ZERO) > 0) {
                status = "RED";
                explanation = "Insufficient stock: " + availableQty + " " + rm.getUnit() + " available, " + requiredQty + " " + rm.getUnit() + " required — short by " + requiredQty.subtract(availableQty) + " " + rm.getUnit();
            } else {
                status = "RED";
                explanation = "Out of stock — " + requiredQty + " " + rm.getUnit() + " required";
            }

            String category = rm.getMaterialCategory();
            if (category == null) {
                category = material.getMaterialType() != null ? material.getMaterialType().name() : "UNKNOWN";
            }

            requirements.add(new RequirementItemDto(
                null,
                "MATERIAL",
                material.getName(),
                category,
                material.getMaterialCode(),
                material.getId(),
                requiredQty,
                rm.getUnit(),
                availableQty,
                status,
                rm.getIsOptional(),
                explanation
            ));
        }

        List<String> machineCapabilities = List.of(
            "RECEIVING", "WASHING", "PEELING", "FILTERING",
            "BLENDING", "PASTEURIZING", "QUALITY_INSPECTION",
            "FILLING", "LABELING", "WAREHOUSING"
        );

        List<String> usableStatuses = List.of("IDLE", "READY");

        for (String cap : machineCapabilities) {
            long availableCount = machineRepository.countByCapabilityAndStatusInAndIsActiveTrue(cap, usableStatuses);
            BigDecimal totalRate = machineRepository.sumProductionRateByCapabilityAndStatusIn(cap, usableStatuses);

            String status;
            String explanation;
            if (availableCount > 0) {
                status = "GREEN";
                explanation = availableCount + " machines available for " + cap + " — combined production rate: " + totalRate.setScale(2, RoundingMode.HALF_UP) + " units/hr";
            } else {
                status = "RED";
                explanation = "No machines available for " + cap + " — all machines are busy, failed, or in maintenance";
            }

            requirements.add(new RequirementItemDto(
                null,
                "MACHINE",
                cap.replace("_", " ").toLowerCase(),
                "MACHINE_CAPABILITY",
                cap,
                null,
                BigDecimal.ONE,
                "unit",
                BigDecimal.valueOf(availableCount),
                status,
                true,
                explanation
            ));
        }

        return requirements;
    }
}
