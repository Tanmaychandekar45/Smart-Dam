using DamControlSystem.Data;
using DamControlSystem.Data.Repositories;
using DamControlSystem.Models;
using DamControlSystem.Services;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;
using Xunit;

namespace DamControlSystem.Tests;

public class HydrologicalEngineTests
{
    private (SmartDamDbContext db, DamControlEngineService engine) CreateService(string dbName)
    {
        var options = new DbContextOptionsBuilder<SmartDamDbContext>()
            .UseInMemoryDatabase(databaseName: dbName)
            .Options;

        var dbContext = new SmartDamDbContext(options);
        var controlRepo = new ControlLogRepository(dbContext);
        var stateRepo = new ReservoirStateRepository(dbContext);
        var logger = NullLogger<DamControlEngineService>.Instance;

        var engine = new DamControlEngineService(controlRepo, stateRepo, logger);
        return (dbContext, engine);
    }

    [Fact]
    public async Task Evaluate_SafeConditions_NoOutflowAndNoFloodAlert()
    {
        // Arrange: Erai max capacity is 226.5M m3, 85% warning is 192.525M m3
        // Start at 100M m3 with 10mm rain
        var (_, engine) = CreateService(nameof(Evaluate_SafeConditions_NoOutflowAndNoFloodAlert));

        // Act
        var result = await engine.EvaluateAndExecuteControlLogicAsync("erai", 100_000_000.0, 10.0);

        // Assert
        Assert.Equal("erai", result.DamId);
        Assert.False(result.FloodAlertTriggered);
        Assert.Equal(0.0, result.RecommendedOutflowM3s);
        Assert.Contains("safe operational parameters", result.StatusMessage);
    }

    [Fact]
    public async Task Evaluate_ProactiveDischarge_ReleasesRequiredOutflowWithoutAlert()
    {
        // Warning threshold for Erai is 192,525,000 m3
        // Current: 190,000,000 m3. Rain: 20 mm.
        // Catchment: 439.33e6 * 0.60 * (20 / 1000) = 5,271,960 m3
        // Projected volume: 195,271,960 m3 -> excess = 2,746,960 m3
        // Required outflow = 2,746,960 / (24 * 3600) ~= 31.79 m3/s (well below 800 m3/s safe cap)
        var (_, engine) = CreateService(nameof(Evaluate_ProactiveDischarge_ReleasesRequiredOutflowWithoutAlert));

        // Act
        var result = await engine.EvaluateAndExecuteControlLogicAsync("erai", 190_000_000.0, 20.0);

        // Assert
        Assert.False(result.FloodAlertTriggered);
        Assert.True(result.RecommendedOutflowM3s > 0.0);
        Assert.True(result.RecommendedOutflowM3s <= DamMetadata.Get("erai").MaxSafeDischargeM3s);
        Assert.Contains("Optimal proactive discharge", result.StatusMessage);
    }

    [Fact]
    public async Task Evaluate_ExtremeRainfall_TriggersFloodAlertAndCapsDischarge()
    {
        // Severe rainfall that forces required discharge > 800 m3/s
        // Current: 192,000,000 m3. Rain: 350 mm.
        var (_, engine) = CreateService(nameof(Evaluate_ExtremeRainfall_TriggersFloodAlertAndCapsDischarge));

        // Act
        var result = await engine.EvaluateAndExecuteControlLogicAsync("erai", 192_000_000.0, 350.0);

        // Assert
        Assert.True(result.FloodAlertTriggered);
        Assert.Equal(DamMetadata.Get("erai").MaxSafeDischargeM3s, result.RecommendedOutflowM3s);
        Assert.Contains("EMERGENCY", result.StatusMessage);
    }

    [Fact]
    public void SimulateControlLogic_DoesNotPersistToDatabase()
    {
        var (db, engine) = CreateService(nameof(SimulateControlLogic_DoesNotPersistToDatabase));

        // Act
        var simulation = engine.SimulateControlLogic("khadakwasla", 50_000_000.0, 50.0);

        // Assert
        Assert.NotNull(simulation);
        Assert.Contains("[SIMULATION]", simulation.StatusMessage);
        Assert.Empty(db.ControlLogs);
        Assert.Empty(db.ReservoirStates);
    }
}
