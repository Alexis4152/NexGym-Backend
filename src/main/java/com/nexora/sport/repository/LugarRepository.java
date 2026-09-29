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
           // cast(:x as long) en el "is null": ver nota en VentaRepository.
           "and (cast(:sucursalId as long) is null or l.sucursal.id = :sucursalId) " +
           "and (cast(:disciplinaId as long) is null or l.disciplinas is empty or :disciplinaId in (select d.id from l.disciplinas d)) " +
           "order by l.nombre")
    List<Lugar> buscarActivos(@Param("centroId") Long centroId,
                              @Param("sucursalId") Long sucursalId,
                              @Param("disciplinaId") Long disciplinaId);
}
