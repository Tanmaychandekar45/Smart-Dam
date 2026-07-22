package com.SmartDam.Dam_Control_System.service;

import com.SmartDam.Dam_Control_System.dto.AiRecommendationResponse;
import com.SmartDam.Dam_Control_System.entity.DamMetadata;
import com.SmartDam.Dam_Control_System.entity.ReservoirState;
import com.SmartDam.Dam_Control_System.entity.ControlLog;
import com.SmartDam.Dam_Control_System.repository.ReservoirStateRepository;
import com.SmartDam.Dam_Control_System.repository.ControlLogRepository;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiSuggestionService {

    private final ReservoirStateRepository reservoirStateRepository;
    private final ControlLogRepository controlLogRepository;

    public AiSuggestionService(ReservoirStateRepository reservoirStateRepository,
                                ControlLogRepository controlLogRepository) {
        this.reservoirStateRepository = reservoirStateRepository;
        this.controlLogRepository = controlLogRepository;
    }

    public AiRecommendationResponse generateRecommendation(String damId) {
        DamMetadata meta = DamMetadata.get(damId);

        ReservoirState state = reservoirStateRepository.findFirstByDamIdOrderByTimestampDesc(meta.getId())
                .orElse(new ReservoirState(null, meta.getId(), java.time.LocalDateTime.now(), meta.getMaxCapacityM3() * 0.8, meta.getMaxWaterLevelMeters() * 0.85, 0.0, 0.0));

        ControlLog log = controlLogRepository.findFirstByDamIdOrderByTimestampDesc(meta.getId())
                .orElse(new ControlLog(null, meta.getId(), java.time.LocalDateTime.now(), 0.0, 0.0, 0.0, false, "Standby"));

        double volumePercent = (state.getCurrentVolumeM3() / meta.getMaxCapacityM3()) * 100.0;
        double rainForecast = log.getForecastPrecipitationMm();
        boolean alert = log.isFloodAlertTriggered();

        String riskLevel;
        double confidenceScore;
        String advisoryMessage;
        String gateScheduleRecommendation;
        List<String> suggestedActions = new ArrayList<>();

        if (alert || volumePercent >= 95.0) {
            riskLevel = "CRITICAL";
            confidenceScore = 98.4;
            advisoryMessage = String.format("URGENT: Storage level at %.1f%% and projected inflow exceeds downstream channel capacity for %s. Active flood threat detected for downstream areas (%s). Emergency discharge required.",
                    volumePercent, meta.getName(), String.join(", ", meta.getDownstreamVillages()));
            
            gateScheduleRecommendation = "IMMEDIATE ACTION: Deploy emergency gate configurations. Unlock G3 & G4. Open G1-G4 at 100% capacity. Continuously dump surplus volume to maintain reservoir structural integrity.";
            
            suggestedActions.add("Sound emergency sirens for Padmapur, Datala, and surrounding downstream zones immediately.");
            suggestedActions.add("Initiate immediate evacuation protocols for downstream populations.");
            suggestedActions.add("Coordinate emergency relief operations with the local NDRF / disaster response teams.");
            suggestedActions.add("Halt all recreational and boat traffic in the upper reservoir.");
        } else if (volumePercent >= 83.0 || log.getRecommendedOutflowM3s() > 0.0) {
            riskLevel = "WARNING";
            confidenceScore = 94.6;
            advisoryMessage = String.format("WARNING: Proactive early discharge recommended. The reservoir is at %.1f%% capacity with %.1f mm of rain forecast. Outflows must be scaled to avoid emergency spikes later.",
                    volumePercent, rainForecast);
            
            double releaseRate = log.getRecommendedOutflowM3s();
            if (releaseRate <= 0.0) {
                releaseRate = meta.getMaxSafeDischargeM3s() * 0.25; // fallback proactive release
            }
            gateScheduleRecommendation = String.format("PROACTIVE RELEASE: Open G1 & G2 at %.0f%% to discharge %.1f m³/s over the next 18 hours. Stand by G3 & G4.",
                    (releaseRate / meta.getMaxSafeDischargeM3s()) * 100, releaseRate);
            
            suggestedActions.add("Alert downstream monitoring stations of proactive discharge initiation.");
            suggestedActions.add("Check and ensure automated telemetry backup sensors are active.");
            suggestedActions.add("Audit gate hydraulics pressure systems.");
        } else {
            riskLevel = "NOMINAL";
            confidenceScore = 91.2;
            advisoryMessage = String.format("NOMINAL: Storage is within safe bounds (%.1f%%). Weather forecasts project mild precipitation (%.1f mm). Downstream river channels are stable. Standby monitoring is active.",
                    volumePercent, rainForecast);
            
            gateScheduleRecommendation = "NOMINAL OPERATIONAL: Close all gates. Stand by in automatic response mode. Monitor real-time telemetry updates.";
            
            suggestedActions.add("Conduct routine structural audits on main concrete blocks.");
            suggestedActions.add("Check open APIs weather sync logs every 3 hours.");
            suggestedActions.add("Log gate status logs in the primary HYDRO-OS ledger.");
        }

        return new AiRecommendationResponse(advisoryMessage, riskLevel, suggestedActions, confidenceScore, gateScheduleRecommendation);
    }
}
