using DamControlSystem.Data.Repositories;
using DamControlSystem.Models;
using DamControlSystem.Services;

namespace DamControlSystem.BackgroundServices;

public class WaterFlowBackgroundService : BackgroundService
{
    private readonly IServiceProvider _serviceProvider;
    private readonly ILogger<WaterFlowBackgroundService> _logger;
    private readonly TimeSpan _period = TimeSpan.FromHours(1);

    public WaterFlowBackgroundService(
        IServiceProvider serviceProvider,
        ILogger<WaterFlowBackgroundService> logger)
    {
        _serviceProvider = serviceProvider;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _logger.LogInformation("WaterFlowBackgroundService started.");

        // Run once on startup
        await ExecuteHourlyAssessmentAsync(stoppingToken);

        using var timer = new PeriodicTimer(_period);
        while (!stoppingToken.IsCancellationRequested && await timer.WaitForNextTickAsync(stoppingToken))
        {
            await ExecuteHourlyAssessmentAsync(stoppingToken);
        }
    }

    public async Task ExecuteHourlyAssessmentAsync(CancellationToken cancellationToken = default)
    {
        _logger.LogInformation("Starting automated hourly reservoir assessment for all registered dams at {Time}", DateTime.UtcNow);

        using var scope = _serviceProvider.CreateScope();
        var reservoirStateRepository = scope.ServiceProvider.GetRequiredService<IReservoirStateRepository>();
        var weatherForecastService = scope.ServiceProvider.GetRequiredService<IWeatherForecastService>();
        var damControlEngineService = scope.ServiceProvider.GetRequiredService<IDamControlEngineService>();

        foreach (var damId in DamMetadata.GetRegistry().Keys)
        {
            if (cancellationToken.IsCancellationRequested)
                break;

            var meta = DamMetadata.Get(damId);
            try
            {
                var currentState = await reservoirStateRepository.GetLatestByDamIdAsync(meta.Id);
                if (currentState == null)
                {
                    _logger.LogInformation("No current state exists for dam {DamName}. Setting default state to ~80% capacity.", meta.Name);
                    currentState = new ReservoirState(
                        id: 0,
                        damId: meta.Id,
                        timestamp: DateTime.UtcNow,
                        currentVolumeM3: meta.MaxCapacityM3 * 0.8,
                        waterLevelMeters: meta.MaxWaterLevelMeters * 0.85,
                        currentOutflowM3s: 0.0,
                        gateOpenPercentage: 0.0
                    );
                    currentState = await reservoirStateRepository.SaveAsync(currentState);
                }

                // Fetch latest weather forecast precipitation
                double precipitationMm = await weatherForecastService.FetchThreeDayPrecipitationAsync(meta.Latitude, meta.Longitude);

                // Run control logic evaluation
                var decision = await damControlEngineService.EvaluateAndExecuteControlLogicAsync(
                    meta.Id,
                    currentState.CurrentVolumeM3,
                    precipitationMm
                );

                _logger.LogInformation("Hourly assessment completed for dam {DamName}. Decision Log ID: {LogId}, Status: {Status}",
                    meta.Name, decision.Id, decision.StatusMessage);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to run hourly assessment for dam {DamName}: {Message}", meta.Name, ex.Message);
            }
        }
    }
}
