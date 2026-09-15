package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Categoria configurable de un movimiento de caja (ej. "Mensualidad", "Renta",
 * "Nomina"). Se siembran categorias de sistema pero el admin puede agregar mas.
 */
@Entity
@Table(name = "categorias_movimiento")
@Getter
@Setter
public class CategoriaMovimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_id", nullable = false)
    private Centro centro;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    @Column(name = "es_sistema", nullable = false)
    private boolean esSistema = false;

    @Column(nullable = false)
    private boolean activo = true;
}
