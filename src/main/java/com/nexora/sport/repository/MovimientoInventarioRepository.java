package com.nexora.sport.repository;

import com.nexora.sport.model.MovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    Page<MovimientoInventario> findByArticuloIdOrderByCreatedAtDesc(Long articuloId, Pageable pageable);
}
