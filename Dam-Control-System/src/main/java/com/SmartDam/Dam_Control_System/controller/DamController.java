package com.SmartDam.Dam_Control_System.controller;

import com.SmartDam.Dam_Control_System.entity.ControlLog;
import com.SmartDam.Dam_Control_System.entity.ReservoirState;
import com.SmartDam.Dam_Control_System.repository.ControlLogRepository;
import com.SmartDam.Dam_Control_System.repository.ReservoirStateRepository;
import com.SmartDam.Dam_Control_System.service.DamControlEngineService;
import com.SmartDam.Dam_Control_System.service.WeatherForecastService;
import com.SmartDam.Dam_Control_System.service.AiSuggestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dam")
@CrossOrigin(origins = "*") // Allow frontend requests to interact during local development
public class DamController {

    private static final Logger log = LoggerFactory.getLogger(DamController.class);

    private final ReservoirStateRepository reservoirStateRepository;
    private final ControlLogRepository controlLogRepository;
    private final DamControlEngineService damControlEngineService;
    private final WeatherForecastService weatherForecastService;
    private final AiSuggestionService aiSuggestionService;
    private final com.SmartDam.Dam_Control_System.repository.EmergencyAlertRepository emergencyAlertRepository;

    public DamController(ReservoirStateRepository reservoirStateRepository,
                         ControlLogRepository controlLogRepository,
                         DamControlEngineService damControlEngineService,
                         WeatherForecastService weatherForecastService,
                         AiSuggestionService aiSuggestionService,
                         com.SmartDam.Dam_Control_System.repository.EmergencyAlertRepository emergencyAlertRepository) {
        this.reservoirStateRepository = reservoirStateRepository;
        this.controlLogRepository = controlLogRepository;
        this.damControlEngineService = damControlEngineService;
        this.weatherForecastService = weatherForecastService;
        this.aiSuggestionService = aiSuggestionService;
        this.emergencyAlertRepository = emergencyAlertRepository;
    }

