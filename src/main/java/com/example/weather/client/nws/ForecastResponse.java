package com.example.weather.client.nws;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Trimmed view of the gridpoint forecast. Periods arrive in chronological order,
 * alternating day/night, starting with the period that is currently in progress.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ForecastResponse(Properties properties) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(List<Period> periods) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Period(
            int number,
            String name,
            OffsetDateTime startTime,
            boolean isDaytime,
            double temperature,
            String temperatureUnit,
            String shortForecast) {
    }
}
