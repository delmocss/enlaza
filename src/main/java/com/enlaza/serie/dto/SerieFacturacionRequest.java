package com.enlaza.serie.dto;

import jakarta.validation.constraints.NotBlank;

public record SerieFacturacionRequest(
        @NotBlank String codigo
) {
}
