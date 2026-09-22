package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * La suscripcion concreta de un alumno a un {@link MembresiaPlan}, en un momento dado.
 * Conserva su propio snapshot (precioOriginal/descuento/precioFinal/tipoPlanSnapshot/
 * numeroClasesContratadas/disciplinas seleccionadas) precisamente para que un cambio
 * posterior al plan (precio, disciplinas, etc.) NUNCA altere una membresia ya
 * contratada. Una renovacion crea SIEMPRE una fila nueva (nunca sobrescribe esta),
 * enlazada via membresiaAnterior.
 */
@Entity
@Table(name = "membresias")
@Getter
@Setter
public class Membresia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_id", nullable = false)
    private Centro centro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private MembresiaPlan plan;

    /** Nombre del plan al momento de contratar (el plan pudo renombrarse/desactivarse despues). */
    @Column(name = "plan_nombre_snapshot", nullable = false, length = 100)
    private String planNombreSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_plan_snapshot", nullable = false, length = 20)
    private TipoPlanMembresia tipoPlanSnapshot;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    /** Null cuando el plan es POR_CLASES/PASE sin vigencia configurada: solo se agota por clases (ver seccion 17). */
    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    /** Snapshot de plan.numeroClasesIncluidas al contratar (null si el plan no es por clases). */
    @Column(name = "numero_clases_contratadas")
    private Integer numeroClasesContratadas;

    @Column(name = "clases_restantes")
    private Integer clasesRestantes;

    @Column(name = "precio_original", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioOriginal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    /** precioOriginal - descuento. Es el total contratado (lo que debe cubrirse con pagos). */
    @Column(name = "precio_final", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioFinal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoMembresia estado = EstadoMembresia.ACTIVA;

    /** Disciplinas que el alumno eligio para ESTA vigencia (subconjunto de plan.disciplinas, o vacio si el plan no restringe/accesoCompleto). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "membresia_disciplinas",
            joinColumns = @JoinColumn(name = "membresia_id"),
            inverseJoinColumns = @JoinColumn(name = "disciplina_id")
    )
    private Set<Disciplina> disciplinas = new HashSet<>();

    /** Si esta membresia nace de una renovacion, la membresia que reemplaza (nunca se sobrescribe). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membresia_anterior_id")
    private Membresia membresiaAnterior;

    @Column(name = "suspendida_motivo", length = 300)
    private String suspendidaMotivo;

    @Column(name = "suspendida_en")
    private LocalDateTime suspendidaEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suspendida_por")
    private Usuario suspendidaPor;

    @Column(name = "cancelada_motivo", length = 300)
    private String canceladaMotivo;

    @Column(name = "cancelada_en")
    private LocalDateTime canceladaEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelada_por")
    private Usuario canceladaPor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
