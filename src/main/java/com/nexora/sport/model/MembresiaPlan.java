package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

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
    @Column(name = "tipo_periodo", nullable = false, length = 20)
    private TipoPeriodoMembresia tipoPeriodo;

    @Column(name = "duracion_dias")
    private Integer duracionDias;

    @Column(name = "numero_clases_incluidas")
    private Integer numeroClasesIncluidas;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private boolean multidisciplina = false;

    @Column(name = "acceso_completo", nullable = false)
    private boolean accesoCompleto = false;

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
