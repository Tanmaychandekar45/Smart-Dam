namespace DamControlSystem.DTOs;

public class AiRecommendationResponse
{
    public string AdvisoryMessage { get; set; } = string.Empty;
    public string RiskLevel { get; set; } = "NOMINAL";
    public List<string> SuggestedActions { get; set; } = new();
    public double ConfidenceScore { get; set; }
    public string GateScheduleRecommendation { get; set; } = string.Empty;

    public AiRecommendationResponse() { }

    public AiRecommendationResponse(
        string advisoryMessage,
        string riskLevel,
        List<string> suggestedActions,
        double confidenceScore,
        string gateScheduleRecommendation)
    {
        AdvisoryMessage = advisoryMessage;
        RiskLevel = riskLevel;
        SuggestedActions = suggestedActions;
        ConfidenceScore = confidenceScore;
        GateScheduleRecommendation = gateScheduleRecommendation;
    }
}
