package com.enlaza.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, message = "La nueva contrasena debe tener al menos 8 caracteres") String newPassword
) {
}
