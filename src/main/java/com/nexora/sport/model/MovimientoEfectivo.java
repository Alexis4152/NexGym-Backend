package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Movimiento de efectivo del cajon que no es una venta ni un pago de membresia (homologado con
 * DemoPV#CashMovement): gasto, retiro, aporte, fondo, prestamo entre cajas...
 *
 *  - Dentro de un corte abierto: corteCaja != null.
 *  - Con la caja cerrada (entre turnos, solo administradores): corteCaja == null; cuando el
 *    siguiente turno abre la caja, queda en recibidoPorCorte como "lo que vio al recibirla".
 */
@Entity
@Table(name = "movimientos_efectivo")
@Getter
@Setter
public class MovimientoEfectivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_id", nullable = false)
    private Centro centro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_registradora_id")
    private CajaRegistradora cajaRegistradora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corte_caja_id")
    private CorteCaja corteCaja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recibido_por_corte_id")
    private CorteCaja recibidoPorCorte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimientoEfectivo tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(length = 150)
    private String concepto;

    /** Quien puso/retiro el dinero (aporte, retiro). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id")
    private Usuario persona;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private DestinoRetiro destino;

    @Column(length = 100)
    private String referencia;

    @Column(name = "comprobante_url", length = 300)
    private String comprobanteUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por", nullable = false)
    private Usuario registradoPor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "ocurrido_en")
    private LocalDateTime ocurridoEn = LocalDateTime.now();

    /** Registrado despues de ocurrido (p. ej. al resolver una diferencia como "retiro no registrado"). */
    @Column(name = "registrado_despues", nullable = false)
    private boolean registradoDespues = false;

    /** Caja cerrada: cuanto debia quedar despues del movimiento, y cuanto conto el admin (si conto). */
    @Column(name = "restante_esperado", precision = 12, scale = 2)
    private BigDecimal restanteEsperado;

    @Column(name = "restante_contado", precision = 12, scale = 2)
    private BigDecimal restanteContado;

    /** Prestamo entre cajas: la otra caja. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_contraparte_id")
    private CajaRegistradora cajaContraparte;

    /** Gasto: su egreso en el libro de Caja (MovimientoFinanciero). */
    @Column(name = "movimiento_financiero_id")
    private Long movimientoFinancieroId;

    /** Fases 2: reembolso que genera un APORTE / que paga un REEMBOLSO, y faltante de un PAGO_FALTANTE. */
    @Column(name = "reembolso_id")
    private Long reembolsoId;

    @Column(name = "faltante_id")
    private Long faltanteId;
}
