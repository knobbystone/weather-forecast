package com.example.weather.client;

import com.example.weather.client.nws.ForecastResponse;
import com.example.weather.config.NwsProperties;
import com.example.weather.domain.ForecastUnavailableException;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class NwsClient {

    private final WebClient webClient;
    private final NwsProperties properties;

    public NwsClient(WebClient nwsWebClient, NwsProperties properties) {
        this.webClient = nwsWebClient;
        this.properties = properties;
    }

    public Mono<ForecastResponse> forecast(String office, int gridX, int gridY) {
        return retrieve(webClient.get().uri(uriBuilder -> uriBuilder
                                .path("/gridpoints/{office}/{gridX},{gridY}/forecast")
                                .queryParam("units", "si")
                                .build(office, gridX, gridY)),
                ForecastResponse.class,
                "forecast lookup for %s/%d,%d".formatted(office, gridX, gridY));
    }

    private <T> Mono<T> retrieve(WebClient.RequestHeadersSpec<?> request, Class<T> responseType, String description) {
        return request.retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("<empty body>")
                        .map(body -> new ForecastUnavailableException(
                                "%s failed with %s: %s".formatted(description, response.statusCode(), body))))
                .bodyToMono(responseType)
                .timeout(properties.requestTimeout())
                .onErrorMap(error -> !(error instanceof ForecastUnavailableException),
                        error -> new ForecastUnavailableException(description + " failed", error));
    }
}
