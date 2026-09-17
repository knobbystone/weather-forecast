package com.example.weather.web;

import com.example.weather.domain.ForecastUnavailableException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ForecastExceptionHandler {

    /** An upstream problem is not the caller's fault, so 502 rather than 500. */
    @ExceptionHandler(ForecastUnavailableException.class)
    ProblemDetail handleForecastUnavailable(ForecastUnavailableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
