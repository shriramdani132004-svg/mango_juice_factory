package com.smartfactory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "machines")
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "machine_id")
    private Long id;

    @Column(name = "machine_code", nullable = false, unique = true, length = 30)
    private String machineCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    private MachineType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_id", nullable = false)
    private ProductionLine line;

    @Column(name = "capability", nullable = false, length = 30)
    private String capability;

    @Column(name = "capacity", nullable = false, precision = 10, scale = 2)
    private BigDecimal capacity = BigDecimal.ONE;

    @Column(name = "production_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal productionRate = BigDecimal.ONE;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "IDLE";

    @Column(name = "health_score", nullable = false)
    private Integer healthScore = 100;

    @Column(name = "temperature", precision = 6, scale = 2)
    private BigDecimal temperature;

    @Column(name = "rpm", precision = 8, scale = 2)
    private BigDecimal rpm;

    @Column(name = "vibration", precision = 6, scale = 3)
    private BigDecimal vibration;

    @Column(name = "power_consumption", precision = 8, scale = 2)
    private BigDecimal powerConsumption;

    @Column(name = "failure_count", nullable = false)
    private Integer failureCount = 0;

    @Column(name = "last_maintenance_at")
    private LocalDateTime lastMaintenanceAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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

    public Machine() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMachineCode() { return machineCode; }
    public void setMachineCode(String machineCode) { this.machineCode = machineCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public MachineType getType() { return type; }
    public void setType(MachineType type) { this.type = type; }
    public ProductionLine getLine() { return line; }
    public void setLine(ProductionLine line) { this.line = line; }
    public String getCapability() { return capability; }
    public void setCapability(String capability) { this.capability = capability; }
    public BigDecimal getCapacity() { return capacity; }
    public void setCapacity(BigDecimal capacity) { this.capacity = capacity; }
    public BigDecimal getProductionRate() { return productionRate; }
    public void setProductionRate(BigDecimal productionRate) { this.productionRate = productionRate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getHealthScore() { return healthScore; }
    public void setHealthScore(Integer healthScore) { this.healthScore = healthScore; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public BigDecimal getTemperature() { return temperature; }
    public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }
    public BigDecimal getRpm() { return rpm; }
    public void setRpm(BigDecimal rpm) { this.rpm = rpm; }
    public BigDecimal getVibration() { return vibration; }
    public void setVibration(BigDecimal vibration) { this.vibration = vibration; }
    public BigDecimal getPowerConsumption() { return powerConsumption; }
    public void setPowerConsumption(BigDecimal powerConsumption) { this.powerConsumption = powerConsumption; }
    public Integer getFailureCount() { return failureCount; }
    public void setFailureCount(Integer failureCount) { this.failureCount = failureCount; }
    public LocalDateTime getLastMaintenanceAt() { return lastMaintenanceAt; }
    public void setLastMaintenanceAt(LocalDateTime lastMaintenanceAt) { this.lastMaintenanceAt = lastMaintenanceAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
