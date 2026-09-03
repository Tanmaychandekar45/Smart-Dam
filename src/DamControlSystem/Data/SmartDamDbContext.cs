using DamControlSystem.Models;
using Microsoft.EntityFrameworkCore;

namespace DamControlSystem.Data;

public class SmartDamDbContext : DbContext
{
    public SmartDamDbContext(DbContextOptions<SmartDamDbContext> options) : base(options)
    {
    }

    public DbSet<ReservoirState> ReservoirStates => Set<ReservoirState>();
    public DbSet<ControlLog> ControlLogs => Set<ControlLog>();
    public DbSet<EmergencyAlert> EmergencyAlerts => Set<EmergencyAlert>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        modelBuilder.Entity<ReservoirState>(entity =>
        {
            entity.ToTable("reservoir_state");
            entity.HasKey(e => e.Id);
            entity.HasIndex(e => new { e.DamId, e.Timestamp });
        });

        modelBuilder.Entity<ControlLog>(entity =>
        {
            entity.ToTable("control_log");
            entity.HasKey(e => e.Id);
            entity.HasIndex(e => new { e.DamId, e.Timestamp });
        });

        modelBuilder.Entity<EmergencyAlert>(entity =>
        {
            entity.ToTable("emergency_alert");
            entity.HasKey(e => e.Id);
            entity.HasIndex(e => new { e.DamId, e.Resolved, e.Timestamp });
        });
    }
}
