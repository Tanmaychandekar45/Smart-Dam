namespace DamControlSystem.Models;

public class DamMetadata
{
    public string Id { get; }
    public string Name { get; }
    public string Region { get; }
    public double Latitude { get; }
    public double Longitude { get; }
    public double MaxCapacityM3 { get; }
    public double CatchmentAreaM2 { get; }
    public double RunoffCoefficient { get; }
    public double WarningThresholdM3 { get; }
    public double MaxSafeDischargeM3s { get; }
    public double MaxWaterLevelMeters { get; }
    public string[] DownstreamVillages { get; }

    public DamMetadata(
        string id,
        string name,
        string region,
        double latitude,
        double longitude,
        double maxCapacityM3,
        double catchmentAreaM2,
        double runoffCoefficient,
        double warningThresholdM3,
        double maxSafeDischargeM3s,
        double maxWaterLevelMeters,
        string[] downstreamVillages)
    {
        Id = id;
        Name = name;
        Region = region;
        Latitude = latitude;
        Longitude = longitude;
        MaxCapacityM3 = maxCapacityM3;
        CatchmentAreaM2 = catchmentAreaM2;
        RunoffCoefficient = runoffCoefficient;
        WarningThresholdM3 = warningThresholdM3;
        MaxSafeDischargeM3s = maxSafeDischargeM3s;
        MaxWaterLevelMeters = maxWaterLevelMeters;
        DownstreamVillages = downstreamVillages;
    }

    private static readonly Dictionary<string, DamMetadata> Registry = new(StringComparer.OrdinalIgnoreCase)
    {
        ["erai"] = new DamMetadata(
            "erai", "Erai Dam", "Chandrapur", 20.1677, 79.3048,
            226500000.0, 439.33 * 1e6, 0.60, 192525000.0, 800.0, 23.6,
            ["Padmapur", "Datala", "Rayatwari"]
        ),
        ["khadakwasla"] = new DamMetadata(
            "khadakwasla", "Khadakwasla Dam", "Pune", 18.4316, 73.7634,
            56000000.0, 501.0 * 1e6, 0.65, 47600000.0, 500.0, 15.0,
            ["Nanded City", "Sinhagad Road", "Karve Nagar"]
        ),
        ["panshet"] = new DamMetadata(
            "panshet", "Panshet Dam", "Pune", 18.3759, 73.6120,
            294000000.0, 120.0 * 1e6, 0.70, 249900000.0, 1000.0, 35.0,
            ["Panshet Village", "Kuran", "Khanapur"]
        ),
        ["mulshi"] = new DamMetadata(
            "mulshi", "Mulshi Dam", "Pune", 18.5284, 73.5134,
            522000000.0, 250.0 * 1e6, 0.75, 443700000.0, 1500.0, 50.0,
            ["Mulshi Valley", "Male Village", "Bhare"]
        )
    };

    public static IReadOnlyDictionary<string, DamMetadata> GetRegistry() => Registry;

    public static DamMetadata Get(string? id)
    {
        if (string.IsNullOrWhiteSpace(id))
            return Registry["erai"];

        return Registry.TryGetValue(id, out var meta) ? meta : Registry["erai"];
    }
}
