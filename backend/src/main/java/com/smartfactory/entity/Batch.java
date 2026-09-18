package com.smartfactory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "batches")
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_id")
    private Long id;

    @Column(name = "batch_code", nullable = false, unique = true, length = 40)
    private String batchCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private ProductionOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_batch_id")
    private Batch parentBatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material;

    @Column(name = "input_quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal inputQuantity = BigDecimal.ZERO;

    @Column(name = "output_quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal outputQuantity = BigDecimal.ZERO;

    @Column(name = "waste_quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal wasteQuantity = BigDecimal.ZERO;

    @Column(name = "rejected_quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal rejectedQuantity = BigDecimal.ZERO;

    @Column(name = "current_phase", nullable = false)
    private Integer currentPhase = 0;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

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

    public Batch() {}

    public Batch(String batchCode, ProductionOrder order, Material material,
                 BigDecimal inputQuantity) {
        this.batchCode = batchCode;
        this.order = order;
        this.material = material;
        this.inputQuantity = inputQuantity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }
    public ProductionOrder getOrder() { return order; }
    public void setOrder(ProductionOrder order) { this.order = order; }
    public Batch getParentBatch() { return parentBatch; }
    public void setParentBatch(Batch parentBatch) { this.parentBatch = parentBatch; }
    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
    public BigDecimal getInputQuantity() { return inputQuantity; }
    public void setInputQuantity(BigDecimal inputQuantity) { this.inputQuantity = inputQuantity; }
    public BigDecimal getOutputQuantity() { return outputQuantity; }
    public void setOutputQuantity(BigDecimal outputQuantity) { this.outputQuantity = outputQuantity; }
    public BigDecimal getWasteQuantity() { return wasteQuantity; }
    public void setWasteQuantity(BigDecimal wasteQuantity) { this.wasteQuantity = wasteQuantity; }
    public BigDecimal getRejectedQuantity() { return rejectedQuantity; }
    public void setRejectedQuantity(BigDecimal rejectedQuantity) { this.rejectedQuantity = rejectedQuantity; }
    public Integer getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(Integer currentPhase) { this.currentPhase = currentPhase; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
