package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Caja: un cajon fisico de efectivo de una sucursal (homologado con DemoPV#CashRegister).
 * Cada {@link CorteCaja} (turno) se abre sobre una caja, y una caja solo puede tener un corte
 * abierto a la vez.
 *
 * Mientras la caja esta cerrada (sin corte abierto), montoPendiente es el efectivo que DEBE
 * haber en ella: lo que dejo el ultimo corte al cerrar, mas/menos lo que un administrador haya
 * movido con la caja cerrada. Quien abre el siguiente corte recibe esa cantidad, la cuenta y
 * da su de acuerdo. Los campos *Pendiente* quedan en null mientras hay un corte abierto (o si
 * la caja nunca se ha usado).
 */
@Entity
@Table(name = "cajas_registradoras")
@Getter
@Setter
public class CajaRegistradora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_id", nullable = false)
    private Centro centro;

    /** Null solo si el centro no tiene sucursales registradas. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "monto_pendiente", precision = 12, scale = 2)
    private BigDecimal montoPendiente;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_conteo_pendiente", length = 20)
    private MetodoConteo metodoConteoPendiente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contado_pendiente_por")
    private Usuario contadoPendientePor;

    @Column(name = "contado_pendiente_en")
    private LocalDateTime contadoPendienteEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ultimo_corte_id")
    private CorteCaja ultimoCorte;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
