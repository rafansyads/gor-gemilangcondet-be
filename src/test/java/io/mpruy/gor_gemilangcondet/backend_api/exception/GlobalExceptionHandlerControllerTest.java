package io.mpruy.gor_gemilangcondet.backend_api.exception;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerControllerTest {

    private final GlobalExceptionHandlerController handler = new GlobalExceptionHandlerController();

    @Test
    @DisplayName("Should handle ResourceNotFoundException → 404")
    void handleResourceNotFound() {
        ResponseEntity<BaseResponseDto<Object>> result = handler
                .handleResourceNotFound(new ResourceNotFoundException("Not found"));
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertEquals("Not found", result.getBody().getMessage());
    }

    @Test
    @DisplayName("Should handle ConflictException → 409")
    void handleConflict() {
        ResponseEntity<BaseResponseDto<Object>> result = handler.handleConflict(new ConflictException("Conflict"));
        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
    }

    @Test
    @DisplayName("Should handle BadRequestException → 400")
    void handleBadRequest() {
        ResponseEntity<BaseResponseDto<Object>> result = handler.handleBadRequest(new BadRequestException("Bad"));
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    @DisplayName("Should handle InvalidTokenException → 401")
    void handleInvalidToken() {
        ResponseEntity<BaseResponseDto<Object>> result = handler
                .handleInvalidToken(new InvalidTokenException("Invalid"));
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    @DisplayName("Should handle AuthenticationException → 401")
    void handleAuthentication() {
        ResponseEntity<BaseResponseDto<Object>> result = handler
                .handleAuthentication(new BadCredentialsException("Bad creds"));
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertTrue(result.getBody().getMessage().contains("Autentikasi gagal"));
    }

    @Test
    @DisplayName("Should handle UnauthorizedException → 401")
    void handleUnauthorized() {
        ResponseEntity<BaseResponseDto<Object>> result = handler
                .handleUnauthorized(new UnauthorizedException("Unauth"));
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    @DisplayName("Should handle ForbiddenException → 403")
    void handleForbidden() {
        ResponseEntity<BaseResponseDto<Object>> result = handler.handleForbidden(new ForbiddenException("Forbidden"));
        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException → 400")
    void handleValidation() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "field", "must not be null");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<BaseResponseDto<Object>> result = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertTrue(result.getBody().getMessage().contains("must not be null"));
    }

    @Test
    @DisplayName("Should handle DataIntegrityViolationException → 409")
    void handleDataIntegrity() {
        ResponseEntity<BaseResponseDto<Object>> result = handler
                .handleDataIntegrity(new DataIntegrityViolationException("constraint"));
        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
    }

    @Test
    @DisplayName("Should handle TooManyRequestException → 429")
    void handleTooManyRequest() {
        ResponseEntity<BaseResponseDto<Object>> result = handler
                .handleTooManyRequest(new TooManyRequestException("Too many"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, result.getStatusCode());
    }

    @Test
    @DisplayName("Should handle generic Exception → 500")
    void handleGeneral() {
        ResponseEntity<BaseResponseDto<Object>> result = handler.handleGeneral(new Exception("Something broke"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertTrue(result.getBody().getMessage().contains("Terjadi kesalahan tak terduga"));
    }
}
