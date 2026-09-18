package com.smartfactory.entity;

import com.smartfactory.enums.Role;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "production_orders")
public class ProductionOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "input_material_id", nullable = false)
    private Material inputMaterial;

    @Column(name = "input_quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal inputQuantity;

    @Column(name = "bottle_size_ml", nullable = false)
    private Integer bottleSizeMl;

    @Column(name = "priority", nullable = false)
    private Integer priority = 5;

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(name = "estimated_output_qty", precision = 12, scale = 4)
    private BigDecimal estimatedOutputQty;

    @Column(name = "estimated_output_unit", length = 20)
    private String estimatedOutputUnit;

    @Column(name = "estimated_production_rate", precision = 10, scale = 4)
    private BigDecimal estimatedProductionRate;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

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

    public ProductionOrder() {}

    public ProductionOrder(String orderNumber, Product product, Recipe recipe,
                           Material inputMaterial, BigDecimal inputQuantity,
                           Integer bottleSizeMl, Integer priority) {
        this.orderNumber = orderNumber;
        this.product = product;
        this.recipe = recipe;
        this.inputMaterial = inputMaterial;
        this.inputQuantity = inputQuantity;
        this.bottleSizeMl = bottleSizeMl;
        this.priority = priority;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Recipe getRecipe() { return recipe; }
    public void setRecipe(Recipe recipe) { this.recipe = recipe; }
    public Material getInputMaterial() { return inputMaterial; }
    public void setInputMaterial(Material inputMaterial) { this.inputMaterial = inputMaterial; }
    public BigDecimal getInputQuantity() { return inputQuantity; }
    public void setInputQuantity(BigDecimal inputQuantity) { this.inputQuantity = inputQuantity; }
    public Integer getBottleSizeMl() { return bottleSizeMl; }
    public void setBottleSizeMl(Integer bottleSizeMl) { this.bottleSizeMl = bottleSizeMl; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getEstimatedOutputQty() { return estimatedOutputQty; }
    public void setEstimatedOutputQty(BigDecimal estimatedOutputQty) { this.estimatedOutputQty = estimatedOutputQty; }
    public String getEstimatedOutputUnit() { return estimatedOutputUnit; }
    public void setEstimatedOutputUnit(String estimatedOutputUnit) { this.estimatedOutputUnit = estimatedOutputUnit; }
    public BigDecimal getEstimatedProductionRate() { return estimatedProductionRate; }
    public void setEstimatedProductionRate(BigDecimal estimatedProductionRate) { this.estimatedProductionRate = estimatedProductionRate; }
    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) { this.estimatedDurationMinutes = estimatedDurationMinutes; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
