package com.nexora.sport.repository;

import com.nexora.sport.model.Proveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    Page<Proveedor> findByCentroIdAndActivoTrue(Long centroId, Pageable pageable);
}