    /**
     * GET /api/v1/dam/status
     * Returns current dam state and last control decision log for the default dam (Erai).
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDamStatus() {
        return getDamStatus("erai");
    }

    /**
     * GET /api/v1/dam/{damId}/status
     * Returns current state and last decision log for the specified damId.
     */
    @GetMapping("/{damId}/status")
    public ResponseEntity<Map<String, Object>> getDamStatus(@PathVariable String damId) {
        com.SmartDam.Dam_Control_System.entity.DamMetadata meta = com.SmartDam.Dam_Control_System.entity.DamMetadata.get(damId);
        
        // Retrieve or initialize default Reservoir State if DB is empty for this dam
        ReservoirState state = reservoirStateRepository.findFirstByDamIdOrderByTimestampDesc(meta.getId())
                .orElseGet(() -> {
                    log.info("No reservoir state found in database for dam {}. Initializing default state...", meta.getName());
                    ReservoirState defaultState = ReservoirState.builder()
                            .damId(meta.getId())
                            .timestamp(LocalDateTime.now())
                            .currentVolumeM3(meta.getMaxCapacityM3() * 0.8) // ~80% Capacity
                            .waterLevelMeters(meta.getMaxWaterLevelMeters() * 0.85) // Approximate water level
                            .currentOutflowM3s(0.0)
                            .gateOpenPercentage(0.0)
                            .build();
                    return reservoirStateRepository.save(defaultState);
                });

        ControlLog latestLog = controlLogRepository.findFirstByDamIdOrderByTimestampDesc(meta.getId())
                .orElseGet(() -> {
                    log.info("No control log found in database for dam {}. Initializing default log...", meta.getName());
                    ControlLog defaultLog = ControlLog.builder()
                            .damId(meta.getId())
                            .timestamp(LocalDateTime.now())
                            .forecastPrecipitationMm(0.0)
                            .predictedInflowM3(0.0)
                            .recommendedOutflowM3s(0.0)
                            .floodAlertTriggered(false)
                            .statusMessage("System initialized. Standing by for weather telemetry updates.")
                            .build();
                    return controlLogRepository.save(defaultLog);
                });

        Map<String, Object> response = new HashMap<>();
        response.put("state", state);
        response.put("latestLog", latestLog);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/dam/update-state
     */
    @PostMapping("/update-state")
    public ResponseEntity<ControlLog> updateReservoirState(@RequestBody UpdateStateRequest request) {
        return updateReservoirState("erai", request);
    }

    /**
     * POST /api/v1/dam/{damId}/update-state
     * Payload: { "currentVolumeM3": 195000000.0, "waterLevelMeters": 21.5 }
     * Updates reservoir state, fetches forecast rain, runs engine logic, returns decision.
     */
    @PostMapping("/{damId}/update-state")
    public ResponseEntity<ControlLog> updateReservoirState(@PathVariable String damId, @RequestBody UpdateStateRequest request) {
        com.SmartDam.Dam_Control_System.entity.DamMetadata meta = com.SmartDam.Dam_Control_System.entity.DamMetadata.get(damId);
        log.info("Received manual state update request for {}: Volume={}, Level={}",
                meta.getName(), request.getCurrentVolumeM3(), request.getWaterLevelMeters());

        double currentVolume = request.getCurrentVolumeM3();
        double level = request.getWaterLevelMeters();

        // Retrieve or create state record for this dam
        ReservoirState state = reservoirStateRepository.findFirstByDamIdOrderByTimestampDesc(meta.getId())
                .orElse(new ReservoirState());

        state.setDamId(meta.getId());
        state.setTimestamp(LocalDateTime.now());
        state.setCurrentVolumeM3(currentVolume);
        state.setWaterLevelMeters(level);
        reservoirStateRepository.save(state);

        // Fetch weather forecast rain sum
        double forecastRainfall = weatherForecastService.fetchThreeDayPrecipitation(meta.getLatitude(), meta.getLongitude());

        // Run decision engine
        ControlLog updatedDecision = damControlEngineService.evaluateAndExecuteControlLogic(meta.getId(), currentVolume, forecastRainfall);

        return ResponseEntity.ok(updatedDecision);
    }

    /**
     * GET /api/v1/dam/forecast-eval
     */
    @GetMapping("/forecast-eval")
    public ResponseEntity<ControlLog> runForecastSimulation(
            @RequestParam double currentVolumeM3,
            @RequestParam double forecastRainfallMm) {
        return runForecastSimulation("erai", currentVolumeM3, forecastRainfallMm);
    }

    /**
     * GET /api/v1/dam/{damId}/forecast-eval
     * Query Parameters: currentVolumeM3, forecastRainfallMm
     * Triggers on-demand evaluation simulation. Does not write/persist simulation results to DB.
     */
    @GetMapping("/{damId}/forecast-eval")
    public ResponseEntity<ControlLog> runForecastSimulation(
            @PathVariable String damId,
            @RequestParam double currentVolumeM3,
            @RequestParam double forecastRainfallMm) {
        com.SmartDam.Dam_Control_System.entity.DamMetadata meta = com.SmartDam.Dam_Control_System.entity.DamMetadata.get(damId);
        log.info("Received simulation request for {}: Volume={}, Rainfall={}", meta.getName(), currentVolumeM3, forecastRainfallMm);
        ControlLog simulatedLog = damControlEngineService.simulateControlLogic(meta.getId(), currentVolumeM3, forecastRainfallMm);
        return ResponseEntity.ok(simulatedLog);
    }

    // DTO for state update requests
    public static class UpdateStateRequest {
        private double currentVolumeM3;
        private double waterLevelMeters;

        public double getCurrentVolumeM3() {
            return currentVolumeM3;
        }

        public void setCurrentVolumeM3(double currentVolumeM3) {
            this.currentVolumeM3 = currentVolumeM3;
        }

        public double getWaterLevelMeters() {
            return waterLevelMeters;
        }

        public void setWaterLevelMeters(double waterLevelMeters) {
            this.waterLevelMeters = waterLevelMeters;
        }
    }

    /**
     * GET /api/v1/dam/ai-recommendation
     */
    @GetMapping("/ai-recommendation")
    public ResponseEntity<com.SmartDam.Dam_Control_System.dto.AiRecommendationResponse> getAiRecommendation() {
        return getAiRecommendation("erai");
    }

    /**
     * GET /api/v1/dam/{damId}/ai-recommendation
     */
    @GetMapping("/{damId}/ai-recommendation")
    public ResponseEntity<com.SmartDam.Dam_Control_System.dto.AiRecommendationResponse> getAiRecommendation(@PathVariable String damId) {
        log.info("Received request for AI Recommendation for dam: {}", damId);
        com.SmartDam.Dam_Control_System.dto.AiRecommendationResponse recommendation = aiSuggestionService.generateRecommendation(damId);
        return ResponseEntity.ok(recommendation);
    }

    /**
     * POST /api/v1/dam/{damId}/submit-decision
     */
    @PostMapping("/{damId}/submit-decision")
    public ResponseEntity<ControlLog> submitDecision(@PathVariable String damId) {
        log.info("Submitting latest decision log to higher authority for dam: {}", damId);
        com.SmartDam.Dam_Control_System.entity.DamMetadata meta = com.SmartDam.Dam_Control_System.entity.DamMetadata.get(damId);
        
        ControlLog logEntry = controlLogRepository.findFirstByDamIdOrderByTimestampDesc(meta.getId())
                .orElseThrow(() -> new IllegalArgumentException("No log found to submit."));
        
        logEntry.setApprovalStatus("PENDING_AUTHORITY");
        controlLogRepository.save(logEntry);
        return ResponseEntity.ok(logEntry);
    }

    /**
     * POST /api/v1/dam/{damId}/authority-action
     */
    @PostMapping("/{damId}/authority-action")
    public ResponseEntity<ControlLog> authorityAction(
            @PathVariable String damId,
            @RequestParam String action,
            @RequestParam(required = false) Double manualOutflow) {
        log.info("Authority action received for dam: {}. Action: {}, ManualOutflow: {}", damId, action, manualOutflow);
        com.SmartDam.Dam_Control_System.entity.DamMetadata meta = com.SmartDam.Dam_Control_System.entity.DamMetadata.get(damId);
        
        ControlLog logEntry = controlLogRepository.findFirstByDamIdOrderByTimestampDesc(meta.getId())
                .orElseThrow(() -> new IllegalArgumentException("No log found to act on."));

        ReservoirState state = reservoirStateRepository.findFirstByDamIdOrderByTimestampDesc(meta.getId())
                .orElseThrow(() -> new IllegalArgumentException("No reservoir state found."));

        if ("APPROVE".equalsIgnoreCase(action)) {
            logEntry.setApprovalStatus("APPROVED");
            logEntry.setStatusMessage("[APPROVED BY AUTHORITY] " + logEntry.getStatusMessage());
            
            double targetOutflow = logEntry.getRecommendedOutflowM3s();
            state.setCurrentOutflowM3s(targetOutflow);
            
            double openPercent = (targetOutflow / meta.getMaxSafeDischargeM3s()) * 100.0;
            state.setGateOpenPercentage(Math.min(100.0, Math.max(0.0, openPercent)));
            
            reservoirStateRepository.save(state);
        } else {
            logEntry.setApprovalStatus("REJECTED");
            double outflowOverride = (manualOutflow != null) ? manualOutflow : 0.0;
            logEntry.setRecommendedOutflowM3s(outflowOverride);
            logEntry.setStatusMessage(String.format("[OVERRIDDEN BY AUTHORITY] Outflow locked at %.2f m³/s manually.", outflowOverride));
            
            state.setCurrentOutflowM3s(outflowOverride);
            double openPercent = (outflowOverride / meta.getMaxSafeDischargeM3s()) * 100.0;
            state.setGateOpenPercentage(Math.min(100.0, Math.max(0.0, openPercent)));
            
            reservoirStateRepository.save(state);
        }

        controlLogRepository.save(logEntry);
        return ResponseEntity.ok(logEntry);
    }

    /**
     * POST /api/v1/dam/{damId}/emergency-alert
     */
    @PostMapping("/{damId}/emergency-alert")
    public ResponseEntity<com.SmartDam.Dam_Control_System.entity.EmergencyAlert> createEmergencyAlert(
            @PathVariable String damId,
            @RequestParam String priority,
            @RequestParam String message,
            @RequestParam String shiftOfficerName) {
        log.info("Creating emergency alert for dam: {}. Priority: {}, Officer: {}", damId, priority, shiftOfficerName);
        com.SmartDam.Dam_Control_System.entity.EmergencyAlert alert = new com.SmartDam.Dam_Control_System.entity.EmergencyAlert(
                null, damId, priority, message, LocalDateTime.now(), shiftOfficerName, false
        );
        emergencyAlertRepository.save(alert);
        return ResponseEntity.ok(alert);
    }

    /**
     * GET /api/v1/dam/alerts
     */
    @GetMapping("/alerts")
    public ResponseEntity<java.util.List<com.SmartDam.Dam_Control_System.entity.EmergencyAlert>> getActiveAlerts() {
        log.info("Fetching all active/unresolved emergency alerts");
        java.util.List<com.SmartDam.Dam_Control_System.entity.EmergencyAlert> alerts = emergencyAlertRepository.findByResolvedOrderByTimestampDesc(false);
        return ResponseEntity.ok(alerts);
    }

    /**
     * POST /api/v1/dam/alert/{id}/resolve
     */
    @PostMapping("/alert/{id}/resolve")
    public ResponseEntity<Void> resolveAlert(@PathVariable Long id) {
        log.info("Resolving emergency alert ID: {}", id);
        com.SmartDam.Dam_Control_System.entity.EmergencyAlert alert = emergencyAlertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        alert.setResolved(true);
        emergencyAlertRepository.save(alert);
        return ResponseEntity.ok().build();
    }
}
