package com.enlaza.factura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Los importes se calculan y redondean (HALF_UP, 2 decimales) por linea en
 * FacturaService antes de guardarse, nunca en el getter ni en el cliente:
 * los totales de Factura son la suma de estos valores ya redondeados, para
 * que la factura mostrada cuadre exactamente con lo que se sumo.
 *
 * "orden" fija la posicion de la linea dentro de su factura (0-based, orden
 * de envio en el request). El hash de la factura (FacturaHashCalculator)
 * concatena el contenido de las lineas en este mismo orden -- sin una
 * columna explicita, una relectura de la tabla no garantiza devolver las
 * filas en el orden en que se insertaron, lo que produciria un hash
 * recalculado distinto (falso positivo de manipulacion) al verificar.
 */
@Entity
@Table(name = "linea_factura")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineaFactura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "factura_id", nullable = false)
    private UUID facturaId;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 19, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "porcentaje_iva", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeIva;

    @Column(name = "porcentaje_irpf", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeIrpf;

    @Column(name = "importe_base", nullable = false, precision = 19, scale = 2)
    private BigDecimal importeBase;

    @Column(name = "importe_iva", nullable = false, precision = 19, scale = 2)
    private BigDecimal importeIva;

    @Column(name = "importe_irpf", nullable = false, precision = 19, scale = 2)
    private BigDecimal importeIrpf;

    @Column(name = "importe_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal importeTotal;

    @Column(nullable = false)
    private int orden;
}
