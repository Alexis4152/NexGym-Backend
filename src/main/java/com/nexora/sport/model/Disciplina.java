package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Disciplina configurable (box, crossfit, pesas...). A proposito NO es un enum:
 * el administrador debe poder crear disciplinas nuevas sin tocar codigo.
 */
@Entity
@Table(name = "disciplinas")
@Getter
@Setter
public class Disciplina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_id", nullable = false)
    private Centro centro;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(length = 10)
    private String icono;

    @Column(length = 9)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModalidadDisciplina modalidad = ModalidadDisciplina.CLASES;

    @Column(name = "limite_alumnos")
    private Integer limiteAlumnos;

    @Column(nullable = false)
    private boolean activo = true;
}
