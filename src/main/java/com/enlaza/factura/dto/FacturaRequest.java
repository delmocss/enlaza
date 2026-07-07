package com.enlaza.factura.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record FacturaRequest(
        @NotNull UUID clienteId,
        @NotEmpty(message = "La factura debe tener al menos una linea") @Valid List<LineaFacturaRequest> lineas
) {
}
