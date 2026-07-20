package com.SmartDam.Dam_Control_System.controller;

import com.SmartDam.Dam_Control_System.entity.ControlLog;
import com.SmartDam.Dam_Control_System.entity.ReservoirState;
import com.SmartDam.Dam_Control_System.repository.ControlLogRepository;
import com.SmartDam.Dam_Control_System.repository.ReservoirStateRepository;
import com.SmartDam.Dam_Control_System.service.DamControlEngineService;
import com.SmartDam.Dam_Control_System.service.WeatherForecastService;
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

    public DamController(ReservoirStateRepository reservoirStateRepository,
                         ControlLogRepository controlLogRepository,
                         DamControlEngineService damControlEngineService,
                         WeatherForecastService weatherForecastService) {
        this.reservoirStateRepository = reservoirStateRepository;
        this.controlLogRepository = controlLogRepository;
        this.damControlEngineService = damControlEngineService;
        this.weatherForecastService = weatherForecastService;
    }

    /**
     * GET /api/v1/dam/status
     * Returns current dam state and last control decision log.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDamStatus() {
        // Retrieve or initialize default Reservoir State if DB is empty
        ReservoirState state = reservoirStateRepository.findFirstByOrderByTimestampDesc()
                .orElseGet(() -> {
                    log.info("No reservoir state found in database. Initializing default state...");
                    ReservoirState defaultState = ReservoirState.builder()
                            .timestamp(LocalDateTime.now())
                            .currentVolumeM3(181200000.0) // ~80% Capacity
                            .waterLevelMeters(20.06)
                            .currentOutflowM3s(0.0)
                            .gateOpenPercentage(0.0)
                            .build();
                    return reservoirStateRepository.save(defaultState);
                });

        ControlLog latestLog = controlLogRepository.findFirstByOrderByTimestampDesc()
                .orElseGet(() -> {
                    log.info("No control log found in database. Initializing default log...");
                    ControlLog defaultLog = ControlLog.builder()
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
     * Payload: { "currentVolumeM3": 195000000.0, "waterLevelMeters": 21.5 }
     * Updates reservoir state, fetches forecast rain, runs engine logic, returns decision.
     */
    @PostMapping("/update-state")
    public ResponseEntity<ControlLog> updateReservoirState(@RequestBody UpdateStateRequest request) {
        log.info("Received manual state update request: Volume={}, Level={}",
                request.getCurrentVolumeM3(), request.getWaterLevelMeters());

        // Update current reservoir state metadata in DB
        double currentVolume = request.getCurrentVolumeM3();
        double level = request.getWaterLevelMeters();

        // Retrieve or create state record
        ReservoirState state = reservoirStateRepository.findFirstByOrderByTimestampDesc()
                .orElse(new ReservoirState());

        state.setTimestamp(LocalDateTime.now());
        state.setCurrentVolumeM3(currentVolume);
        state.setWaterLevelMeters(level);
        reservoirStateRepository.save(state);

        // Fetch weather forecast rain sum
        double forecastRainfall = weatherForecastService.fetchThreeDayPrecipitation();

        // Run decision engine
        ControlLog updatedDecision = damControlEngineService.evaluateAndExecuteControlLogic(currentVolume, forecastRainfall);

        return ResponseEntity.ok(updatedDecision);
    }

    /**
     * GET /api/v1/dam/forecast-eval
     * Query Parameters: currentVolumeM3, forecastRainfallMm
     * Triggers on-demand evaluation simulation. Does not write/persist simulation results to DB.
     */
    @GetMapping("/forecast-eval")
    public ResponseEntity<ControlLog> runForecastSimulation(
            @RequestParam double currentVolumeM3,
            @RequestParam double forecastRainfallMm) {
        log.info("Received simulation request: Volume={}, Rainfall={}", currentVolumeM3, forecastRainfallMm);
        ControlLog simulatedLog = damControlEngineService.simulateControlLogic(currentVolumeM3, forecastRainfallMm);
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
}
