using System.Globalization;
using System.Text.Json;

namespace DamControlSystem.Services;

public class WeatherForecastService : IWeatherForecastService
{
    private readonly HttpClient _httpClient;
    private readonly ILogger<WeatherForecastService> _logger;

    public WeatherForecastService(HttpClient httpClient, ILogger<WeatherForecastService> logger)
    {
        _httpClient = httpClient;
        _logger = logger;
    }

    public async Task<double> FetchThreeDayPrecipitationAsync(double latitude, double longitude)
    {
        try
        {
            _logger.LogInformation("Fetching 3-day weather forecast from Open-Meteo API for coordinates: Lat {Lat}, Lon {Lon}...", latitude, longitude);

            var latStr = latitude.ToString(CultureInfo.InvariantCulture);
            var lonStr = longitude.ToString(CultureInfo.InvariantCulture);
            var url = $"v1/forecast?latitude={latStr}&longitude={lonStr}&daily=precipitation_sum&forecast_days=3&timezone=auto";

            var response = await _httpClient.GetAsync(url);
            if (!response.IsSuccessStatusCode)
            {
                _logger.LogWarning("Open-Meteo API returned non-success code {StatusCode}. Defaulting to 0.0 mm", response.StatusCode);
                return 0.0;
            }

            using var document = await JsonDocument.ParseAsync(await response.Content.ReadAsStreamAsync());
            var root = document.RootElement;

            if (root.TryGetProperty("daily", out var daily) &&
                daily.TryGetProperty("precipitation_sum", out var precipArray) &&
                precipArray.ValueKind == JsonValueKind.Array)
            {
                double totalPrecipitation = 0.0;
                foreach (var item in precipArray.EnumerateArray())
                {
                    if (item.ValueKind == JsonValueKind.Number)
                    {
                        totalPrecipitation += item.GetDouble();
                    }
                }

                _logger.LogInformation("Successfully fetched 3-day weather forecast. Total Precipitation: {Total} mm", totalPrecipitation);
                return totalPrecipitation;
            }

            _logger.LogWarning("Weather API response did not contain daily precipitation fields. Defaulting to 0.0 mm");
            return 0.0;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to retrieve weather forecast from Open-Meteo: {Message}. Defaulting to 0.0 mm", ex.Message);
            return 0.0;
        }
    }

    public Task<double> FetchThreeDayPrecipitationAsync()
    {
        return FetchThreeDayPrecipitationAsync(20.1677, 79.3048);
    }
}
