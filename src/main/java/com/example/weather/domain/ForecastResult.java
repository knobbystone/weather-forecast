package com.example.weather.domain;

import java.util.List;

public record ForecastResult(List<DailyForecast> daily) {
}
