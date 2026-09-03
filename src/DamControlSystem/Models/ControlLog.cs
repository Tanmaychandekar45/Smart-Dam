using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace DamControlSystem.Models;

[Table("control_log")]
public class ControlLog
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    [Column("id")]
    public long Id { get; set; }

    [Column("dam_id")]
    public string DamId { get; set; } = "erai";

    [Column("timestamp")]
    public DateTime Timestamp { get; set; }

    [Column("forecast_precipitation_mm")]
    public double ForecastPrecipitationMm { get; set; }

    [Column("predicted_inflowm3")]
    public double PredictedInflowM3 { get; set; }

    [Column("recommended_outflowm3s")]
    public double RecommendedOutflowM3s { get; set; }

    [Column("flood_alert_triggered")]
    public bool FloodAlertTriggered { get; set; }

    [Column("status_message")]
    [MaxLength(1000)]
    public string StatusMessage { get; set; } = string.Empty;

    [Column("approval_status")]
    public string ApprovalStatus { get; set; } = "PENDING_OPERATOR";

    public ControlLog() { }

    public ControlLog(
        long id,
        string damId,
        DateTime timestamp,
        double forecastPrecipitationMm,
        double predictedInflowM3,
        double recommendedOutflowM3s,
        bool floodAlertTriggered,
        string statusMessage,
        string approvalStatus = "PENDING_OPERATOR")
    {
        Id = id;
        DamId = damId;
        Timestamp = timestamp;
        ForecastPrecipitationMm = forecastPrecipitationMm;
        PredictedInflowM3 = predictedInflowM3;
        RecommendedOutflowM3s = recommendedOutflowM3s;
        FloodAlertTriggered = floodAlertTriggered;
        StatusMessage = statusMessage;
        ApprovalStatus = approvalStatus;
    }
}
