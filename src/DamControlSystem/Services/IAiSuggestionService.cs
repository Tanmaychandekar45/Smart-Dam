using DamControlSystem.DTOs;

namespace DamControlSystem.Services;

public interface IAiSuggestionService
{
    Task<AiRecommendationResponse> GenerateRecommendationAsync(string damId);
}
