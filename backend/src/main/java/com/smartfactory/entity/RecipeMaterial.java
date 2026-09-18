package com.smartfactory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recipe_materials")
public class RecipeMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_material_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "quantity_per_batch", nullable = false, precision = 12, scale = 4)
    private BigDecimal quantityPerBatch;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "is_optional", nullable = false)
    private Boolean isOptional = false;

    @Column(name = "bom_order")
    private Integer bomOrder = 0;

    @Column(name = "material_category", length = 30)
    private String materialCategory;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public RecipeMaterial() {}

    public RecipeMaterial(Recipe recipe, Material material, BigDecimal quantityPerBatch, String unit) {
        this.recipe = recipe;
        this.material = material;
        this.quantityPerBatch = quantityPerBatch;
        this.unit = unit;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Recipe getRecipe() { return recipe; }
    public void setRecipe(Recipe recipe) { this.recipe = recipe; }
    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
    public BigDecimal getQuantityPerBatch() { return quantityPerBatch; }
    public void setQuantityPerBatch(BigDecimal quantityPerBatch) { this.quantityPerBatch = quantityPerBatch; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Boolean getIsOptional() { return isOptional; }
    public void setIsOptional(Boolean isOptional) { this.isOptional = isOptional; }
    public Integer getBomOrder() { return bomOrder; }
    public void setBomOrder(Integer bomOrder) { this.bomOrder = bomOrder; }
    public String getMaterialCategory() { return materialCategory; }
    public void setMaterialCategory(String materialCategory) { this.materialCategory = materialCategory; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
