package com.smartfactory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "machine_types")
public class MachineType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "type_id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "capability", nullable = false, length = 30)
    private String capability;

    @Column(name = "default_capacity", nullable = false, precision = 10, scale = 2)
    private BigDecimal defaultCapacity = BigDecimal.ONE;

    @Column(name = "default_production_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal defaultProductionRate = BigDecimal.ONE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public MachineType() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCapability() { return capability; }
    public void setCapability(String capability) { this.capability = capability; }
    public BigDecimal getDefaultCapacity() { return defaultCapacity; }
    public void setDefaultCapacity(BigDecimal defaultCapacity) { this.defaultCapacity = defaultCapacity; }
    public BigDecimal getDefaultProductionRate() { return defaultProductionRate; }
    public void setDefaultProductionRate(BigDecimal defaultProductionRate) { this.defaultProductionRate = defaultProductionRate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
