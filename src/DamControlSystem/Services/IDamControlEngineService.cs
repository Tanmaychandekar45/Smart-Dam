using DamControlSystem.Models;

namespace DamControlSystem.Services;

public interface IDamControlEngineService
{
    Task<ControlLog> EvaluateAndExecuteControlLogicAsync(string damId, double currentVolumeM3, double forecastRainfallMm);
    Task<ControlLog> EvaluateAndExecuteControlLogicAsync(double currentVolumeM3, double forecastRainfallMm);
    ControlLog SimulateControlLogic(string damId, double currentVolumeM3, double forecastRainfallMm);
    ControlLog SimulateControlLogic(double currentVolumeM3, double forecastRainfallMm);
}
