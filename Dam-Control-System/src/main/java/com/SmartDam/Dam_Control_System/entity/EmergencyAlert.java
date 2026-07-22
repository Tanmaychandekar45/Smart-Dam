package com.SmartDam.Dam_Control_System.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_alert")
public class EmergencyAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String damId;
    private String priority; // LOW, MEDIUM, HIGH, CRITICAL
    
    @Column(length = 1000)
    private String message;
    
    private LocalDateTime timestamp;
    private String shiftOfficerName;
    private boolean resolved;

    public EmergencyAlert() {}

    public EmergencyAlert(Long id, String damId, String priority, String message, LocalDateTime timestamp, String shiftOfficerName, boolean resolved) {
        this.id = id;
        this.damId = damId;
        this.priority = priority;
        this.message = message;
        this.timestamp = timestamp;
        this.shiftOfficerName = shiftOfficerName;
        this.resolved = resolved;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDamId() { return damId; }
    public void setDamId(String damId) { this.damId = damId; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getShiftOfficerName() { return shiftOfficerName; }
    public void setShiftOfficerName(String shiftOfficerName) { this.shiftOfficerName = shiftOfficerName; }

    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
}
