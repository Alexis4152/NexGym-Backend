package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Rol por centro (o de plataforma cuando centro es null: SUPER_ADMIN).
 * La autorizacion fina se resuelve por el conjunto de {@link Seccion} asignado.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_id")
    private Centro centro;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(name = "es_sistema", nullable = false)
    private boolean esSistema = false;

    @Column(nullable = false)
    private boolean activo = true;

    @ElementCollection(targetClass = Seccion.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "role_secciones", joinColumns = @JoinColumn(name = "role_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "seccion")
    private Set<Seccion> secciones = new HashSet<>();
}
