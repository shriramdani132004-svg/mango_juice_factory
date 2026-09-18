package com.smartfactory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "production_order_requirements")
public class ProductionOrderRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "requirement_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private ProductionOrder order;

    @Column(name = "requirement_type", nullable = false, length = 30)
    private String requirementType;

    @Column(name = "item_name", length = 100)
    private String itemName;

    @Column(name = "item_type", length = 30)
    private String itemType;

    @Column(name = "item_code", length = 50)
    private String itemCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material;

    @Column(name = "required_quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal requiredQuantity;

    @Column(name = "required_unit", nullable = false, length = 20)
    private String requiredUnit;

    @Column(name = "available_quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal availableQuantity = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 10)
    private String status = "RED";

    @Column(name = "mandatory", nullable = false)
    private Boolean mandatory = true;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

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

    public ProductionOrderRequirement() {}

    public ProductionOrderRequirement(ProductionOrder order, String requirementType,
                                       String itemName, BigDecimal requiredQuantity,
                                       String requiredUnit, BigDecimal availableQuantity,
                                       String status, Boolean mandatory, String explanation) {
        this.order = order;
        this.requirementType = requirementType;
        this.itemName = itemName;
        this.requiredQuantity = requiredQuantity;
        this.requiredUnit = requiredUnit;
        this.availableQuantity = availableQuantity;
        this.status = status;
        this.mandatory = mandatory;
        this.explanation = explanation;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ProductionOrder getOrder() { return order; }
    public void setOrder(ProductionOrder order) { this.order = order; }
    public String getRequirementType() { return requirementType; }
    public void setRequirementType(String requirementType) { this.requirementType = requirementType; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
    public BigDecimal getRequiredQuantity() { return requiredQuantity; }
    public void setRequiredQuantity(BigDecimal requiredQuantity) { this.requiredQuantity = requiredQuantity; }
    public String getRequiredUnit() { return requiredUnit; }
    public void setRequiredUnit(String requiredUnit) { this.requiredUnit = requiredUnit; }
    public BigDecimal getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(BigDecimal availableQuantity) { this.availableQuantity = availableQuantity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getMandatory() { return mandatory; }
    public void setMandatory(Boolean mandatory) { this.mandatory = mandatory; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
