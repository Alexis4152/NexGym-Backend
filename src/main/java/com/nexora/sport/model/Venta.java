package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Venta de mostrador (punto de venta) del catalogo de Inventario/Tienda. */
@Entity
@Table(name = "ventas")
@Getter
@Setter
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_id", nullable = false)
    private Centro centro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Sucursal del vendedor AL MOMENTO de la venta (seccion 36 del encargo): se guarda aqui
     * en vez de leerse en vivo de usuario.sucursal porque esa referencia es mutable -- si el
     * cajero cambia de sucursal despues, sus ventas viejas no se deben "mover" con el. Null
     * solo en el caso raro de un Dueno/SUPER_ADMIN vendiendo sin sucursal activa elegida. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corte_caja_id", nullable = false)
    private CorteCaja corteCaja;

    @Column(name = "cliente_nombre", length = 150)
    private String clienteNombre;

    @Column(name = "cliente_email", length = 150)
    private String clienteEmail;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal impuesto = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "monto_recibido", precision = 12, scale = 2)
    private BigDecimal montoRecibido;

    @Column(name = "cambio", precision = 12, scale = 2)
    private BigDecimal cambio;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 20)
    private MetodoPago metodoPago = MetodoPago.EFECTIVO;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ticket", nullable = false, length = 10)
    private TipoTicket tipoTicket = TipoTicket.NINGUNO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoVenta estado = EstadoVenta.COMPLETADA;

    @Column(length = 300)
    private String notas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelada_por")
    private Usuario canceladaPor;

    @Column(name = "cancelada_en")
    private LocalDateTime canceladaEn;

    /** El MovimientoFinanciero hermano creado por CajaService#registrarIngresoDeVenta.
     * Sin esta FK, cancelar la venta no podia encontrarlo para anularlo (soft-void) y el
     * dashboard/reportes seguian contando el ingreso de una venta ya cancelada. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimiento_financiero_id")
    private MovimientoFinanciero movimientoFinanciero;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VentaItem> items = new ArrayList<>();
}
