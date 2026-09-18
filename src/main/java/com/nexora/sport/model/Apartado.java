package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reserva ("apartado"/layaway) de uno o varios articulos, hecha por un cliente desde la
 * tienda publica sin necesidad de cuenta ni login, y gestionada despues por el staff del
 * centro. A diferencia de una Venta, un apartado NO es una venta: es una promesa de compra.
 * El stock se descuenta hasta que un cajero lo confirma (PENDIENTE -> ACTIVO) — nunca al
 * momento de la solicitud publica, para que una solicitud falsa o de broma (no hay login ni
 * pago que filtre) no bloquee inventario sin que nadie la revise. Al completarse (el cliente
 * recoge y paga) se genera una Venta real (ver ventaId) SIN volver a descontar stock, ya que
 * se descarto al confirmar. Si se cancela estando ACTIVO, o si el job de vencimiento lo
 * encuentra vencido, el stock se restituye.
 */
@Entity
@Table(name = "apartados")
@Getter
@Setter
public class Apartado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_id", nullable = false)
    private Centro centro;

    @Column(name = "cliente_nombre", nullable = false, length = 150)
    private String clienteNombre;

    @Column(name = "cliente_telefono", nullable = false, length = 30)
    private String clienteTelefono;

    @Column(name = "cliente_email", length = 150)
    private String clienteEmail;

    @Column(length = 500)
    private String notas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoApartado estado = EstadoApartado.PENDIENTE;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "horas_vigencia")
    private Integer horasVigencia;

    @Column(name = "solicitado_en", nullable = false, updatable = false)
    private LocalDateTime solicitadoEn = LocalDateTime.now();

    @Column(name = "confirmado_en")
    private LocalDateTime confirmadoEn;

    @Column(name = "expira_en")
    private LocalDateTime expiraEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmado_por")
    private Usuario confirmadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completado_por")
    private Usuario completadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelado_por")
    private Usuario canceladoPor;

    @Column(name = "cancelado_en")
    private LocalDateTime canceladoEn;

    @Column(name = "motivo_cancelacion", length = 500)
    private String motivoCancelacion;

    @Column(name = "venta_id")
    private Long ventaId;

    @OneToMany(mappedBy = "apartado", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ApartadoItem> items = new ArrayList<>();
}
