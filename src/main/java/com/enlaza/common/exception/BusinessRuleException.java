package com.enlaza.common.exception;

/**
 * Se lanza cuando una operacion viola una regla de negocio (no un error de validacion de input).
 * Ejemplos: intentar anular una factura ya anulada, asignar un CONTABLE a una empresa inactiva.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
