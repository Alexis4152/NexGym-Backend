package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Un pago o abono de una {@link Membresia}. Nunca se sobrescribe ni se borra: el
 * historial completo queda aqui. Cada pago VALIDO tiene un MovimientoFinanciero
 * hermano en Caja (misma transaccion); al cancelar un pago, ese movimiento se anula
 * (ver CajaService#anularMovimiento) en vez de borrarse, para conservar trazabilidad
 * en ambos lados.
 */
@Entity
@Table(name = "pagos_membresia")
@Getter
@Setter
public class PagoMembresia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membresia_id", nullable = false)
    private Membresia membresia;

    /** Corte de caja (turno) en el que se cobro: su efectivo cuenta en el arqueo de ese corte.
     * Null en el anticipo con tarjeta de la tienda en linea (no paso por ninguna caja). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corte_caja_id")
    private CorteCaja corteCaja;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 20)
    private MetodoPago metodoPago = MetodoPago.EFECTIVO;

    @Column(nullable = false)
    private LocalDate fecha = LocalDate.now();

    /** Solo efectivo: lo que entrego el cliente (puede ser mas que monto) y el cambio que se le
     * dio. monto es lo que realmente se abona; estos dos solo van al ticket. Null en otros metodos. */
    @Column(name = "monto_recibido", precision = 10, scale = 2)
    private BigDecimal montoRecibido;

    @Column(precision = 10, scale = 2)
    private BigDecimal cambio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado = EstadoPago.VALIDO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimiento_financiero_id")
    private MovimientoFinanciero movimientoFinanciero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por")
    private Usuario registradoPor;

    @Column(name = "motivo_cancelacion", length = 300)
    private String motivoCancelacion;

    @Column(name = "cancelado_en")
    private LocalDateTime canceladoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelado_por")
    private Usuario canceladoPor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
