using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace DamControlSystem.Models;

[Table("reservoir_state")]
public class ReservoirState
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    [Column("id")]
    public long Id { get; set; }

    [Column("dam_id")]
    public string DamId { get; set; } = "erai";

    [Column("timestamp")]
    public DateTime Timestamp { get; set; }

    [Column("current_volumem3")]
    public double CurrentVolumeM3 { get; set; }

    [Column("water_level_meters")]
    public double WaterLevelMeters { get; set; }

    [Column("current_outflowm3s")]
    public double CurrentOutflowM3s { get; set; }

    [Column("gate_open_percentage")]
    public double GateOpenPercentage { get; set; }

    public ReservoirState() { }

    public ReservoirState(
        long id,
        string damId,
        DateTime timestamp,
        double currentVolumeM3,
        double waterLevelMeters,
        double currentOutflowM3s,
        double gateOpenPercentage)
    {
        Id = id;
        DamId = damId;
        Timestamp = timestamp;
        CurrentVolumeM3 = currentVolumeM3;
        WaterLevelMeters = waterLevelMeters;
        CurrentOutflowM3s = currentOutflowM3s;
        GateOpenPercentage = gateOpenPercentage;
    }
}
