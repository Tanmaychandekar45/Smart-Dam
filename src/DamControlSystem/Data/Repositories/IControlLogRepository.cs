using DamControlSystem.Models;

namespace DamControlSystem.Data.Repositories;

public interface IControlLogRepository
{
    Task<ControlLog?> GetLatestAsync();
    Task<ControlLog?> GetLatestByDamIdAsync(string damId);
    Task<ControlLog> SaveAsync(ControlLog log);
}
