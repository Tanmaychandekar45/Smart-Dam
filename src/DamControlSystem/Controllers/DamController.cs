using DamControlSystem.Data.Repositories;
using DamControlSystem.DTOs;
using DamControlSystem.Models;
using DamControlSystem.Services;
using Microsoft.AspNetCore.Mvc;

namespace DamControlSystem.Controllers;

[ApiController]
[Route("api/v1/dam")]
public class DamController : ControllerBase
{
    private readonly ILogger<DamController> _logger;
    private readonly IReservoirStateRepository _reservoirStateRepository;
    private readonly IControlLogRepository _controlLogRepository;
    private readonly IDamControlEngineService _damControlEngineService;
    private readonly IWeatherForecastService _weatherForecastService;
    private readonly IAiSuggestionService _aiSuggestionService;
    private readonly IEmergencyAlertRepository _emergencyAlertRepository;

    public DamController(
        IReservoirStateRepository reservoirStateRepository,
        IControlLogRepository controlLogRepository,
        IDamControlEngineService damControlEngineService,
        IWeatherForecastService weatherForecastService,
        IAiSuggestionService aiSuggestionService,
        IEmergencyAlertRepository emergencyAlertRepository,
        ILogger<DamController> logger)
    {
        _reservoirStateRepository = reservoirStateRepository;
        _controlLogRepository = controlLogRepository;
        _damControlEngineService = damControlEngineService;
        _weatherForecastService = weatherForecastService;
        _aiSuggestionService = aiSuggestionService;
        _emergencyAlertRepository = emergencyAlertRepository;
        _logger = logger;
    }

    /// <summary>
    /// GET /api/v1/dam/status
    /// </summary>
    [HttpGet("status")]
    public Task<IActionResult> GetDamStatus()
    {
        return GetDamStatus("erai");
    }

    /// <summary>
    /// GET /api/v1/dam/{damId}/status
    /// </summary>
    [HttpGet("{damId}/status")]
    public async Task<IActionResult> GetDamStatus(string damId)
    {
        var meta = DamMetadata.Get(damId);

        var state = await _reservoirStateRepository.GetLatestByDamIdAsync(meta.Id);
        if (state == null)
        {
            _logger.LogInformation("No reservoir state found in database for dam {DamName}. Initializing default state...", meta.Name);
            var defaultState = new ReservoirState(
                id: 0,
                damId: meta.Id,
                timestamp: DateTime.UtcNow,
                currentVolumeM3: meta.MaxCapacityM3 * 0.8,
                waterLevelMeters: meta.MaxWaterLevelMeters * 0.85,
                currentOutflowM3s: 0.0,
                gateOpenPercentage: 0.0
            );
            state = await _reservoirStateRepository.SaveAsync(defaultState);
        }

        var latestLog = await _controlLogRepository.GetLatestByDamIdAsync(meta.Id);
        if (latestLog == null)
        {
            _logger.LogInformation("No control log found in database for dam {DamName}. Initializing default log...", meta.Name);
            var defaultLog = new ControlLog(
                id: 0,
                damId: meta.Id,
                timestamp: DateTime.UtcNow,
                forecastPrecipitationMm: 0.0,
                predictedInflowM3: 0.0,
                recommendedOutflowM3s: 0.0,
                floodAlertTriggered: false,
                statusMessage: "System initialized. Standing by for weather telemetry updates."
            );
            latestLog = await _controlLogRepository.SaveAsync(defaultLog);
        }

        return Ok(new
        {
            state,
            latestLog
        });
    }

    /// <summary>
    /// POST /api/v1/dam/update-state
    /// </summary>
    [HttpPost("update-state")]
    public Task<IActionResult> UpdateReservoirState([FromBody] UpdateStateRequest request)
    {
        return UpdateReservoirState("erai", request);
    }

