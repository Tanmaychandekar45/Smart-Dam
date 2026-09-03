using DamControlSystem.Data.Repositories;
using DamControlSystem.Models;

namespace DamControlSystem.Services;

public class DamControlEngineService : IDamControlEngineService
{
    private readonly ILogger<DamControlEngineService> _logger;
    private readonly IControlLogRepository _controlLogRepository;
    private readonly IReservoirStateRepository _reservoirStateRepository;

    public const double CatchmentAreaM2 = 439.33 * 1e6; // 439.33 km^2
    public const double RunoffCoefficient = 0.60;
    public const double MaxCapacityM3 = 226500000.0;
    public const double WarningThresholdM3 = 192525000.0; // 85% of total
    public const double MaxSafeDischargeM3s = 800.0;

    public DamControlEngineService(
        IControlLogRepository controlLogRepository,
        IReservoirStateRepository reservoirStateRepository,
        ILogger<DamControlEngineService> logger)
    {
        _controlLogRepository = controlLogRepository;
        _reservoirStateRepository = reservoirStateRepository;
        _logger = logger;
    }

    public async Task<ControlLog> EvaluateAndExecuteControlLogicAsync(string damId, double currentVolumeM3, double forecastRainfallMm)
    {
        var meta = DamMetadata.Get(damId);
        _logger.LogInformation("Running Control Engine Evaluation for Dam: {DamName} ({DamId}) - Current Volume: {Volume} m3, Forecast Rain: {Rain} mm",
            meta.Name, meta.Id, currentVolumeM3, forecastRainfallMm);

        // Runoff Calculation
        // Projected Inflow (m³) = (forecastRainfallMm / 1000.0) * Catchment Area * Run-off Coefficient
        double projectedInflowM3 = (forecastRainfallMm / 1000.0) * meta.CatchmentAreaM2 * meta.RunoffCoefficient;

        // Mass Balance
        double projectedVolumeM3 = currentVolumeM3 + projectedInflowM3;

        double recommendedOutflowM3s;
        bool floodAlertTriggered;
        string statusMessage;

        // Decision Logic
        if (projectedVolumeM3 > meta.WarningThresholdM3)
        {
            double excessVolume = projectedVolumeM3 - meta.WarningThresholdM3;
            double requiredOutflowM3s = excessVolume / (24.0 * 3600.0);

            if (requiredOutflowM3s > meta.MaxSafeDischargeM3s)
            {
                recommendedOutflowM3s = meta.MaxSafeDischargeM3s;
                floodAlertTriggered = true;
                statusMessage = $"EMERGENCY: Projected volume ({projectedVolumeM3 / 1e6:F2}M m³) exceeds warning threshold. " +
                                $"Required release rate ({requiredOutflowM3s:F2} m³/s) exceeds downstream safe channel capacity ({meta.MaxSafeDischargeM3s:F2} m³/s). " +
                                $"Flood alert triggered for {meta.Region} region.";
            }
            else
            {
                recommendedOutflowM3s = requiredOutflowM3s;
                floodAlertTriggered = false;
                statusMessage = $"Optimal proactive discharge initiated. Releasing {recommendedOutflowM3s:F2} m³/s over 24h " +
                                $"to maintain reservoir capacity below the warning threshold (85%).";
            }
        }
        else
        {
            recommendedOutflowM3s = 0.0;
            floodAlertTriggered = false;
            statusMessage = $"Reservoir state within safe operational parameters. " +
                            $"Projected Volume ({projectedVolumeM3 / 1e6:F2}M m³) is below the 85% capacity threshold. No proactive release required.";
        }

        // Save ControlLog decision
        var decisionLog = new ControlLog(
            id: 0,
            damId: meta.Id,
            timestamp: DateTime.UtcNow,
            forecastPrecipitationMm: forecastRainfallMm,
            predictedInflowM3: projectedInflowM3,
            recommendedOutflowM3s: recommendedOutflowM3s,
            floodAlertTriggered: floodAlertTriggered,
            statusMessage: statusMessage
        );

        var savedLog = await _controlLogRepository.SaveAsync(decisionLog);
        _logger.LogInformation("Persisted Decision Control Log. ID: {Id}, Dam: {DamId}, Alert: {Alert}, Recommended Release: {Release} m3/s",
            savedLog.Id, savedLog.DamId, savedLog.FloodAlertTriggered, savedLog.RecommendedOutflowM3s);

        // Update latest ReservoirState
        double gateOpenPercentage = (recommendedOutflowM3s / meta.MaxSafeDischargeM3s) * 100.0;
        double estimatedLevel = (currentVolumeM3 / meta.MaxCapacityM3) * meta.MaxWaterLevelMeters;

        var currentState = await _reservoirStateRepository.GetLatestByDamIdAsync(meta.Id);
        if (currentState != null)
        {
            currentState.Timestamp = DateTime.UtcNow;
            currentState.CurrentVolumeM3 = currentVolumeM3;
            currentState.WaterLevelMeters = Math.Round(estimatedLevel, 2);
            currentState.CurrentOutflowM3s = recommendedOutflowM3s;
            currentState.GateOpenPercentage = Math.Round(gateOpenPercentage, 2);
            await _reservoirStateRepository.SaveAsync(currentState);
        }
        else
        {
            var newState = new ReservoirState(
                id: 0,
                damId: meta.Id,
                timestamp: DateTime.UtcNow,
                currentVolumeM3: currentVolumeM3,
                waterLevelMeters: Math.Round(estimatedLevel, 2),
                currentOutflowM3s: recommendedOutflowM3s,
                gateOpenPercentage: Math.Round(gateOpenPercentage, 2)
            );
            await _reservoirStateRepository.SaveAsync(newState);
        }

        return savedLog;
    }

