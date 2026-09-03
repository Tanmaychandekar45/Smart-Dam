using DamControlSystem.Models;

namespace DamControlSystem.Data.Repositories;

public interface IReservoirStateRepository
{
    Task<ReservoirState?> GetLatestAsync();
    Task<ReservoirState?> GetLatestByDamIdAsync(string damId);
    Task<ReservoirState> SaveAsync(ReservoirState state);
}
