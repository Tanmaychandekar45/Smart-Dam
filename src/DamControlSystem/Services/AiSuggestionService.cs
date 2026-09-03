using DamControlSystem.Data.Repositories;
using DamControlSystem.DTOs;
using DamControlSystem.Models;

namespace DamControlSystem.Services;

public class AiSuggestionService : IAiSuggestionService
{
    private readonly IReservoirStateRepository _reservoirStateRepository;
    private readonly IControlLogRepository _controlLogRepository;

    public AiSuggestionService(
        IReservoirStateRepository reservoirStateRepository,
        IControlLogRepository controlLogRepository)
    {
        _reservoirStateRepository = reservoirStateRepository;
        _controlLogRepository = controlLogRepository;
    }

    public async Task<AiRecommendationResponse> GenerateRecommendationAsync(string damId)
    {
        var meta = DamMetadata.Get(damId);

        var state = await _reservoirStateRepository.GetLatestByDamIdAsync(meta.Id)
            ?? new ReservoirState(
                id: 0,
                damId: meta.Id,
                timestamp: DateTime.UtcNow,
                currentVolumeM3: meta.MaxCapacityM3 * 0.8,
                waterLevelMeters: meta.MaxWaterLevelMeters * 0.85,
                currentOutflowM3s: 0.0,
                gateOpenPercentage: 0.0
            );

        var log = await _controlLogRepository.GetLatestByDamIdAsync(meta.Id)
            ?? new ControlLog(
                id: 0,
                damId: meta.Id,
                timestamp: DateTime.UtcNow,
                forecastPrecipitationMm: 0.0,
                predictedInflowM3: 0.0,
                recommendedOutflowM3s: 0.0,
                floodAlertTriggered: false,
                statusMessage: "Standby"
            );

        double volumePercent = (state.CurrentVolumeM3 / meta.MaxCapacityM3) * 100.0;
        double rainForecast = log.ForecastPrecipitationMm;
        bool alert = log.FloodAlertTriggered;

        string riskLevel;
        double confidenceScore;
        string advisoryMessage;
        string gateScheduleRecommendation;
        var suggestedActions = new List<string>();

        if (alert || volumePercent >= 95.0)
        {
            riskLevel = "CRITICAL";
            confidenceScore = 98.4;
            advisoryMessage = $"URGENT: Storage level at {volumePercent:F1}% and projected inflow exceeds downstream channel capacity for {meta.Name}. Active flood threat detected for downstream areas ({string.Join(", ", meta.DownstreamVillages)}). Emergency discharge required.";

            gateScheduleRecommendation = "IMMEDIATE ACTION: Deploy emergency gate configurations. Unlock G3 & G4. Open G1-G4 at 100% capacity. Continuously dump surplus volume to maintain reservoir structural integrity.";

            suggestedActions.Add("Sound emergency sirens for Padmapur, Datala, and surrounding downstream zones immediately.");
            suggestedActions.Add("Initiate immediate evacuation protocols for downstream populations.");
            suggestedActions.Add("Coordinate emergency relief operations with the local NDRF / disaster response teams.");
            suggestedActions.Add("Halt all recreational and boat traffic in the upper reservoir.");
        }
        else if (volumePercent >= 83.0 || log.RecommendedOutflowM3s > 0.0)
        {
            riskLevel = "WARNING";
            confidenceScore = 94.6;
            advisoryMessage = $"WARNING: Proactive early discharge recommended. The reservoir is at {volumePercent:F1}% capacity with {rainForecast:F1} mm of rain forecast. Outflows must be scaled to avoid emergency spikes later.";

            double releaseRate = log.RecommendedOutflowM3s;
            if (releaseRate <= 0.0)
            {
                releaseRate = meta.MaxSafeDischargeM3s * 0.25; // fallback proactive release
            }

            gateScheduleRecommendation = $"PROACTIVE RELEASE: Open G1 & G2 at {(releaseRate / meta.MaxSafeDischargeM3s) * 100:F0}% to discharge {releaseRate:F1} m³/s over the next 18 hours. Stand by G3 & G4.";

            suggestedActions.Add("Alert downstream monitoring stations of proactive discharge initiation.");
            suggestedActions.Add("Check and ensure automated telemetry backup sensors are active.");
            suggestedActions.Add("Audit gate hydraulics pressure systems.");
        }
        else
        {
            riskLevel = "NOMINAL";
            confidenceScore = 91.2;
            advisoryMessage = $"NOMINAL: Storage is within safe bounds ({volumePercent:F1}%). Weather forecasts project mild precipitation ({rainForecast:F1} mm). Downstream river channels are stable. Standby monitoring is active.";

            gateScheduleRecommendation = "NOMINAL OPERATIONAL: Close all gates. Stand by in automatic response mode. Monitor real-time telemetry updates.";

            suggestedActions.Add("Conduct routine structural audits on main concrete blocks.");
            suggestedActions.Add("Check open APIs weather sync logs every 3 hours.");
            suggestedActions.Add("Log gate status logs in the primary HYDRO-OS ledger.");
        }

        return new AiRecommendationResponse(advisoryMessage, riskLevel, suggestedActions, confidenceScore, gateScheduleRecommendation);
    }
}
