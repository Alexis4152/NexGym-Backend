package com.nexora.sport.repository;

import com.nexora.sport.model.Compra;
import com.nexora.sport.model.EstadoCompra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    Page<Compra> findByCentroId(Long centroId, Pageable pageable);
    List<Compra> findByCentroIdAndEstado(Long centroId, EstadoCompra estado);
}
