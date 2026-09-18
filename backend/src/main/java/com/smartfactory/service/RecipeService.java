package com.smartfactory.service;

import com.smartfactory.dto.*;
import com.smartfactory.entity.Inventory;
import com.smartfactory.entity.Material;
import com.smartfactory.entity.Recipe;
import com.smartfactory.entity.RecipeMaterial;
import com.smartfactory.exception.ResourceNotFoundException;
import com.smartfactory.repository.InventoryRepository;
import com.smartfactory.repository.RecipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final InventoryRepository inventoryRepository;

    public RecipeService(RecipeRepository recipeRepository, InventoryRepository inventoryRepository) {
        this.recipeRepository = recipeRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public List<RecipeDto> getAllRecipes() {
        return recipeRepository.findAll().stream()
            .map(this::toDto)
            .toList();
    }

    public RecipeDto getRecipeById(Long id) {
        Recipe recipe = recipeRepository.findByIdWithMaterials(id)
            .orElseThrow(() -> new ResourceNotFoundException("Recipe not found: " + id));
        return toDto(recipe);
    }

    public List<RecipeDto> getRecipesByProduct(Long productId) {
        return recipeRepository.findActiveRecipesByProductId(productId).stream()
            .map(this::toDto)
            .toList();
    }

    public RecipeDto getActiveRecipeForProduct(Long productId) {
        List<Recipe> recipes = recipeRepository.findActiveRecipesByProductId(productId);
        if (recipes.isEmpty()) {
            throw new ResourceNotFoundException("No active recipe for product: " + productId);
        }
        return toDto(recipes.get(0));
    }

    public RecipeCalculationResponse calculateRequirements(Long recipeId, BigDecimal requestedOutputQuantity) {
        Recipe recipe = recipeRepository.findByIdWithMaterials(recipeId)
            .orElseThrow(() -> new ResourceNotFoundException("Recipe not found: " + recipeId));

        BigDecimal batchSize = recipe.getBatchSize();
        if (batchSize == null || batchSize.compareTo(BigDecimal.ZERO) == 0) {
            batchSize = recipe.getOutputQuantity() != null ? recipe.getOutputQuantity() : new BigDecimal("1000.00");
        }

        BigDecimal scaleFactor = requestedOutputQuantity.divide(batchSize, 6, RoundingMode.HALF_UP);

        List<RecipeRequirementDto> requirements = new ArrayList<>();
        for (RecipeMaterial rm : recipe.getRecipeMaterials()) {
            Material material = rm.getMaterial();
            BigDecimal requiredQty = rm.getQuantityPerBatch().multiply(scaleFactor)
                .setScale(4, RoundingMode.HALF_UP);

            Inventory inv = inventoryRepository.findByMaterialId(material.getId()).orElse(null);
            BigDecimal availableQty = inv != null ? inv.getAvailableQuantity() : BigDecimal.ZERO;

            String status;
            if (rm.getIsOptional()) {
                status = "OPTIONAL";
            } else if (availableQty.compareTo(requiredQty) >= 0) {
                status = "GREEN";
            } else if (availableQty.compareTo(BigDecimal.ZERO) > 0) {
                status = "RED";
            } else {
                status = "RED";
            }

            String category = rm.getMaterialCategory();
            if (category == null) {
                category = material.getMaterialType() != null ? material.getMaterialType().name() : "UNKNOWN";
            }

            requirements.add(new RecipeRequirementDto(
                material.getId(),
                material.getName(),
                material.getMaterialCode(),
                category,
                requiredQty,
                availableQty,
                rm.getUnit(),
                status,
                rm.getIsOptional()
            ));
        }

        return new RecipeCalculationResponse(
            recipe.getId(),
            recipe.getName(),
            recipe.getProduct().getId(),
            recipe.getProduct().getName(),
            requestedOutputQuantity,
            recipe.getOutputUnit(),
            requirements
        );
    }

    private RecipeDto toDto(Recipe recipe) {
        List<RecipeMaterialDto> materialDtos = recipe.getRecipeMaterials() != null
            ? recipe.getRecipeMaterials().stream().map(rm -> new RecipeMaterialDto(
                rm.getId(),
                rm.getMaterial().getId(),
                rm.getMaterial().getName(),
                rm.getMaterial().getMaterialCode(),
                rm.getMaterial().getMaterialType() != null ? rm.getMaterial().getMaterialType().name() : null,
                rm.getMaterialCategory(),
                rm.getQuantityPerBatch(),
                rm.getUnit(),
                rm.getIsOptional(),
                rm.getBomOrder()
            )).toList()
            : List.of();

        return new RecipeDto(
            recipe.getId(),
            recipe.getProduct().getId(),
            recipe.getProduct().getName(),
            recipe.getName(),
            recipe.getVersion(),
            recipe.getYieldPercentage(),
            recipe.getOutputQuantity(),
            recipe.getOutputUnit(),
            recipe.getBatchSize(),
            recipe.getIsActive(),
            materialDtos
        );
    }
}
