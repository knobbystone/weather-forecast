package com.example.weather.service;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.example.weather.client.NwsClient;
import com.example.weather.config.NwsProperties;
import com.example.weather.domain.DailyForecast;
import com.example.weather.domain.ForecastResult;
import com.example.weather.domain.ForecastUnavailableException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class ForecastServiceTest {

    private MockWebServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void stopServer() throws IOException {
        server.shutdown();
    }

    @Test
    void returnsDayNameCelsiusAndShortDescriptionForToday() {
        enqueueForecast("""
                {
                  "properties": {
                    "periods": [
                      {"number": 1, "name": "Today", "startTime": "2026-09-16T06:00:00Z",
                       "isDaytime": true, "temperature": 25.0, "temperatureUnit": "C",
                       "shortForecast": "Sunny", "detailedForecast": "Sunny."},
                      {"number": 2, "name": "Tonight", "startTime": "2026-09-16T18:00:00Z",
                       "isDaytime": false, "temperature": 17.2, "temperatureUnit": "C",
                       "shortForecast": "Mostly Clear", "detailedForecast": "Mostly clear."},
                      {"number": 3, "name": "Thursday", "startTime": "2026-09-17T06:00:00Z",
                       "isDaytime": true, "temperature": 27.2, "temperatureUnit": "C",
                       "shortForecast": "Chance Showers", "detailedForecast": "A chance of showers."}
                    ]
                  }
                }
                """);

        StepVerifier.create(serviceAt("2026-09-16T12:00:00Z").todaysForecast("MLB", 33, 70))
                .expectNext(new ForecastResult(List.of(new DailyForecast("Wednesday", 25.0, "Sunny"))))
                .verifyComplete();
    }

    @Test
    void fallsBackToTheOvernightPeriodWhenTheDaytimePeriodHasPassed() {
        enqueueForecast("""
                {
                  "properties": {
                    "periods": [
                      {"number": 1, "name": "Tonight", "startTime": "2026-09-16T18:00:00Z",
                       "isDaytime": false, "temperature": 17.2, "temperatureUnit": "C",
                       "shortForecast": "Mostly Clear", "detailedForecast": "Mostly clear."},
                      {"number": 2, "name": "Thursday", "startTime": "2026-09-17T06:00:00Z",
                       "isDaytime": true, "temperature": 27.2, "temperatureUnit": "C",
                       "shortForecast": "Chance Showers", "detailedForecast": "A chance of showers."}
                    ]
                  }
                }
                """);

        // 8pm UTC: daytime has passed, only the overnight period remains for today.
        StepVerifier.create(serviceAt("2026-09-16T20:00:00Z").todaysForecast("MLB", 33, 70))
                .expectNext(new ForecastResult(List.of(new DailyForecast("Wednesday", 17.2, "Mostly Clear"))))
                .verifyComplete();
    }

    @Test
    void roundsTheSiFullPrecisionCelsiusValue() {
        // units=si can return the full conversion e.g. 12.777777777777779
        enqueueForecast(singlePeriodBody("Sunny", "12.777777777777779"));

        StepVerifier.create(serviceAt("2026-09-16T12:00:00Z").todaysForecast("MLB", 33, 70))
                .expectNext(new ForecastResult(List.of(new DailyForecast("Wednesday", 12.8, "Sunny"))))
                .verifyComplete();
    }

    @Test
    void surfacesAnUpstreamFailureAsForecastUnavailable() {
        server.enqueue(new MockResponse().setResponseCode(404)
                .setBody("{\"detail\":\"Unable to provide data for requested point\"}"));

        StepVerifier.create(serviceAt("2026-09-16T12:00:00Z").todaysForecast("MLB", 33, 70))
                .expectError(ForecastUnavailableException.class)
                .verify();
    }

    @Test
    void errorsWhenNoPeriodCoversToday() {
        enqueueForecast("{\"properties\": {\"periods\": []}}");

        StepVerifier.create(serviceAt("2026-09-16T12:00:00Z").todaysForecast("MLB", 33, 70))
                .expectError(ForecastUnavailableException.class)
                .verify();
    }

    @Test
    void asksWeatherGovWithCorrectPathAndHeaders() throws InterruptedException {
        enqueueForecast("{\"properties\": {\"periods\": []}}");

        serviceAt("2026-09-16T12:00:00Z").todaysForecast("MLB", 33, 70)
                .onErrorComplete()
                .block();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/gridpoints/MLB/33,70/forecast?units=si");
        assertThat(request.getHeader(HttpHeaders.USER_AGENT)).contains("@");
        assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/geo+json");
    }

    private ForecastService serviceAt(String now) {
        NwsProperties properties = new NwsProperties("http://localhost:" + server.getPort(),
                "(weather-forecast test, promo-prep@example.com)", Duration.ofSeconds(5));
        WebClient webClient = WebClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .defaultHeader(HttpHeaders.ACCEPT, "application/geo+json")
                .build();
        return new ForecastService(new NwsClient(webClient, properties),
                Clock.fixed(Instant.parse(now), ZoneOffset.UTC));
    }

    private void enqueueForecast(String body) {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/geo+json")
                .setBody(body));
    }

    private static String singlePeriodBody(String shortForecast, String celsius) {
        return """
                {
                  "properties": {
                    "periods": [
                      {"number": 1, "name": "Today", "startTime": "2026-09-16T06:00:00Z",
                       "isDaytime": true, "temperature": %s, "temperatureUnit": "C",
                       "shortForecast": "%s", "detailedForecast": "..."}
                    ]
                  }
                }
                """.formatted(celsius, shortForecast);
    }
}
