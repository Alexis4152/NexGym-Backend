package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Reserva de un plan de membresia hecha por un cliente desde la tienda publica, pagando
 * un anticipo del 20% con tarjeta para apartar su lugar (seccion 41 del encargo). A
 * diferencia de Apartado (productos), esto NO descuenta ningun inventario ni cupo real:
 * solo deja folio + anticipo ya cobrado para que el staff de membresias complete el alta
 * (Membresia real) cuando el cliente se presente -- esta fila nunca crea la Membresia por
 * si sola. El pago de tarjeta es SIMULADO (no hay pasarela conectada todavia), pero el
 * anticipo de todas formas se refleja de inmediato en Caja (ver
 * CajaService#registrarIngresoDeApartadoPlan) para que cuente en los ingresos del centro.
 */
@Entity
@Table(name = "apartados_plan")
@Getter
@Setter
public class ApartadoPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_id", nullable = false)
    private Centro centro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private MembresiaPlan plan;

    /** Nombre/precio del plan al momento de apartarse (el plan pudo cambiar despues). */
    @Column(name = "plan_nombre_snapshot", nullable = false, length = 100)
    private String planNombreSnapshot;

    @Column(name = "precio_plan_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioPlanSnapshot;

    @Column(name = "monto_anticipo", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoAnticipo;

    @Column(name = "cliente_nombre", nullable = false, length = 150)
    private String clienteNombre;

    @Column(name = "cliente_telefono", nullable = false, length = 30)
    private String clienteTelefono;

    @Column(name = "cliente_email", nullable = false, length = 150)
    private String clienteEmail;

    @Column(length = 500)
    private String notas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoApartadoPlan estado = EstadoApartadoPlan.PENDIENTE;

    /** Id del MovimientoFinanciero (anticipo) creado en Caja al solicitarse. */
    @Column(name = "movimiento_financiero_id")
    private Long movimientoFinancieroId;

    @Column(name = "solicitado_en", nullable = false, updatable = false)
    private LocalDateTime solicitadoEn = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "convertido_por")
    private Usuario convertidoPor;

    @Column(name = "convertido_en")
    private LocalDateTime convertidoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelado_por")
    private Usuario canceladoPor;

    @Column(name = "cancelado_en")
    private LocalDateTime canceladoEn;

    @Column(name = "motivo_cancelacion", length = 500)
    private String motivoCancelacion;
}
