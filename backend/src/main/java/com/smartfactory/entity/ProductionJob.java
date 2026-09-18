package com.smartfactory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "production_jobs")
public class ProductionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id", nullable = false)
    private ProductionPhase phase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "QUEUED";

    @Column(name = "input_quantity", precision = 12, scale = 4)
    private BigDecimal inputQuantity;

    @Column(name = "expected_output", precision = 12, scale = 4)
    private BigDecimal expectedOutput;

    @Column(name = "actual_output", precision = 12, scale = 4)
    private BigDecimal actualOutput;

    @Column(name = "production_rate", precision = 10, scale = 4)
    private BigDecimal productionRate;

    @Column(name = "progress", precision = 5, scale = 2)
    private BigDecimal progress = BigDecimal.ZERO;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

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

    public ProductionJob() {}

    public ProductionJob(Batch batch, ProductionPhase phase, Machine machine,
                          BigDecimal inputQuantity, BigDecimal expectedOutput,
                          BigDecimal productionRate) {
        this.batch = batch;
        this.phase = phase;
        this.machine = machine;
        this.inputQuantity = inputQuantity;
        this.expectedOutput = expectedOutput;
        this.productionRate = productionRate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Batch getBatch() { return batch; }
    public void setBatch(Batch batch) { this.batch = batch; }
    public ProductionPhase getPhase() { return phase; }
    public void setPhase(ProductionPhase phase) { this.phase = phase; }
    public Machine getMachine() { return machine; }
    public void setMachine(Machine machine) { this.machine = machine; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getInputQuantity() { return inputQuantity; }
    public void setInputQuantity(BigDecimal inputQuantity) { this.inputQuantity = inputQuantity; }
    public BigDecimal getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(BigDecimal expectedOutput) { this.expectedOutput = expectedOutput; }
    public BigDecimal getActualOutput() { return actualOutput; }
    public void setActualOutput(BigDecimal actualOutput) { this.actualOutput = actualOutput; }
    public BigDecimal getProductionRate() { return productionRate; }
    public void setProductionRate(BigDecimal productionRate) { this.productionRate = productionRate; }
    public BigDecimal getProgress() { return progress; }
    public void setProgress(BigDecimal progress) { this.progress = progress; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
