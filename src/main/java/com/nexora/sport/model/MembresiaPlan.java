package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Producto/configuracion que ofrece el gimnasio (catalogo). NO confundir con
 * {@link Membresia}, que es la instancia contratada por un alumno y conserva su propio
 * snapshot historico independiente de los cambios posteriores a este plan.
 */
@Entity
@Table(name = "membresia_planes")
@Getter
@Setter
public class MembresiaPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_id", nullable = false)
    private Centro centro;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_plan", nullable = false, length = 20)
    private TipoPlanMembresia tipoPlan;

    /** Obligatorio cuando tipoPlan=PERIODO; opcional (vigencia extra) cuando POR_CLASES. */
    @Column(name = "duracion_cantidad")
    private Integer duracionCantidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "duracion_unidad", length = 10)
    private UnidadDuracion duracionUnidad;

    /** Obligatorio cuando tipoPlan=POR_CLASES/PASE (PASE siempre queda fijo en 1). */
    @Column(name = "numero_clases_incluidas")
    private Integer numeroClasesIncluidas;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    /** Todas las disciplinas del centro, sin restriccion ni limite de cantidad. */
    @Column(name = "acceso_completo", nullable = false)
    private boolean accesoCompleto = false;

    /**
     * Cuantas disciplinas de {@link #disciplinas} puede elegir el alumno al contratar
     * (1 = una sola; >1 = "combo", ej. Combate Plus = 2 de 4). Sin efecto si
     * accesoCompleto=true o si disciplinas esta vacio.
     */
    @Column(name = "max_disciplinas_seleccionables")
    private Integer maxDisciplinasSeleccionables = 1;

    @Column(name = "permite_abonos", nullable = false)
    private boolean permiteAbonos = false;

    /** Pago minimo del primer abono cuando permiteAbonos=true (opcional). */
    @Column(name = "monto_minimo_abono", precision = 10, scale = 2)
    private BigDecimal montoMinimoAbono;

    /** Tope informativo de alumnos simultaneos con este plan (no se valida aun en asignacion). */
    @Column(name = "limite_alumnos")
    private Integer limiteAlumnos;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "membresia_plan_disciplinas",
            joinColumns = @JoinColumn(name = "plan_id"),
            inverseJoinColumns = @JoinColumn(name = "disciplina_id")
    )
    private Set<Disciplina> disciplinas = new HashSet<>();
}