    public Task<ControlLog> EvaluateAndExecuteControlLogicAsync(double currentVolumeM3, double forecastRainfallMm)
    {
        return EvaluateAndExecuteControlLogicAsync("erai", currentVolumeM3, forecastRainfallMm);
    }

    public ControlLog SimulateControlLogic(string damId, double currentVolumeM3, double forecastRainfallMm)
    {
        var meta = DamMetadata.Get(damId);
        double projectedInflowM3 = (forecastRainfallMm / 1000.0) * meta.CatchmentAreaM2 * meta.RunoffCoefficient;
        double projectedVolumeM3 = currentVolumeM3 + projectedInflowM3;

        double recommendedOutflowM3s;
        bool floodAlertTriggered;
        string statusMessage;

        if (projectedVolumeM3 > meta.WarningThresholdM3)
        {
            double excessVolume = projectedVolumeM3 - meta.WarningThresholdM3;
            double requiredOutflowM3s = excessVolume / (24.0 * 3600.0);

            if (requiredOutflowM3s > meta.MaxSafeDischargeM3s)
            {
                recommendedOutflowM3s = meta.MaxSafeDischargeM3s;
                floodAlertTriggered = true;
                statusMessage = $"[SIMULATION] EMERGENCY: Projected volume ({projectedVolumeM3 / 1e6:F2}M m³) exceeds warning threshold. " +
                                $"Required release rate ({requiredOutflowM3s:F2} m³/s) exceeds downstream safe channel capacity ({meta.MaxSafeDischargeM3s:F2} m³/s). " +
                                $"Flood alert triggered for {meta.Region} region.";
            }
            else
            {
                recommendedOutflowM3s = requiredOutflowM3s;
                floodAlertTriggered = false;
                statusMessage = $"[SIMULATION] Optimal proactive discharge initiated. Releasing {recommendedOutflowM3s:F2} m³/s over 24h " +
                                $"to maintain reservoir capacity below the warning threshold (85%).";
            }
        }
        else
        {
            recommendedOutflowM3s = 0.0;
            floodAlertTriggered = false;
            statusMessage = $"[SIMULATION] Reservoir state within safe operational parameters. " +
                            $"Projected Volume ({projectedVolumeM3 / 1e6:F2}M m³) is below the 85% capacity threshold. No proactive release required.";
        }

        return new ControlLog(
            id: 0,
            damId: meta.Id,
            timestamp: DateTime.UtcNow,
            forecastPrecipitationMm: forecastRainfallMm,
            predictedInflowM3: projectedInflowM3,
            recommendedOutflowM3s: recommendedOutflowM3s,
            floodAlertTriggered: floodAlertTriggered,
            statusMessage: statusMessage
        );
    }

    public ControlLog SimulateControlLogic(double currentVolumeM3, double forecastRainfallMm)
    {
        return SimulateControlLogic("erai", currentVolumeM3, forecastRainfallMm);
    }
}
