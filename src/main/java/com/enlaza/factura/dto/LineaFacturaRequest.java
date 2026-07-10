package com.enlaza.factura.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * @Digits(fraction = 2) en los 4 importes/porcentajes no es solo cosmetico:
 * las columnas NUMERIC de linea_factura son todas escala 2, y
 * FacturaService.calcularLinea() redondea a esa misma escala antes de
 * usarlos en el hash. Sin este limite, un valor con mas de 2 decimales
 * (que @DecimalMin/@DecimalMax no rechazan) se redondearia de forma
 * distinta segun se use antes o despues de multiplicar, cambiando el
 * importe calculado respecto a lo que el cliente realmente envio.
 */
public record LineaFacturaRequest(
        @NotBlank String descripcion,
        @NotNull @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor que 0")
        @Digits(integer = 17, fraction = 2, message = "La cantidad admite como maximo 2 decimales") BigDecimal cantidad,
        @NotNull @DecimalMin(value = "0.0", message = "El precio unitario no puede ser negativo")
        @Digits(integer = 17, fraction = 2, message = "El precio unitario admite como maximo 2 decimales") BigDecimal precioUnitario,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0")
        @Digits(integer = 3, fraction = 2, message = "El IVA admite como maximo 2 decimales") BigDecimal porcentajeIva,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0")
        @Digits(integer = 3, fraction = 2, message = "El IRPF admite como maximo 2 decimales") BigDecimal porcentajeIrpf
) {
}