    /// <summary>
    /// POST /api/v1/dam/{damId}/update-state
    /// </summary>
    [HttpPost("{damId}/update-state")]
    public async Task<IActionResult> UpdateReservoirState(string damId, [FromBody] UpdateStateRequest request)
    {
        var meta = DamMetadata.Get(damId);
        _logger.LogInformation("Received manual state update request for {DamName}: Volume={Volume}, Level={Level}",
            meta.Name, request.CurrentVolumeM3, request.WaterLevelMeters);

        var state = await _reservoirStateRepository.GetLatestByDamIdAsync(meta.Id) ?? new ReservoirState();
        state.DamId = meta.Id;
        state.Timestamp = DateTime.UtcNow;
        state.CurrentVolumeM3 = request.CurrentVolumeM3;
        state.WaterLevelMeters = request.WaterLevelMeters;
        await _reservoirStateRepository.SaveAsync(state);

        // Fetch weather forecast rain sum
        double forecastRainfall = await _weatherForecastService.FetchThreeDayPrecipitationAsync(meta.Latitude, meta.Longitude);

        // Run decision engine
        var updatedDecision = await _damControlEngineService.EvaluateAndExecuteControlLogicAsync(
            meta.Id, request.CurrentVolumeM3, forecastRainfall);

        return Ok(updatedDecision);
    }

    /// <summary>
    /// GET /api/v1/dam/forecast-eval
    /// </summary>
    [HttpGet("forecast-eval")]
    public IActionResult RunForecastSimulation([FromQuery] double currentVolumeM3, [FromQuery] double forecastRainfallMm)
    {
        return RunForecastSimulation("erai", currentVolumeM3, forecastRainfallMm);
    }

    /// <summary>
    /// GET /api/v1/dam/{damId}/forecast-eval
    /// </summary>
    [HttpGet("{damId}/forecast-eval")]
    public IActionResult RunForecastSimulation(string damId, [FromQuery] double currentVolumeM3, [FromQuery] double forecastRainfallMm)
    {
        var meta = DamMetadata.Get(damId);
        _logger.LogInformation("Received simulation request for {DamName}: Volume={Volume}, Rainfall={Rainfall}",
            meta.Name, currentVolumeM3, forecastRainfallMm);

        var simulatedLog = _damControlEngineService.SimulateControlLogic(meta.Id, currentVolumeM3, forecastRainfallMm);
        return Ok(simulatedLog);
    }

    /// <summary>
    /// GET /api/v1/dam/ai-recommendation
    /// </summary>
    [HttpGet("ai-recommendation")]
    public Task<IActionResult> GetAiRecommendation()
    {
        return GetAiRecommendation("erai");
    }

    /// <summary>
    /// GET /api/v1/dam/{damId}/ai-recommendation
    /// </summary>
    [HttpGet("{damId}/ai-recommendation")]
    public async Task<IActionResult> GetAiRecommendation(string damId)
    {
        _logger.LogInformation("Received request for AI Recommendation for dam: {DamId}", damId);
        var recommendation = await _aiSuggestionService.GenerateRecommendationAsync(damId);
        return Ok(recommendation);
    }

    /// <summary>
    /// POST /api/v1/dam/{damId}/submit-decision
    /// </summary>
    [HttpPost("{damId}/submit-decision")]
    public async Task<IActionResult> SubmitDecision(string damId)
    {
        _logger.LogInformation("Submitting latest decision log to higher authority for dam: {DamId}", damId);
        var meta = DamMetadata.Get(damId);

        var logEntry = await _controlLogRepository.GetLatestByDamIdAsync(meta.Id);
        if (logEntry == null)
            return BadRequest(new { error = "No log found to submit." });

        logEntry.ApprovalStatus = "PENDING_AUTHORITY";
        await _controlLogRepository.SaveAsync(logEntry);

        return Ok(logEntry);
    }

