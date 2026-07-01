package com.enlaza.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando una operacion viola una regla de negocio (no un error de validacion de input).
 * Ejemplos: intentar anular una factura ya anulada, asignar un CONTABLE a una empresa inactiva.
 */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
