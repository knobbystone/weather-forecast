package com.example.weather.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WeatherConfiguration {

    /**
     * weather.gov serves {@code application/geo+json}. Jackson's decoder claims any
     * {@code application/*+json} subtype, so no custom codec is needed.
     */
    @Bean
    WebClient nwsWebClient(WebClient.Builder builder, NwsProperties properties) {
        return builder.baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .defaultHeader(HttpHeaders.ACCEPT, "application/geo+json")
                .build();
    }

    /** Injected rather than using {@code LocalDate.now()} so "today" is testable. */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
