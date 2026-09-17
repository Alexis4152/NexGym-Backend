package com.nexora.sport.repository;

import com.nexora.sport.model.Sucursal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SucursalRepository extends JpaRepository<Sucursal, Long> {
    Page<Sucursal> findByCentroId(Long centroId, Pageable pageable);
    List<Sucursal> findByCentroIdAndActivoTrueOrderByNombre(Long centroId);
}
