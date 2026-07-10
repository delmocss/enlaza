package com.enlaza.serie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * @Pattern restringe codigo a alfanumerico/guiones: FacturaPdfService lo
 * interpola sin escapar en el nombre de archivo del header
 * Content-Disposition de la descarga de PDF -- sin este limite, un
 * codigo con una comilla doble rompe ese header para todas las facturas
 * de la serie.
 */
public record SerieFacturacionRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "El codigo solo admite letras, numeros, guiones y guiones bajos")
        String codigo
) {
}
