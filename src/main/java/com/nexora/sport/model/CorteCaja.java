package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Turno de un cajero sobre una {@link CajaRegistradora} (homologado con DemoPV#CashCut). Al
 * abrir RECIBE la caja: cuenta lo que dejo el turno anterior (montoInicial) contra lo que debia
 * recibir (montoInicialEsperado) -> estadoEntrega. Mientras esta abierto acumula ventas, pagos
 * de membresia y movimientos de efectivo. Al cerrar hace ARQUEO: lo contado (montoContado)
 * contra el esperado (montoFinal) -> estadoArqueo; lo contado queda en la caja para el
 * siguiente turno (montoEntregado).
 *
 * Un corte abierto por cajero y uno por caja. Dueno/Administrador pueden abrir mas de uno el
 * mismo dia; el resto del personal solo uno por dia.
 *
 * Las columnas agregadas con las cajas son nullable a proposito (ddl-auto=update no puede
 * agregar NOT NULL a una tabla con datos): null = corte anterior a las cajas / no aplica.
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

    // ---------------------------------------------------------------- caja y entrega
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_registradora_id")
    private CajaRegistradora cajaRegistradora;

    /** Corte anterior de la misma caja (de quien se recibio). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corte_anterior_id")
    private CorteCaja corteAnterior;

    /** Lo que DEBIA recibir al abrir (entrega pendiente de la caja); null = sin entrega previa. */
    @Column(name = "monto_inicial_esperado", precision = 12, scale = 2)
    private BigDecimal montoInicialEsperado;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_conteo_recibido", length = 20)
    private MetodoConteo metodoConteoRecibido;

    @Column(name = "nota_recepcion", length = 500)
    private String notaRecepcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_entrega", length = 12)
    private EstadoDiferencia estadoEntrega;

    @Column(name = "diferencia_entrega", precision = 12, scale = 2)
    private BigDecimal diferenciaEntrega;

    // ---------------------------------------------------------------- totales del turno
    /** Pagos de membresia cobrados en el turno, por metodo (el efectivo cuenta en el arqueo). */
    @Column(name = "membresias_efectivo", precision = 12, scale = 2)
    private BigDecimal membresiasEfectivo;

    @Column(name = "membresias_tarjeta", precision = 12, scale = 2)
    private BigDecimal membresiasTarjeta;

    @Column(name = "membresias_transferencia", precision = 12, scale = 2)
    private BigDecimal membresiasTransferencia;

    @Column(name = "membresias_otro", precision = 12, scale = 2)
    private BigDecimal membresiasOtro;

    @Column(name = "pagos_membresia")
    private Integer pagosMembresia;

    @Column(name = "total_entradas", precision = 12, scale = 2)
    private BigDecimal totalEntradas;

    @Column(name = "total_retiros", precision = 12, scale = 2)
    private BigDecimal totalRetiros;

    @Column(name = "total_reembolsos", precision = 12, scale = 2)
    private BigDecimal totalReembolsos;

    @Column(name = "total_prestamos_salida", precision = 12, scale = 2)
    private BigDecimal totalPrestamosSalida;

    // ---------------------------------------------------------------- arqueo de cierre
    /** Efectivo contado al cerrar; null = nadie conto (cierre forzado sin conteo / automatico). */
    @Column(name = "monto_contado", precision = 12, scale = 2)
    private BigDecimal montoContado;

    /** montoContado - montoFinal (esperado). */
    @Column(name = "diferencia_efectivo", precision = 12, scale = 2)
    private BigDecimal diferenciaEfectivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_conteo", length = 20)
    private MetodoConteo metodoConteo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contado_por")
    private Usuario contadoPor;

    @Column(name = "evidencia_conteo_url", length = 300)
    private String evidenciaConteoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_arqueo", length = 12)
    private EstadoDiferencia estadoArqueo;

    /** Lo que se deja en la caja para el siguiente turno (lo contado, o el esperado si nadie conto). */
    @Column(name = "monto_entregado", precision = 12, scale = 2)
    private BigDecimal montoEntregado;
}
