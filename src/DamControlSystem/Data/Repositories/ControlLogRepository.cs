using DamControlSystem.Models;
using Microsoft.EntityFrameworkCore;

namespace DamControlSystem.Data.Repositories;

public class ControlLogRepository : IControlLogRepository
{
    private readonly SmartDamDbContext _context;

    public ControlLogRepository(SmartDamDbContext context)
    {
        _context = context;
    }

    public async Task<ControlLog?> GetLatestAsync()
    {
        return await _context.ControlLogs
            .OrderByDescending(c => c.Timestamp)
            .FirstOrDefaultAsync();
    }

    public async Task<ControlLog?> GetLatestByDamIdAsync(string damId)
    {
        return await _context.ControlLogs
            .Where(c => c.DamId == damId)
            .OrderByDescending(c => c.Timestamp)
            .FirstOrDefaultAsync();
    }

    public async Task<ControlLog> SaveAsync(ControlLog log)
    {
        if (log.Id == 0)
        {
            _context.ControlLogs.Add(log);
        }
        else
        {
            _context.ControlLogs.Update(log);
        }
        await _context.SaveChangesAsync();
        return log;
    }
}
