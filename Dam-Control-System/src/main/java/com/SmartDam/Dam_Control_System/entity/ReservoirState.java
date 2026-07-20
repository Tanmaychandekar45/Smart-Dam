package com.SmartDam.Dam_Control_System.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservoir_state")
public class ReservoirState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;
    private double currentVolumeM3;
    private double waterLevelMeters;
    private double currentOutflowM3s;
    private double gateOpenPercentage;

    public ReservoirState() {}

    public ReservoirState(Long id, LocalDateTime timestamp, double currentVolumeM3, double waterLevelMeters, double currentOutflowM3s, double gateOpenPercentage) {
        this.id = id;
        this.timestamp = timestamp;
        this.currentVolumeM3 = currentVolumeM3;
        this.waterLevelMeters = waterLevelMeters;
        this.currentOutflowM3s = currentOutflowM3s;
        this.gateOpenPercentage = gateOpenPercentage;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public double getCurrentVolumeM3() { return currentVolumeM3; }
    public void setCurrentVolumeM3(double currentVolumeM3) { this.currentVolumeM3 = currentVolumeM3; }

    public double getWaterLevelMeters() { return waterLevelMeters; }
    public void setWaterLevelMeters(double waterLevelMeters) { this.waterLevelMeters = waterLevelMeters; }

    public double getCurrentOutflowM3s() { return currentOutflowM3s; }
    public void setCurrentOutflowM3s(double currentOutflowM3s) { this.currentOutflowM3s = currentOutflowM3s; }

    public double getGateOpenPercentage() { return gateOpenPercentage; }
    public void setGateOpenPercentage(double gateOpenPercentage) { this.gateOpenPercentage = gateOpenPercentage; }

    public static ReservoirStateBuilder builder() {
        return new ReservoirStateBuilder();
    }

    public static class ReservoirStateBuilder {
        private Long id;
        private LocalDateTime timestamp;
        private double currentVolumeM3;
        private double waterLevelMeters;
        private double currentOutflowM3s;
        private double gateOpenPercentage;

        public ReservoirStateBuilder id(Long id) { this.id = id; return this; }
        public ReservoirStateBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
        public ReservoirStateBuilder currentVolumeM3(double currentVolumeM3) { this.currentVolumeM3 = currentVolumeM3; return this; }
        public ReservoirStateBuilder waterLevelMeters(double waterLevelMeters) { this.waterLevelMeters = waterLevelMeters; return this; }
        public ReservoirStateBuilder currentOutflowM3s(double currentOutflowM3s) { this.currentOutflowM3s = currentOutflowM3s; return this; }
        public ReservoirStateBuilder gateOpenPercentage(double gateOpenPercentage) { this.gateOpenPercentage = gateOpenPercentage; return this; }

        public ReservoirState build() {
            return new ReservoirState(id, timestamp, currentVolumeM3, waterLevelMeters, currentOutflowM3s, gateOpenPercentage);
        }
    }
}
