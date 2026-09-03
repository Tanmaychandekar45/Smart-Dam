namespace DamControlSystem.Services;

public interface IWeatherForecastService
{
    Task<double> FetchThreeDayPrecipitationAsync(double latitude, double longitude);
    Task<double> FetchThreeDayPrecipitationAsync();
}
