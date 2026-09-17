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

    private static final int MAX_CONCURRENT_LOOKUPS = 4;

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
    public Mono<ForecastResult> todaysForecast() {
        return nwsClient.forecast()
                .flatMapMany(response -> Flux.fromIterable(getPeriods(response)))
                .filter(period -> period.startTime().toLocalDate().equals(LocalDate.now(clock)))
                .reduce((current, next) -> current.isDaytime() ? current : next)
                .map(ForecastService::toDailyForecast)
                .map(forecast -> new ForecastResult(List.of(forecast)))
                .switchIfEmpty(Mono.error(() -> new ForecastUnavailableException("no forecast available for today")));
    }

    /** Same upstream call, but cardinality is 0..N — one item per daytime period. */
    public Flux<DailyForecast> weekAhead() {
        return nwsClient.forecast()
                .flatMapMany(response -> Flux.fromIterable(getPeriods(response)))
                .filter(Period::isDaytime)
                .map(ForecastService::toDailyForecast);
    }

    /** Fan-out: N gridpoints in parallel, results interleaved as they arrive. */
    public Flux<DailyForecast> todaysForecasts(List<String> gridpoints) {
        return Flux.fromIterable(gridpoints)
                .flatMap(gp -> nwsClient.forecast(gp)
                        .flatMapMany(response -> Flux.fromIterable(getPeriods(response)))
                        .filter(period -> period.startTime().toLocalDate().equals(LocalDate.now(clock)))
                        .reduce((current, next) -> current.isDaytime() ? current : next)
                        .map(ForecastService::toDailyForecast),
                        MAX_CONCURRENT_LOOKUPS);
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
