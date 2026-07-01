package com.enlaza.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base para excepciones de dominio que se traducen directamente a una
 * respuesta HTTP. Cada subclase declara su propio status; GlobalExceptionHandler
 * tiene un unico @ExceptionHandler(ApiException.class) en vez de un metodo por
 * excepcion -- necesario porque cada fase nueva del proyecto anade sus propias
 * excepciones de negocio (huecos en la numeracion, cadena de hashes rota,
 * empresa no asignada...).
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
