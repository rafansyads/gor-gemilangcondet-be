package io.mpruy.gor_gemilangcondet.backend_api.exception;

import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
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

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<BaseResponseDto<Object>> handleIllegalStateException(IllegalStateException ex) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<BaseResponseDto<Object>> handleInvalidToken(InvalidTokenException ex) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<BaseResponseDto<Object>> handleAuthentication(AuthenticationException ex) {
        return ResponseUtil.error("Autentikasi gagal: " + ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<BaseResponseDto<Object>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<BaseResponseDto<Object>> handleForbidden(ForbiddenException ex) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({ AccessDeniedException.class, AuthorizationDeniedException.class })
    public ResponseEntity<BaseResponseDto<Object>> handleAccessDenied(RuntimeException ex) {
        return ResponseUtil.error("Akses ditolak", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponseDto<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseUtil.error(message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<BaseResponseDto<Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseUtil.error("Data constraint violation. Operasi tidak dapat dilakukan.",
                HttpStatus.CONFLICT);
    }

    @ExceptionHandler(TooManyRequestException.class)
    public ResponseEntity<BaseResponseDto<Object>> handleTooManyRequest(TooManyRequestException ex) {
        return ResponseUtil.error("Terlalu banyak request: " + ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<BaseResponseDto<Object>> handleExternalService(ExternalServiceException ex) {
        return ResponseUtil.error("Layanan eksternal tidak tersedia: " + ex.getMessage(),
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponseDto<Object>> handleGeneral(Exception ex) {
        return ResponseUtil.error("Terjadi kesalahan tak terduga: " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
