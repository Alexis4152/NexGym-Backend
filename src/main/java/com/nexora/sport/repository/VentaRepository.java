package com.nexora.sport.repository;

import com.nexora.sport.model.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    List<Venta> findByCorteCajaId(Long corteCajaId);
    Page<Venta> findByCentroIdOrderByCreatedAtDesc(Long centroId, Pageable pageable);
}
