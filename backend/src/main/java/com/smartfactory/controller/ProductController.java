package com.smartfactory.controller;

import com.smartfactory.dto.ProductDto;
import com.smartfactory.dto.RecipeDto;
import com.smartfactory.service.ProductService;
import com.smartfactory.service.RecipeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final RecipeService recipeService;

    public ProductController(ProductService productService, RecipeService recipeService) {
        this.productService = productService;
        this.recipeService = recipeService;
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ProductDto> getProductByCode(@PathVariable String code) {
        return ResponseEntity.ok(productService.getProductByCode(code));
    }

    @GetMapping("/{id}/recipe")
    public ResponseEntity<RecipeDto> getProductRecipe(@PathVariable Long id) {
        return ResponseEntity.ok(recipeService.getActiveRecipeForProduct(id));
    }
}
