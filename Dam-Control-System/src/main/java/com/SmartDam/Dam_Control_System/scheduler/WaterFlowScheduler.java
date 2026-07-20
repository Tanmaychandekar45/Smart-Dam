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
     */
    @Scheduled(fixedRate = 3600000)
    public void executeHourlyDamAssessment() {
        log.info("Starting automated hourly reservoir assessment at {}", LocalDateTime.now());

        // Get latest reservoir state or initialize a default state
        ReservoirState currentState = reservoirStateRepository.findFirstByOrderByTimestampDesc()
                .orElseGet(() -> {
                    log.info("No current state exists. Setting default state to ~80% capacity.");
                    return ReservoirState.builder()
                            .timestamp(LocalDateTime.now())
                            .currentVolumeM3(181200000.0)
                            .waterLevelMeters(20.06)
                            .currentOutflowM3s(0.0)
                            .gateOpenPercentage(0.0)
                            .build();
                });

        // Save default state if it wasn't persistent
        if (currentState.getId() == null) {
            currentState = reservoirStateRepository.save(currentState);
        }

        // Fetch latest weather forecast precipitation
        double precipitationMm = weatherForecastService.fetchThreeDayPrecipitation();

        // Run control logic evaluation
        ControlLog decision = damControlEngineService.evaluateAndExecuteControlLogic(
                currentState.getCurrentVolumeM3(),
                precipitationMm
        );

        log.info("Hourly assessment completed. Decision Log ID: {}, Status: {}",
                decision.getId(), decision.getStatusMessage());
    }
}
