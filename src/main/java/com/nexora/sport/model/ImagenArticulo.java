package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Una foto de un articulo de inventario. Un articulo puede tener varias; una es la portada. */
@Entity
@Table(name = "imagenes_articulo")
@Getter
@Setter
public class ImagenArticulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "articulo_id", nullable = false)
    private ArticuloInventario articulo;

    @Column(nullable = false, length = 300)
    private String ruta;

    @Column(name = "es_principal", nullable = false)
    private boolean esPrincipal = false;

    @Column(name = "orden", nullable = false)
    private int orden = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
