package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Libro de caja: cada ingreso (mensualidad, inscripcion, clase particular,
 * venta de producto...) y cada egreso (renta, nomina, mantenimiento...) es
 * una fila aqui. Es la fuente de verdad para el dashboard y los reportes.
 */
@Entity
@Table(name = "movimientos_financieros")
@Getter
@Setter
public class MovimientoFinanciero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_id", nullable = false)
    private Centro centro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaMovimiento categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 20)
    private MetodoPago metodoPago = MetodoPago.EFECTIVO;

    @Column(length = 300)
    private String descripcion;

    @Column(nullable = false)
    private LocalDate fecha = LocalDate.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id")
    private Alumno alumno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membresia_id")
    private Membresia membresia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id")
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por")
    private Usuario registradoPor;

    /** Sucursal activa del actor al momento de registrar (ver TenantScope#sucursalActivaId).
     * Null cuando quien registra es SUPER_ADMIN/SUPERVISOR operando "todas las sucursales" --
     * en ese caso solo Dueno puede aprobar/rechazar el egreso (ver CajaService#assertPuedeResolver). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    /** Comprobante (PDF/imagen, ver FileStorageService) del egreso. Un ingreso nunca lo pide. */
    @Column(name = "comprobante_url", length = 300)
    private String comprobanteUrl;

    /** Un ingreso siempre nace APROBADO. Un egreso nace APROBADO si trae comprobante al
     * registrarse, o PENDIENTE si no -- mientras este PENDIENTE, queda fuera de las sumas
     * de reportes/dashboard (ver MovimientoFinancieroRepository) hasta que Dueno o el
     * Encargado de esa sucursal lo apruebe o rechace. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_aprobacion", nullable = false, length = 12)
    private EstadoAprobacion estadoAprobacion = EstadoAprobacion.APROBADO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resuelto_por")
    private Usuario resueltoPor;

    @Column(name = "resuelto_en")
    private LocalDateTime resueltoEn;

    @Column(name = "rechazado_motivo", length = 300)
    private String rechazadoMotivo;

    /**
     * Soft-void: true cuando el origen del movimiento (ej. un pago de membresia) se
     * cancelo/reverso. Nunca se borra la fila (trazabilidad); se excluye de sumas y
     * listados normales via CajaService/MovimientoFinancieroRepository.
     */
    @Column(nullable = false)
    private boolean anulado = false;

    @Column(name = "anulado_en")
    private LocalDateTime anuladoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anulado_por")
    private Usuario anuladoPor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
