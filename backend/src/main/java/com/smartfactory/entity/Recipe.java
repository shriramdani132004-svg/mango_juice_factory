package com.smartfactory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "yield_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal yieldPercentage = new BigDecimal("100.00");

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "output_quantity", precision = 12, scale = 4)
    private BigDecimal outputQuantity = new BigDecimal("1000.00");

    @Column(name = "output_unit", length = 20)
    private String outputUnit = "btl";

    @Column(name = "batch_size", precision = 12, scale = 4)
    private BigDecimal batchSize = new BigDecimal("1000.00");

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeMaterial> recipeMaterials = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Recipe() {}

    public Recipe(Product product, String name, Integer version) {
        this.product = product;
        this.name = name;
        this.version = version;
    }

    public void addRecipeMaterial(RecipeMaterial rm) {
        recipeMaterials.add(rm);
        rm.setRecipe(this);
    }

    public void removeRecipeMaterial(RecipeMaterial rm) {
        recipeMaterials.remove(rm);
        rm.setRecipe(null);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public BigDecimal getYieldPercentage() { return yieldPercentage; }
    public void setYieldPercentage(BigDecimal yieldPercentage) { this.yieldPercentage = yieldPercentage; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public BigDecimal getOutputQuantity() { return outputQuantity; }
    public void setOutputQuantity(BigDecimal outputQuantity) { this.outputQuantity = outputQuantity; }
    public String getOutputUnit() { return outputUnit; }
    public void setOutputUnit(String outputUnit) { this.outputUnit = outputUnit; }
    public BigDecimal getBatchSize() { return batchSize; }
    public void setBatchSize(BigDecimal batchSize) { this.batchSize = batchSize; }
    public List<RecipeMaterial> getRecipeMaterials() { return recipeMaterials; }
    public void setRecipeMaterials(List<RecipeMaterial> recipeMaterials) { this.recipeMaterials = recipeMaterials; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
