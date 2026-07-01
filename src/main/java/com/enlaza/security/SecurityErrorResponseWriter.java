package com.enlaza.security;

import com.enlaza.common.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Unico punto que escribe ApiError como respuesta desde la cadena de filtros
 * de Spring Security (donde @RestControllerAdvice no llega). Usado por
 * RestAuthenticationEntryPoint y RestAccessDeniedHandler para no duplicar la
 * misma serializacion en dos sitios.
 */
@Component
@RequiredArgsConstructor
class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    void write(HttpServletResponse response, HttpServletRequest request, HttpStatus status, String message)
            throws IOException {
        ApiError apiError = ApiError.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), apiError);
    }
}
