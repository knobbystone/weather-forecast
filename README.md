# weather-forecast

Spring WebFlux service that calls the National Weather Service API and returns
today's forecast: **day name, high temperature in celsius, short description**.

```json
{
  "daily": [{
    "day_name": "Wednesday",
    "temp_high_celsius": 25.0,
    "forecast_blurp": "Sunny"
  }]
}
```

## Run it

```bash
mvn spring-boot:run
curl localhost:8080/api/forecast/today
```

## Endpoint

`GET /api/forecast/today` — calls `api.weather.gov/gridpoints/MLB/33,70/forecast`,
filters to the current day's daytime period, converts Fahrenheit to Celsius, and
returns the result above.

## How the reactive chain works

```
nwsClient.forecast()               // Mono<ForecastResponse>  — one HTTP call
  .flatMapMany(periods)            // Flux<Period>            — bridge to many
  .filter(today)                   // only periods dated today
  .reduce(preferDaytime)           // Mono<Period>            — daytime wins, overnight fallback
  .map(fahrenheitToCelsius)        // sync 1:1 transform
  .map(ForecastResult::wrap)       // wrap in {"daily":[...]}
  .switchIfEmpty(error)            // no matching period → 502
```

## Tests

```bash
mvn test
```

`ForecastServiceTest` runs the real `WebClient` against MockWebServer with a fixed
`Clock`, so "today" is deterministic and no network is touched.

## Layout

```
domain/    DailyForecast, ForecastResult, ForecastUnavailableException
client/    NwsClient + trimmed records for the weather.gov forecast payload
service/   ForecastService — the reactive pipeline
web/       ForecastController + ProblemDetail exception handling
```
