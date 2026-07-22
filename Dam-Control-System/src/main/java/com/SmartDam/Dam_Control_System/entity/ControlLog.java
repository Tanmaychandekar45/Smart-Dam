package com.SmartDam.Dam_Control_System.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "control_log")
public class ControlLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String damId;
    private LocalDateTime timestamp;
    private double forecastPrecipitationMm;
    private double predictedInflowM3;
    private double recommendedOutflowM3s;
    private boolean floodAlertTriggered;

    @Column(length = 1000)
    private String statusMessage;

    public ControlLog() {}

    public ControlLog(Long id, String damId, LocalDateTime timestamp, double forecastPrecipitationMm, double predictedInflowM3, double recommendedOutflowM3s, boolean floodAlertTriggered, String statusMessage) {
        this.id = id;
        this.damId = damId;
        this.timestamp = timestamp;
        this.forecastPrecipitationMm = forecastPrecipitationMm;
        this.predictedInflowM3 = predictedInflowM3;
        this.recommendedOutflowM3s = recommendedOutflowM3s;
        this.floodAlertTriggered = floodAlertTriggered;
        this.statusMessage = statusMessage;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDamId() { return damId; }
    public void setDamId(String damId) { this.damId = damId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public double getForecastPrecipitationMm() { return forecastPrecipitationMm; }
    public void setForecastPrecipitationMm(double forecastPrecipitationMm) { this.forecastPrecipitationMm = forecastPrecipitationMm; }

    public double getPredictedInflowM3() { return predictedInflowM3; }
    public void setPredictedInflowM3(double predictedInflowM3) { this.predictedInflowM3 = predictedInflowM3; }

    public double getRecommendedOutflowM3s() { return recommendedOutflowM3s; }
    public void setRecommendedOutflowM3s(double recommendedOutflowM3s) { this.recommendedOutflowM3s = recommendedOutflowM3s; }

    public boolean isFloodAlertTriggered() { return floodAlertTriggered; }
    public void setFloodAlertTriggered(boolean floodAlertTriggered) { this.floodAlertTriggered = floodAlertTriggered; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }

    public static ControlLogBuilder builder() {
        return new ControlLogBuilder();
    }

    public static class ControlLogBuilder {
        private Long id;
        private String damId;
        private LocalDateTime timestamp;
        private double forecastPrecipitationMm;
        private double predictedInflowM3;
        private double recommendedOutflowM3s;
        private boolean floodAlertTriggered;
        private String statusMessage;

        public ControlLogBuilder id(Long id) { this.id = id; return this; }
        public ControlLogBuilder damId(String damId) { this.damId = damId; return this; }
        public ControlLogBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
        public ControlLogBuilder forecastPrecipitationMm(double forecastPrecipitationMm) { this.forecastPrecipitationMm = forecastPrecipitationMm; return this; }
        public ControlLogBuilder predictedInflowM3(double predictedInflowM3) { this.predictedInflowM3 = predictedInflowM3; return this; }
        public ControlLogBuilder recommendedOutflowM3s(double recommendedOutflowM3s) { this.recommendedOutflowM3s = recommendedOutflowM3s; return this; }
        public ControlLogBuilder floodAlertTriggered(boolean floodAlertTriggered) { this.floodAlertTriggered = floodAlertTriggered; return this; }
        public ControlLogBuilder statusMessage(String statusMessage) { this.statusMessage = statusMessage; return this; }

        public ControlLog build() {
            return new ControlLog(id, damId, timestamp, forecastPrecipitationMm, predictedInflowM3, recommendedOutflowM3s, floodAlertTriggered, statusMessage);
        }
    }
}
