package io.mpruy.gor_gemilangcondet.backend_api.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for all controllers. This is where we define
 * custom handling for exceptions thrown by any controller method, allowing us
 * to return consistent error responses across the entire API.
 * 
 * Note: We use @RestControllerAdvice to ensure that all responses are in JSON format.
 * The @RequestMapping("/error") is optional and can be used to specify a base path for error handling if needed.
 */
@RestControllerAdvice
@RequestMapping("/error")
public class GlobalExceptionHandlerController {
    
}
