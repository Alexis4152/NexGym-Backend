package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Inventario general unico: equipo/herramienta de disciplina y producto de
 * tienda (ropa) conviven aqui, distinguidos por {@link #tipo} y {@link #vendible}.
 * Se relaciona N:N con disciplinas para poder clasificar un mismo articulo
 * (ej. kettlebell) en varias disciplinas a la vez.
 */
@Entity
@Table(name = "articulos_inventario")
@Getter
@Setter
public class ArticuloInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_id", nullable = false)
    private Centro centro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private CategoriaInventario categoria;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoArticulo tipo = TipoArticulo.EQUIPO;

    @Column(name = "codigo_barras", length = 80)
    private String codigoBarras;

    @Column(nullable = false)
    private int stock = 0;

    @Column(name = "stock_minimo", nullable = false)
    private int stockMinimo = 0;

    @Column(precision = 10, scale = 2)
    private BigDecimal costo;

    @Column(name = "precio_venta", precision = 10, scale = 2)
    private BigDecimal precioVenta;

    @Column(nullable = false)
    private boolean vendible = false;

    @Column(name = "imagen_url", length = 300)
    private String imagenUrl;

    @Column(nullable = false)
    private boolean reservable = false;

    @Column(name = "descuento_apartado_porcentaje", precision = 5, scale = 2)
    private BigDecimal descuentoApartadoPorcentaje;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "articulo_disciplinas",
            joinColumns = @JoinColumn(name = "articulo_id"),
            inverseJoinColumns = @JoinColumn(name = "disciplina_id")
    )
    private Set<Disciplina> disciplinas = new HashSet<>();
}
