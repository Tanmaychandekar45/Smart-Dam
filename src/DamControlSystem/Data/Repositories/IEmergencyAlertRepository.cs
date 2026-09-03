using DamControlSystem.Models;

namespace DamControlSystem.Data.Repositories;

public interface IEmergencyAlertRepository
{
    Task<List<EmergencyAlert>> GetByDamIdAndResolvedAsync(string damId, bool resolved);
    Task<List<EmergencyAlert>> GetByResolvedAsync(bool resolved);
    Task<EmergencyAlert?> GetByIdAsync(long id);
    Task<EmergencyAlert> SaveAsync(EmergencyAlert alert);
}
