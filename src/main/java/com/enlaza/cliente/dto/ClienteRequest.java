package com.enlaza.cliente.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClienteRequest(
        @NotBlank String nombre,
        @NotBlank String nif,
        String direccion,
        @Email String email
) {
}
