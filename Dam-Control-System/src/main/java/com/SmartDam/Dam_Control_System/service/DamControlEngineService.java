package com.SmartDam.Dam_Control_System.service;

import com.SmartDam.Dam_Control_System.entity.ControlLog;
import com.SmartDam.Dam_Control_System.entity.ReservoirState;
import com.SmartDam.Dam_Control_System.repository.ControlLogRepository;
import com.SmartDam.Dam_Control_System.repository.ReservoirStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class DamControlEngineService {

    private static final Logger log = LoggerFactory.getLogger(DamControlEngineService.class);

    // Constants for Erai Dam
    public static final double CATCHMENT_AREA_M2 = 439.33 * 1e6; // 439.33 km^2
    public static final double RUNOFF_COEFFICIENT = 0.60;
    public static final double MAX_CAPACITY_M3 = 226500000.0;
    public static final double WARNING_THRESHOLD_M3 = 192525000.0; // 85% of total
    public static final double MAX_SAFE_DISCHARGE_M3S = 800.0; // Q_safe

    private final ControlLogRepository controlLogRepository;
    private final ReservoirStateRepository reservoirStateRepository;

    public DamControlEngineService(ControlLogRepository controlLogRepository,
                                  ReservoirStateRepository reservoirStateRepository) {
        this.controlLogRepository = controlLogRepository;
        this.reservoirStateRepository = reservoirStateRepository;
    }

    /**
     * Core business logic engine evaluating runoff predictions, safe discharges, and updating dam states.
     */
    @Transactional
    public ControlLog evaluateAndExecuteControlLogic(double currentVolumeM3, double forecastRainfallMm) {
        log.info("Running Control Engine Evaluation - Current Volume: {} m3, Forecast Rain: {} mm",
                currentVolumeM3, forecastRainfallMm);

        // Runoff Calculation
        // Projected Inflow (m³) = (forecastRainfallMm / 1000.0) * Catchment Area * Run-off Coefficient
        double projectedInflowM3 = (forecastRainfallMm / 1000.0) * CATCHMENT_AREA_M2 * RUNOFF_COEFFICIENT;

        // Mass Balance
        double projectedVolumeM3 = currentVolumeM3 + projectedInflowM3;

        double recommendedOutflowM3s = 0.0;
        boolean floodAlertTriggered = false;
        String statusMessage;

        // Decision Logic
        if (projectedVolumeM3 > WARNING_THRESHOLD_M3) {
            double excessVolume = projectedVolumeM3 - WARNING_THRESHOLD_M3;
            // Proactive discharge needed over 24 hours: excessVolume / (24 * 3600)
            double requiredOutflowM3s = excessVolume / (24.0 * 3600.0);

            if (requiredOutflowM3s > MAX_SAFE_DISCHARGE_M3S) {
                recommendedOutflowM3s = MAX_SAFE_DISCHARGE_M3S;
                floodAlertTriggered = true;
                statusMessage = String.format("EMERGENCY: Projected volume (%.2fM m³) exceeds warning threshold. " +
                                "Required release rate (%.2f m³/s) exceeds downstream safe channel capacity (800.00 m³/s). " +
                                "Flood alert triggered for Chandrapur region.",
                        projectedVolumeM3 / 1e6, requiredOutflowM3s);
            } else {
                recommendedOutflowM3s = requiredOutflowM3s;
                floodAlertTriggered = false;
                statusMessage = String.format("Optimal proactive discharge initiated. Releasing %.2f m³/s over 24h " +
                                "to maintain reservoir capacity below the warning threshold (85%%).",
                        recommendedOutflowM3s);
            }
        } else {
            recommendedOutflowM3s = 0.0;
            floodAlertTriggered = false;
            statusMessage = String.format("Reservoir state within safe operational parameters. " +
                            "Projected Volume (%.2fM m³) is below the 85%% capacity threshold. No proactive release required.",
                    projectedVolumeM3 / 1e6);
        }

        // Save ControlLog decision
        ControlLog decisionLog = ControlLog.builder()
                .timestamp(LocalDateTime.now())
                .forecastPrecipitationMm(forecastRainfallMm)
                .predictedInflowM3(projectedInflowM3)
                .recommendedOutflowM3s(recommendedOutflowM3s)
                .floodAlertTriggered(floodAlertTriggered)
                .statusMessage(statusMessage)
                .build();

        ControlLog savedLog = controlLogRepository.save(decisionLog);
        log.info("Persisted Decision Control Log. ID: {}, Alert: {}, Recommended Release: {} m3/s",
                savedLog.getId(), savedLog.isFloodAlertTriggered(), savedLog.getRecommendedOutflowM3s());

        // Update the ReservoirState (or create one if updating via background/API scheduler flow)
        // Set gate open percentage proportional to discharge (Q_rec / Q_safe * 100%)
        double gateOpenPercentage = (recommendedOutflowM3s / MAX_SAFE_DISCHARGE_M3S) * 100.0;
        final double finalOutflow = recommendedOutflowM3s;
        
        // Find latest reservoir state to update or insert a new one
        reservoirStateRepository.findFirstByOrderByTimestampDesc()
                .ifPresentOrElse(
                        state -> {
                            state.setTimestamp(LocalDateTime.now());
                            state.setCurrentVolumeM3(currentVolumeM3);
                            // Estimate water level linearly for demonstration: max capacity corresponds to e.g. 23.6m, let's keep it proportionate
                            double estimatedLevel = (currentVolumeM3 / MAX_CAPACITY_M3) * 23.6; 
                            state.setWaterLevelMeters(Math.round(estimatedLevel * 100.0) / 100.0);
                            state.setCurrentOutflowM3s(finalOutflow);
                            state.setGateOpenPercentage(Math.round(gateOpenPercentage * 100.0) / 100.0);
                            reservoirStateRepository.save(state);
                        },
                        () -> {
                            double estimatedLevel = (currentVolumeM3 / MAX_CAPACITY_M3) * 23.6;
                            ReservoirState newState = ReservoirState.builder()
                                    .timestamp(LocalDateTime.now())
                                    .currentVolumeM3(currentVolumeM3)
                                    .waterLevelMeters(Math.round(estimatedLevel * 100.0) / 100.0)
                                    .currentOutflowM3s(finalOutflow)
                                    .gateOpenPercentage(Math.round(gateOpenPercentage * 100.0) / 100.0)
                                    .build();
                            reservoirStateRepository.save(newState);
                        }
                );

        return savedLog;
    }

    /**
     * Executes scenario simulation on-demand without database side effects.
     */
    public ControlLog simulateControlLogic(double currentVolumeM3, double forecastRainfallMm) {
        double projectedInflowM3 = (forecastRainfallMm / 1000.0) * CATCHMENT_AREA_M2 * RUNOFF_COEFFICIENT;
        double projectedVolumeM3 = currentVolumeM3 + projectedInflowM3;

        double recommendedOutflowM3s;
        boolean floodAlertTriggered;
        String statusMessage;

        if (projectedVolumeM3 > WARNING_THRESHOLD_M3) {
            double excessVolume = projectedVolumeM3 - WARNING_THRESHOLD_M3;
            double requiredOutflowM3s = excessVolume / (24.0 * 3600.0);

            if (requiredOutflowM3s > MAX_SAFE_DISCHARGE_M3S) {
                recommendedOutflowM3s = MAX_SAFE_DISCHARGE_M3S;
                floodAlertTriggered = true;
                statusMessage = String.format("[SIMULATION] EMERGENCY: Projected volume (%.2fM m³) exceeds warning threshold. " +
                                "Required release rate (%.2f m³/s) exceeds downstream safe channel capacity (800.00 m³/s). " +
                                "Flood alert triggered for Chandrapur region.",
                        projectedVolumeM3 / 1e6, requiredOutflowM3s);
            } else {
                recommendedOutflowM3s = requiredOutflowM3s;
                floodAlertTriggered = false;
                statusMessage = String.format("[SIMULATION] Optimal proactive discharge initiated. Releasing %.2f m³/s over 24h " +
                                "to maintain reservoir capacity below the warning threshold (85%%).",
                        recommendedOutflowM3s);
            }
        } else {
            recommendedOutflowM3s = 0.0;
            floodAlertTriggered = false;
            statusMessage = String.format("[SIMULATION] Reservoir state within safe operational parameters. " +
                            "Projected Volume (%.2fM m³) is below the 85%% capacity threshold. No proactive release required.",
                    projectedVolumeM3 / 1e6);
        }

        return ControlLog.builder()
                .timestamp(LocalDateTime.now())
                .forecastPrecipitationMm(forecastRainfallMm)
                .predictedInflowM3(projectedInflowM3)
                .recommendedOutflowM3s(recommendedOutflowM3s)
                .floodAlertTriggered(floodAlertTriggered)
                .statusMessage(statusMessage)
                .build();
    }
}