    /// <summary>
    /// POST /api/v1/dam/{damId}/authority-action
    /// </summary>
    [HttpPost("{damId}/authority-action")]
    public async Task<IActionResult> AuthorityAction(
        string damId,
        [FromQuery] string action,
        [FromQuery] double? manualOutflow = null)
    {
        _logger.LogInformation("Authority action received for dam: {DamId}. Action: {Action}, ManualOutflow: {ManualOutflow}",
            damId, action, manualOutflow);

        var meta = DamMetadata.Get(damId);

        var logEntry = await _controlLogRepository.GetLatestByDamIdAsync(meta.Id);
        if (logEntry == null)
            return BadRequest(new { error = "No log found to act on." });

        var state = await _reservoirStateRepository.GetLatestByDamIdAsync(meta.Id);
        if (state == null)
            return BadRequest(new { error = "No reservoir state found." });

        if ("APPROVE".Equals(action, StringComparison.OrdinalIgnoreCase))
        {
            logEntry.ApprovalStatus = "APPROVED";
            logEntry.StatusMessage = "[APPROVED BY AUTHORITY] " + logEntry.StatusMessage;

            double targetOutflow = logEntry.RecommendedOutflowM3s;
            state.CurrentOutflowM3s = targetOutflow;

            double openPercent = (targetOutflow / meta.MaxSafeDischargeM3s) * 100.0;
            state.GateOpenPercentage = Math.Clamp(openPercent, 0.0, 100.0);

            await _reservoirStateRepository.SaveAsync(state);
        }
        else
        {
            logEntry.ApprovalStatus = "REJECTED";
            double outflowOverride = manualOutflow ?? 0.0;
            logEntry.RecommendedOutflowM3s = outflowOverride;
            logEntry.StatusMessage = $"[OVERRIDDEN BY AUTHORITY] Outflow locked at {outflowOverride:F2} m³/s manually.";

            state.CurrentOutflowM3s = outflowOverride;
            double openPercent = (outflowOverride / meta.MaxSafeDischargeM3s) * 100.0;
            state.GateOpenPercentage = Math.Clamp(openPercent, 0.0, 100.0);

            await _reservoirStateRepository.SaveAsync(state);
        }

        await _controlLogRepository.SaveAsync(logEntry);
        return Ok(logEntry);
    }

    /// <summary>
    /// POST /api/v1/dam/{damId}/emergency-alert
    /// </summary>
    [HttpPost("{damId}/emergency-alert")]
    public async Task<IActionResult> CreateEmergencyAlert(
        string damId,
        [FromQuery] string priority,
        [FromQuery] string message,
        [FromQuery] string shiftOfficerName)
    {
        _logger.LogInformation("Creating emergency alert for dam: {DamId}. Priority: {Priority}, Officer: {Officer}",
            damId, priority, shiftOfficerName);

        var alert = new EmergencyAlert(
            id: 0,
            damId: damId,
            priority: priority,
            message: message,
            timestamp: DateTime.UtcNow,
            shiftOfficerName: shiftOfficerName,
            resolved: false
        );

        var saved = await _emergencyAlertRepository.SaveAsync(alert);
        return Ok(saved);
    }

    /// <summary>
    /// GET /api/v1/dam/alerts
    /// </summary>
    [HttpGet("alerts")]
    public async Task<IActionResult> GetActiveAlerts()
    {
        _logger.LogInformation("Fetching all active/unresolved emergency alerts");
        var alerts = await _emergencyAlertRepository.GetByResolvedAsync(false);
        return Ok(alerts);
    }

    /// <summary>
    /// POST /api/v1/dam/alert/{id}/resolve
    /// </summary>
    [HttpPost("alert/{id:long}/resolve")]
    public async Task<IActionResult> ResolveAlert(long id)
    {
        _logger.LogInformation("Resolving emergency alert ID: {Id}", id);
        var alert = await _emergencyAlertRepository.GetByIdAsync(id);
        if (alert == null)
            return NotFound(new { error = "Alert not found" });

        alert.Resolved = true;
        await _emergencyAlertRepository.SaveAsync(alert);
        return Ok();
    }
}
