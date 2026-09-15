package com.nexora.sport.repository;

import com.nexora.sport.model.CategoriaInventario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaInventarioRepository extends JpaRepository<CategoriaInventario, Long> {
    List<CategoriaInventario> findByCentroIdAndActivoTrue(Long centroId);
}
