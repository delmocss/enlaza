package com.enlaza.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Las excepciones lanzadas dentro de la cadena de filtros de Spring Security
 * ocurren antes del DispatcherServlet, asi que @RestControllerAdvice
 * (GlobalExceptionHandler) nunca las ve. Este entry point es el unico sitio
 * donde se garantiza la misma forma de respuesta (ApiError) tambien para
 * fallos de autenticacion.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorResponseWriter errorResponseWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        errorResponseWriter.write(response, request, HttpStatus.UNAUTHORIZED, "Autenticacion requerida");
    }
}
