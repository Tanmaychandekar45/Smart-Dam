using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace DamControlSystem.Models;

[Table("emergency_alert")]
public class EmergencyAlert
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    [Column("id")]
    public long Id { get; set; }

    [Column("dam_id")]
    public string DamId { get; set; } = string.Empty;

    [Column("priority")]
    public string Priority { get; set; } = "LOW";

    [Column("message")]
    [MaxLength(1000)]
    public string Message { get; set; } = string.Empty;

    [Column("timestamp")]
    public DateTime Timestamp { get; set; }

    [Column("shift_officer_name")]
    public string ShiftOfficerName { get; set; } = string.Empty;

    [Column("resolved")]
    public bool Resolved { get; set; }

    public EmergencyAlert() { }

    public EmergencyAlert(
        long id,
        string damId,
        string priority,
        string message,
        DateTime timestamp,
        string shiftOfficerName,
        bool resolved)
    {
        Id = id;
        DamId = damId;
        Priority = priority;
        Message = message;
        Timestamp = timestamp;
        ShiftOfficerName = shiftOfficerName;
        Resolved = resolved;
    }
}
