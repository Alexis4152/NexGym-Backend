package com.nexora.sport.exception;

/** Error de negocio atribuible a un campo especifico (ej. nombre duplicado). */
public class FieldConflictException extends RuntimeException {
    private final String field;

    public FieldConflictException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
