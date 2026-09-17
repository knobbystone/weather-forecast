package com.example.weather.domain;

/** Raised when weather.gov cannot give us a usable forecast for a location. */
public class ForecastUnavailableException extends RuntimeException {

    public ForecastUnavailableException(String message) {
        super(message);
    }

    public ForecastUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
