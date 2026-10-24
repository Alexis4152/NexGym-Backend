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

    /** Texto compuesto a partir de `domicilio` (lo leen tienda en linea, tickets, etc.). */
    @Column(length = 300)
    private String direccion;

    @Embedded
    private Domicilio domicilio;

    @Column(length = 300)
    private String notas;

    @Column(nullable = false)
    private boolean activo = true;
}
