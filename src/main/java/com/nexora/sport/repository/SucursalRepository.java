package com.nexora.sport.repository;

import com.nexora.sport.model.Sucursal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SucursalRepository extends JpaRepository<Sucursal, Long> {
    Page<Sucursal> findByCentroId(Long centroId, Pageable pageable);
    List<Sucursal> findByCentroIdAndActivoTrueOrderByNombre(Long centroId);
    boolean existsByCentroIdAndActivoTrue(Long centroId);

    /** Alcance por sucursal (ver TenantScope#sucursalesPermitidas) para Admin/Operativo:
     * solo las sucursales que tienen asignadas, no todas las del centro. */
    Page<Sucursal> findByCentroIdAndIdIn(Long centroId, Collection<Long> ids, Pageable pageable);
    List<Sucursal> findByCentroIdAndIdInAndActivoTrueOrderByNombre(Long centroId, Collection<Long> ids);
}
