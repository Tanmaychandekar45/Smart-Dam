using DamControlSystem.Models;
using Microsoft.EntityFrameworkCore;

namespace DamControlSystem.Data.Repositories;

public class EmergencyAlertRepository : IEmergencyAlertRepository
{
    private readonly SmartDamDbContext _context;

    public EmergencyAlertRepository(SmartDamDbContext context)
    {
        _context = context;
    }

    public async Task<List<EmergencyAlert>> GetByDamIdAndResolvedAsync(string damId, bool resolved)
    {
        return await _context.EmergencyAlerts
            .Where(a => a.DamId == damId && a.Resolved == resolved)
            .OrderByDescending(a => a.Timestamp)
            .ToListAsync();
    }

    public async Task<List<EmergencyAlert>> GetByResolvedAsync(bool resolved)
    {
        return await _context.EmergencyAlerts
            .Where(a => a.Resolved == resolved)
            .OrderByDescending(a => a.Timestamp)
            .ToListAsync();
    }

    public async Task<EmergencyAlert?> GetByIdAsync(long id)
    {
        return await _context.EmergencyAlerts.FindAsync(id);
    }

    public async Task<EmergencyAlert> SaveAsync(EmergencyAlert alert)
    {
        if (alert.Id == 0)
        {
            _context.EmergencyAlerts.Add(alert);
        }
        else
        {
            _context.EmergencyAlerts.Update(alert);
        }
        await _context.SaveChangesAsync();
        return alert;
    }
}
