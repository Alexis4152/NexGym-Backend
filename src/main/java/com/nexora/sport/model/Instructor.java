package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "instructores")
@Getter
@Setter
public class Instructor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_id", nullable = false)
    private Centro centro;

    /** Cuenta de login ligada a esta ficha (nullable: la mayoria de instructores son solo un registro de
     * roster, sin acceso al sistema). Cuando existe, permite acotar "mis clases/reservas" al iniciar sesion. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 30)
    private String telefono;

    @Column(length = 150)
    private String email;

    @Column(length = 200)
    private String especialidad;

    @Column(name = "foto_url", length = 300)
    private String fotoUrl;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "instructor_disciplinas",
            joinColumns = @JoinColumn(name = "instructor_id"),
            inverseJoinColumns = @JoinColumn(name = "disciplina_id")
    )
    private Set<Disciplina> disciplinas = new HashSet<>();

    /** Sucursales donde este instructor esta disponible para impartir clases. Vacio =
     * disponible en todas (comportamiento por defecto, no pierde nada quien ya existia). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "instructor_sucursales",
            joinColumns = @JoinColumn(name = "instructor_id"),
            inverseJoinColumns = @JoinColumn(name = "sucursal_id")
    )
    private Set<Sucursal> sucursales = new HashSet<>();
}
