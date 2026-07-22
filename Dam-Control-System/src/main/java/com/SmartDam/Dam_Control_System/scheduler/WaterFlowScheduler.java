package com.SmartDam.Dam_Control_System.scheduler;

import com.SmartDam.Dam_Control_System.entity.ControlLog;
import com.SmartDam.Dam_Control_System.entity.ReservoirState;
import com.SmartDam.Dam_Control_System.repository.ReservoirStateRepository;
import com.SmartDam.Dam_Control_System.service.DamControlEngineService;
import com.SmartDam.Dam_Control_System.service.WeatherForecastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class WaterFlowScheduler {

    private static final Logger log = LoggerFactory.getLogger(WaterFlowScheduler.class);

    private final ReservoirStateRepository reservoirStateRepository;
    private final WeatherForecastService weatherForecastService;
    private final DamControlEngineService damControlEngineService;

    public WaterFlowScheduler(ReservoirStateRepository reservoirStateRepository,
                              WeatherForecastService weatherForecastService,
                              DamControlEngineService damControlEngineService) {
        this.reservoirStateRepository = reservoirStateRepository;
        this.weatherForecastService = weatherForecastService;
        this.damControlEngineService = damControlEngineService;
    }

    /**
     * Hourly task to monitor current dam volume, fetch weather forecast,
     * execute decision control logic and update outflow/logs.
     * Scheduled for fixedRate = 3600000 milliseconds (1 hour).
    /**
     * Hourly task to monitor current dam volumes, fetch weather forecasts,
     * execute decision control logic and update outflow/logs for all registered dams.
     * Scheduled for fixedRate = 3600000 milliseconds (1 hour).
     */
    @Scheduled(fixedRate = 3600000)
    public void executeHourlyDamAssessment() {
        log.info("Starting automated hourly reservoir assessment for all registered dams at {}", LocalDateTime.now());

        for (String damId : com.SmartDam.Dam_Control_System.entity.DamMetadata.getRegistry().keySet()) {
            com.SmartDam.Dam_Control_System.entity.DamMetadata meta = com.SmartDam.Dam_Control_System.entity.DamMetadata.get(damId);
            try {
                // Get latest reservoir state for this dam or initialize a default state
                ReservoirState currentState = reservoirStateRepository.findFirstByDamIdOrderByTimestampDesc(meta.getId())
                        .orElseGet(() -> {
                            log.info("No current state exists for dam {}. Setting default state to ~80% capacity.", meta.getName());
                            return ReservoirState.builder()
                                    .damId(meta.getId())
                                    .timestamp(LocalDateTime.now())
                                    .currentVolumeM3(meta.getMaxCapacityM3() * 0.8)
                                    .waterLevelMeters(meta.getMaxWaterLevelMeters() * 0.85)
                                    .currentOutflowM3s(0.0)
                                    .gateOpenPercentage(0.0)
                                    .build();
                        });

                // Save default state if it wasn't persistent
                if (currentState.getId() == null) {
                    currentState = reservoirStateRepository.save(currentState);
                }

                // Fetch latest weather forecast precipitation
                double precipitationMm = weatherForecastService.fetchThreeDayPrecipitation(meta.getLatitude(), meta.getLongitude());

                // Run control logic evaluation
                ControlLog decision = damControlEngineService.evaluateAndExecuteControlLogic(
                        meta.getId(),
                        currentState.getCurrentVolumeM3(),
                        precipitationMm
                );

                log.info("Hourly assessment completed for dam {}. Decision Log ID: {}, Status: {}",
                        meta.getName(), decision.getId(), decision.getStatusMessage());
            } catch (Exception e) {
                log.error("Failed to run hourly assessment for dam {}: {}", meta.getName(), e.getMessage(), e);
            }
        }
    }
}
