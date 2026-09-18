package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Turno de caja de un cajero: abre con un fondo inicial, acumula las ventas que
 * registre mientras esta abierto, y se cierra con un resumen. Varios cortes pueden
 * estar abiertos a la vez (uno por cajero); Dueno/Administrador pueden abrir mas
 * de uno el mismo dia, el resto del personal solo uno por dia.
 */
@Entity
@Table(name = "cortes_caja")
@Getter
@Setter
public class CorteCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_id", nullable = false)
    private Centro centro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cerrado_por")
    private Usuario cerradoPor;

    @Column(name = "monto_inicial", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoInicial = BigDecimal.ZERO;

    @Column(name = "monto_final", precision = 12, scale = 2)
    private BigDecimal montoFinal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal gastos = BigDecimal.ZERO;

    @Column(name = "total_ventas", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalVentas = BigDecimal.ZERO;

    @Column(name = "ventas_efectivo", nullable = false, precision = 12, scale = 2)
    private BigDecimal ventasEfectivo = BigDecimal.ZERO;

    @Column(name = "ventas_tarjeta", nullable = false, precision = 12, scale = 2)
    private BigDecimal ventasTarjeta = BigDecimal.ZERO;

    @Column(name = "ventas_transferencia", nullable = false, precision = 12, scale = 2)
    private BigDecimal ventasTransferencia = BigDecimal.ZERO;

    @Column(name = "ventas_otro", nullable = false, precision = 12, scale = 2)
    private BigDecimal ventasOtro = BigDecimal.ZERO;

    @Column(name = "total_transacciones", nullable = false)
    private int totalTransacciones = 0;

    @Column(name = "canceladas", nullable = false)
    private int canceladas = 0;

    @Column(name = "total_cancelado", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCancelado = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoCorteCaja estado = EstadoCorteCaja.ABIERTO;

    @Column(length = 300)
    private String notas;

    @Column(name = "abierto_en", nullable = false, updatable = false)
    private LocalDateTime abiertoEn = LocalDateTime.now();

    @Column(name = "cerrado_en")
    private LocalDateTime cerradoEn;
}
