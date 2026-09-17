package com.nexora.sport.repository;

import com.nexora.sport.model.Lugar;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LugarRepository extends JpaRepository<Lugar, Long> {
    Page<Lugar> findByCentroId(Long centroId, Pageable pageable);

    @Query("select l from Lugar l where l.centro.id = :centroId and l.activo = true " +
           "and (:sucursalId is null or l.sucursal.id = :sucursalId) " +
           "and (:disciplinaId is null or :disciplinaId in (select d.id from l.disciplinas d)) " +
           "order by l.nombre")
    List<Lugar> buscarActivos(@Param("centroId") Long centroId,
                              @Param("sucursalId") Long sucursalId,
                              @Param("disciplinaId") Long disciplinaId);
}
