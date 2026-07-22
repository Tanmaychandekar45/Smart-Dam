package com.SmartDam.Dam_Control_System.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;

@Service
public class WeatherForecastService {

    private static final Logger log = LoggerFactory.getLogger(WeatherForecastService.class);
    private final RestClient restClient;

    public WeatherForecastService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.open-meteo.com/v1/forecast")
                .build();
    }

    /**
     * Fetches 3-day precipitation forecast for dynamic coordinates.
     *
     * @return Sum of forecast precipitation in millimeters (mm) over the next 3 days.
     */
    public double fetchThreeDayPrecipitation(double latitude, double longitude) {
        try {
            log.info("Fetching 3-day weather forecast from Open-Meteo API for coordinates: Lat {}, Lon {}...", latitude, longitude);
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("daily", "precipitation_sum")
                            .queryParam("forecast_days", 3)
                            .queryParam("timezone", "auto")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("daily") && response.get("daily").has("precipitation_sum")) {
                JsonNode precipSumNode = response.get("daily").get("precipitation_sum");
                double totalPrecipitation = 0.0;
                if (precipSumNode.isArray()) {
                    for (JsonNode valueNode : precipSumNode) {
                        if (!valueNode.isNull()) {
                            totalPrecipitation += valueNode.asDouble();
                        }
                    }
                }
                log.info("Successfully fetched 3-day weather forecast. Total Precipitation: {} mm", totalPrecipitation);
                return totalPrecipitation;
            } else {
                log.warn("Weather API response did not contain daily precipitation fields. Defaulting to 0.0 mm");
                return 0.0;
            }
        } catch (Exception e) {
            log.error("Failed to retrieve weather forecast from Open-Meteo: {}. Defaulting to 0.0 mm", e.getMessage(), e);
            return 0.0;
        }
    }

    public double fetchThreeDayPrecipitation() {
        return fetchThreeDayPrecipitation(20.1677, 79.3048);
    }
}
