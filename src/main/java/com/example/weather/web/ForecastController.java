package com.example.weather.web;

import java.util.List;

import com.example.weather.domain.DailyForecast;
import com.example.weather.domain.ForecastResult;
import com.example.weather.service.ForecastService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {

    private final ForecastService forecastService;

    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @GetMapping("/today")
    public Mono<ForecastResult> today() {
        return forecastService.todaysForecast();
    }

    /** Same data, 0..N cardinality — one item per daytime period for the week. */
    @GetMapping("/week")
    public Flux<DailyForecast> week() {
        return forecastService.weekAhead();
    }

    /** Fan-out: several {@code gridpoint=MLB/33,70} params, results as they land. */
    @GetMapping("/today/batch")
    public Flux<DailyForecast> todayBatch(@RequestParam("gridpoint") List<String> gridpoints) {
        return forecastService.todaysForecasts(gridpoints);
    }
}
