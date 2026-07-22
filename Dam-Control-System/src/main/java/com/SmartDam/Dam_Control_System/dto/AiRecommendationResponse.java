package com.SmartDam.Dam_Control_System.dto;

import java.util.List;

public class AiRecommendationResponse {
    private String advisoryMessage;
    private String riskLevel;
    private List<String> suggestedActions;
    private double confidenceScore;
    private String gateScheduleRecommendation;

    public AiRecommendationResponse() {}

    public AiRecommendationResponse(String advisoryMessage, String riskLevel, List<String> suggestedActions, double confidenceScore, String gateScheduleRecommendation) {
        this.advisoryMessage = advisoryMessage;
        this.riskLevel = riskLevel;
        this.suggestedActions = suggestedActions;
        this.confidenceScore = confidenceScore;
        this.gateScheduleRecommendation = gateScheduleRecommendation;
    }

    public String getAdvisoryMessage() { return advisoryMessage; }
    public void setAdvisoryMessage(String advisoryMessage) { this.advisoryMessage = advisoryMessage; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public List<String> getSuggestedActions() { return suggestedActions; }
    public void setSuggestedActions(List<String> suggestedActions) { this.suggestedActions = suggestedActions; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getGateScheduleRecommendation() { return gateScheduleRecommendation; }
    public void setGateScheduleRecommendation(String gateScheduleRecommendation) { this.gateScheduleRecommendation = gateScheduleRecommendation; }
}
