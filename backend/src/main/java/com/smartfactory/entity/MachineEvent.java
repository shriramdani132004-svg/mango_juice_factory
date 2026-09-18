package com.smartfactory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "machine_events")
public class MachineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "temperature", precision = 6, scale = 2)
    private BigDecimal temperature;

    @Column(name = "rpm", precision = 8, scale = 2)
    private BigDecimal rpm;

    @Column(name = "vibration", precision = 6, scale = 3)
    private BigDecimal vibration;

    @Column(name = "power_consumption", precision = 8, scale = 2)
    private BigDecimal powerConsumption;

    @Column(name = "state_from", length = 20)
    private String stateFrom;

    @Column(name = "state_to", length = 20)
    private String stateTo;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public MachineEvent() {}

    public MachineEvent(Machine machine, String eventType, String message) {
        this.machine = machine;
        this.eventType = eventType;
        this.message = message;
    }

    public Long getId() { return id; }
    public Machine getMachine() { return machine; }
    public void setMachine(Machine machine) { this.machine = machine; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public BigDecimal getTemperature() { return temperature; }
    public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }
    public BigDecimal getRpm() { return rpm; }
    public void setRpm(BigDecimal rpm) { this.rpm = rpm; }
    public BigDecimal getVibration() { return vibration; }
    public void setVibration(BigDecimal vibration) { this.vibration = vibration; }
    public BigDecimal getPowerConsumption() { return powerConsumption; }
    public void setPowerConsumption(BigDecimal powerConsumption) { this.powerConsumption = powerConsumption; }
    public String getStateFrom() { return stateFrom; }
    public void setStateFrom(String stateFrom) { this.stateFrom = stateFrom; }
    public String getStateTo() { return stateTo; }
    public void setStateTo(String stateTo) { this.stateTo = stateTo; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
