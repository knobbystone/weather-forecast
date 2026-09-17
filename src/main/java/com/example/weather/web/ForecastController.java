package com.example.weather.web;

import com.example.weather.domain.ForecastResult;
import com.example.weather.service.ForecastService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {

    private final ForecastService forecastService;

    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @GetMapping("/today")
    public Mono<ForecastResult> today(@RequestParam String office,
                                      @RequestParam int gridX,
                                      @RequestParam int gridY) {
        return forecastService.todaysForecast(office, gridX, gridY);
    }
}
