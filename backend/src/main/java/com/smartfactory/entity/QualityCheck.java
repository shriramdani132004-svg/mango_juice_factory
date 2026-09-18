package com.smartfactory.entity;

import com.smartfactory.enums.QualityCheckStatus;
import com.smartfactory.enums.QualityCheckType;
import com.smartfactory.enums.QualityResult;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "quality_checks")
public class QualityCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quality_check_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private ProductionOrder productionOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id", nullable = false)
    private ProductionPhase phase;

    @Column(name = "check_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private QualityCheckType checkType;

    @Column(name = "parameter_name", nullable = false, length = 100)
    private String parameterName;

    @Column(name = "observed_value", precision = 16, scale = 6)
    private BigDecimal observedValue;

    @Column(name = "expected_min", precision = 16, scale = 6)
    private BigDecimal expectedMin;

    @Column(name = "expected_max", precision = 16, scale = 6)
    private BigDecimal expectedMax;

    @Column(name = "expected_value", precision = 16, scale = 6)
    private BigDecimal expectedValue;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private QualityCheckStatus status = QualityCheckStatus.PENDING;

    @Column(name = "result")
    @Enumerated(EnumType.STRING)
    private QualityResult result;

    @Column(name = "mandatory", nullable = false)
    private Boolean mandatory = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspector_id")
    private User inspector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public QualityCheck() {}

    public QualityCheck(Batch batch, ProductionOrder order, ProductionPhase phase,
                        QualityCheckType checkType, String parameterName,
                        BigDecimal expectedMin, BigDecimal expectedMax, Boolean mandatory) {
        this.batch = batch;
        this.productionOrder = order;
        this.phase = phase;
        this.checkType = checkType;
        this.parameterName = parameterName;
        this.expectedMin = expectedMin;
        this.expectedMax = expectedMax;
        this.mandatory = mandatory;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Batch getBatch() { return batch; }
    public void setBatch(Batch batch) { this.batch = batch; }
    public ProductionOrder getProductionOrder() { return productionOrder; }
    public void setProductionOrder(ProductionOrder order) { this.productionOrder = order; }
    public ProductionPhase getPhase() { return phase; }
    public void setPhase(ProductionPhase phase) { this.phase = phase; }
    public QualityCheckType getCheckType() { return checkType; }
    public void setCheckType(QualityCheckType checkType) { this.checkType = checkType; }
    public String getParameterName() { return parameterName; }
    public void setParameterName(String parameterName) { this.parameterName = parameterName; }
    public BigDecimal getObservedValue() { return observedValue; }
    public void setObservedValue(BigDecimal observedValue) { this.observedValue = observedValue; }
    public BigDecimal getExpectedMin() { return expectedMin; }
    public void setExpectedMin(BigDecimal expectedMin) { this.expectedMin = expectedMin; }
    public BigDecimal getExpectedMax() { return expectedMax; }
    public void setExpectedMax(BigDecimal expectedMax) { this.expectedMax = expectedMax; }
    public BigDecimal getExpectedValue() { return expectedValue; }
    public void setExpectedValue(BigDecimal expectedValue) { this.expectedValue = expectedValue; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public QualityCheckStatus getStatus() { return status; }
    public void setStatus(QualityCheckStatus status) { this.status = status; }
    public QualityResult getResult() { return result; }
    public void setResult(QualityResult result) { this.result = result; }
    public Boolean getMandatory() { return mandatory; }
    public void setMandatory(Boolean mandatory) { this.mandatory = mandatory; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public User getInspector() { return inspector; }
    public void setInspector(User inspector) { this.inspector = inspector; }
    public Machine getMachine() { return machine; }
    public void setMachine(Machine machine) { this.machine = machine; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
}
