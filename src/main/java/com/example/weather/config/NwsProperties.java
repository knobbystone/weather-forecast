package com.example.weather.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nws")
public record NwsProperties(String baseUrl, String userAgent, Duration requestTimeout) {
}
