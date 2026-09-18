package com.smartfactory.entity;

import com.smartfactory.enums.InventoryTransactionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transactions")
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "transaction_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private InventoryTransactionType transactionType;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "previous_quantity", precision = 12, scale = 4)
    private BigDecimal previousQuantity;

    @Column(name = "resulting_quantity", precision = 12, scale = 4)
    private BigDecimal resultingQuantity;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reason", length = 200)
    private String reason;

    @Column(name = "notes")
    private String notes;

    @Column(name = "performed_by")
    private Long performedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public InventoryTransaction() {}

    public InventoryTransaction(Material material, InventoryTransactionType type, BigDecimal quantity,
                                 String unit, BigDecimal previousQuantity, BigDecimal resultingQuantity) {
        this.material = material;
        this.transactionType = type;
        this.quantity = quantity;
        this.unit = unit;
        this.previousQuantity = previousQuantity;
        this.resultingQuantity = resultingQuantity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
    public InventoryTransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(InventoryTransactionType transactionType) { this.transactionType = transactionType; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getPreviousQuantity() { return previousQuantity; }
    public void setPreviousQuantity(BigDecimal previousQuantity) { this.previousQuantity = previousQuantity; }
    public BigDecimal getResultingQuantity() { return resultingQuantity; }
    public void setResultingQuantity(BigDecimal resultingQuantity) { this.resultingQuantity = resultingQuantity; }
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Long getPerformedBy() { return performedBy; }
    public void setPerformedBy(Long performedBy) { this.performedBy = performedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
