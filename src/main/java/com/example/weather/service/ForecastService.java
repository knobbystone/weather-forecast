package com.example.weather.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import com.example.weather.client.NwsClient;
import com.example.weather.client.nws.ForecastResponse;
import com.example.weather.client.nws.ForecastResponse.Period;
import com.example.weather.domain.DailyForecast;
import com.example.weather.domain.ForecastResult;
import com.example.weather.domain.ForecastUnavailableException;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ForecastService {

    private final NwsClient nwsClient;
    private final Clock clock;

    public ForecastService(NwsClient nwsClient, Clock clock) {
        this.nwsClient = nwsClient;
        this.clock = clock;
    }

    /**
     * One upstream call → one response → many periods, reduced to at most one.
     * {@code flatMapMany} bridges the single response into a {@code Flux<Period>} so the
     * date filter and daytime preference are plain reactive operators. {@code reduce}
     * collapses today's periods to one, keeping the daytime period if found, otherwise
     * yielding the overnight fallback.
     */
    public Mono<ForecastResult> todaysForecast(String office, int gridX, int gridY) {
        return nwsClient.forecast(office, gridX, gridY)
                .flatMapMany(response -> Flux.fromIterable(getPeriods(response)))
                .filter(period -> period.startTime().toLocalDate().equals(LocalDate.now(clock)))
                .reduce((current, next) -> current.isDaytime() ? current : next)
                .map(ForecastService::toDailyForecast)
                .map(forecast -> new ForecastResult(List.of(forecast)))
                .switchIfEmpty(Mono.error(() -> new ForecastUnavailableException("no forecast available for today")));
    }

    private static List<Period> getPeriods(ForecastResponse response) {
        return response.properties() != null && response.properties().periods() != null
                ? response.properties().periods()
                : List.of();
    }

    private static DailyForecast toDailyForecast(Period period) {
        double celsius = Math.round((period.temperature() - 32) * 5.0 / 9.0 * 10) / 10.0;
        return new DailyForecast(
                period.startTime().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                celsius,
                period.shortForecast());
    }
}
