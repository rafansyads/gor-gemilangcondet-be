package io.mpruy.gor_gemilangcondet.backend_api.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.util.ResponseUtil;

/**
 * Global exception handler for all controllers. Centralises error responses so
 * controllers stay free of try-catch blocks.
 * 
 * @author rafansyads
 */
@RestControllerAdvice
public class GlobalExceptionHandlerController {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseResponseDto<Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<BaseResponseDto<Object>> handleConflict(ConflictException ex) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<BaseResponseDto<Object>> handleBadRequest(BadRequestException ex) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<BaseResponseDto<Object>> handleInvalidToken(InvalidTokenException ex) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<BaseResponseDto<Object>> handleAuthentication(AuthenticationException ex) {
        return ResponseUtil.error("Authentication failed: " + ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<BaseResponseDto<Object>> handleForbidden(ForbiddenException ex) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponseDto<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseUtil.error(message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponseDto<Object>> handleGeneral(Exception ex) {
        return ResponseUtil.error("An unexpected error occurred: " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
