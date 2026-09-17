package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Sucursal fisica de un Centro. Un Centro puede tener una o varias. */
@Entity
@Table(name = "sucursales")
@Getter
@Setter
public class Sucursal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_id", nullable = false)
    private Centro centro;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 300)
    private String direccion;

    @Column(length = 300)
    private String notas;

    @Column(nullable = false)
    private boolean activo = true;
}
