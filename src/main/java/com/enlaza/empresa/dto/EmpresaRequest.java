package com.enlaza.empresa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmpresaRequest(
        @NotBlank String razonSocial,
        @NotBlank String nif,
        String direccion,
        @Email String email,
        String telefono
) {
}
