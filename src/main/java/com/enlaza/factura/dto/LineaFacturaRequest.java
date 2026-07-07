package com.enlaza.factura.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LineaFacturaRequest(
        @NotBlank String descripcion,
        @NotNull @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor que 0") BigDecimal cantidad,
        @NotNull @DecimalMin(value = "0.0", message = "El precio unitario no puede ser negativo") BigDecimal precioUnitario,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal porcentajeIva,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal porcentajeIrpf
) {
}
