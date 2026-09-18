package com.smartfactory.controller;

import com.smartfactory.dto.RecipeCalculationResponse;
import com.smartfactory.dto.RecipeDto;
import com.smartfactory.service.RecipeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public ResponseEntity<List<RecipeDto>> getAllRecipes() {
        return ResponseEntity.ok(recipeService.getAllRecipes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeDto> getRecipeById(@PathVariable Long id) {
        return ResponseEntity.ok(recipeService.getRecipeById(id));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<RecipeDto>> getRecipesByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(recipeService.getRecipesByProduct(productId));
    }

    @GetMapping("/{id}/calculate")
    public ResponseEntity<RecipeCalculationResponse> calculateRequirements(
            @PathVariable Long id,
            @RequestParam BigDecimal quantity) {
        return ResponseEntity.ok(recipeService.calculateRequirements(id, quantity));
    }
}
