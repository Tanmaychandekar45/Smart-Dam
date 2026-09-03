using DamControlSystem.Models;
using Microsoft.EntityFrameworkCore;

namespace DamControlSystem.Data.Repositories;

public class ReservoirStateRepository : IReservoirStateRepository
{
    private readonly SmartDamDbContext _context;

    public ReservoirStateRepository(SmartDamDbContext context)
    {
        _context = context;
    }

    public async Task<ReservoirState?> GetLatestAsync()
    {
        return await _context.ReservoirStates
            .OrderByDescending(r => r.Timestamp)
            .FirstOrDefaultAsync();
    }

    public async Task<ReservoirState?> GetLatestByDamIdAsync(string damId)
    {
        return await _context.ReservoirStates
            .Where(r => r.DamId == damId)
            .OrderByDescending(r => r.Timestamp)
            .FirstOrDefaultAsync();
    }

    public async Task<ReservoirState> SaveAsync(ReservoirState state)
    {
        if (state.Id == 0)
        {
            _context.ReservoirStates.Add(state);
        }
        else
        {
            _context.ReservoirStates.Update(state);
        }
        await _context.SaveChangesAsync();
        return state;
    }
}
