package com.SmartDam.Dam_Control_System.entity;

import java.util.HashMap;
import java.util.Map;

public class DamMetadata {
    private final String id;
    private final String name;
    private final String region;
    private final double latitude;
    private final double longitude;
    private final double maxCapacityM3;
    private final double catchmentAreaM2;
    private final double runoffCoefficient;
    private final double warningThresholdM3;
    private final double maxSafeDischargeM3s;
    private final double maxWaterLevelMeters;
    private final String[] downstreamVillages;

    public DamMetadata(String id, String name, String region, double latitude, double longitude,
                       double maxCapacityM3, double catchmentAreaM2, double runoffCoefficient,
                       double warningThresholdM3, double maxSafeDischargeM3s, double maxWaterLevelMeters,
                       String[] downstreamVillages) {
        this.id = id;
        this.name = name;
        this.region = region;
        this.latitude = latitude;
        this.longitude = longitude;
        this.maxCapacityM3 = maxCapacityM3;
        this.catchmentAreaM2 = catchmentAreaM2;
        this.runoffCoefficient = runoffCoefficient;
        this.warningThresholdM3 = warningThresholdM3;
        this.maxSafeDischargeM3s = maxSafeDischargeM3s;
        this.maxWaterLevelMeters = maxWaterLevelMeters;
        this.downstreamVillages = downstreamVillages;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getRegion() { return region; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getMaxCapacityM3() { return maxCapacityM3; }
    public double getCatchmentAreaM2() { return catchmentAreaM2; }
    public double getRunoffCoefficient() { return runoffCoefficient; }
    public double getWarningThresholdM3() { return warningThresholdM3; }
    public double getMaxSafeDischargeM3s() { return maxSafeDischargeM3s; }
    public double getMaxWaterLevelMeters() { return maxWaterLevelMeters; }
    public String[] getDownstreamVillages() { return downstreamVillages; }

    // Registry of all dams
    private static final Map<String, DamMetadata> REGISTRY = new HashMap<>();

    static {
        REGISTRY.put("erai", new DamMetadata(
            "erai", "Erai Dam", "Chandrapur", 20.1677, 79.3048,
            226500000.0, 439.33 * 1e6, 0.60, 192525000.0, 800.0, 23.6,
            new String[]{"Padmapur", "Datala", "Rayatwari"}
        ));
        REGISTRY.put("khadakwasla", new DamMetadata(
            "khadakwasla", "Khadakwasla Dam", "Pune", 18.4316, 73.7634,
            56000000.0, 501.0 * 1e6, 0.65, 47600000.0, 500.0, 15.0,
            new String[]{"Nanded City", "Sinhagad Road", "Karve Nagar"}
        ));
        REGISTRY.put("panshet", new DamMetadata(
            "panshet", "Panshet Dam", "Pune", 18.3759, 73.6120,
            294000000.0, 120.0 * 1e6, 0.70, 249900000.0, 1000.0, 35.0,
            new String[]{"Panshet Village", "Kuran", "Khanapur"}
        ));
        REGISTRY.put("mulshi", new DamMetadata(
            "mulshi", "Mulshi Dam", "Pune", 18.5284, 73.5134,
            522000000.0, 250.0 * 1e6, 0.75, 443700000.0, 1500.0, 50.0,
            new String[]{"Mulshi Valley", "Male Village", "Bhare"}
        ));
    }

    public static Map<String, DamMetadata> getRegistry() {
        return REGISTRY;
    }

    public static DamMetadata get(String id) {
        return REGISTRY.getOrDefault(id.toLowerCase(), REGISTRY.get("erai"));
    }
}
