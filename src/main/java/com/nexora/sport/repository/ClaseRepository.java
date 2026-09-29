package com.nexora.sport.repository;

import com.nexora.sport.model.Clase;
import com.nexora.sport.model.DiaSemana;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public interface ClaseRepository extends JpaRepository<Clase, Long> {
    Page<Clase> findByCentroId(Long centroId, Pageable pageable);
    List<Clase> findByCentroIdAndActivoTrue(Long centroId);
    List<Clase> findByInstructorId(Long instructorId);

    /** Alcance por sucursal (una o varias, ver TenantScope#sucursalesPermitidas) y/o por
     * instructor-ownership (null en cualquiera = sin esa restriccion). */
    @Query("select c from Clase c where c.centro.id = :centroId " +
           "and (:sucursalIds is null or c.sucursal.id in :sucursalIds) " +
           // cast(:instructorId as long) is null: sin el cast, esta ocurrencia suelta
           // (sin comparar contra ninguna columna) no le da a Postgres forma de inferir
           // el tipo del parametro y truena "no se pudo determinar el tipo" -- ver
           // reporte final (mismo bug que el de lower(bytea), pero para tipos no-string).
           "and (cast(:instructorId as long) is null or c.instructor.id = :instructorId)")
    Page<Clase> buscarEnAlcance(@Param("centroId") Long centroId, @Param("sucursalIds") Set<Long> sucursalIds,
                                 @Param("instructorId") Long instructorId, Pageable pageable);

    @Query("select case when count(c) > 0 then true else false end from Clase c " +
           "where c.lugar.id = :lugarId and c.diaSemana = :dia and c.activo = true " +
           "and c.horaInicio < :horaFin and c.horaFin > :horaInicio " +
           "and (cast(:excludeId as long) is null or c.id <> :excludeId)")
    boolean existeConflictoLugar(@Param("lugarId") Long lugarId, @Param("dia") DiaSemana dia,
                                 @Param("horaInicio") LocalTime horaInicio, @Param("horaFin") LocalTime horaFin,
                                 @Param("excludeId") Long excludeId);

    @Query("select case when count(c) > 0 then true else false end from Clase c " +
           "where c.instructor.id = :instructorId and c.diaSemana = :dia and c.activo = true " +
           "and c.horaInicio < :horaFin and c.horaFin > :horaInicio " +
           "and (cast(:excludeId as long) is null or c.id <> :excludeId)")
    boolean existeConflictoInstructor(@Param("instructorId") Long instructorId, @Param("dia") DiaSemana dia,
                                      @Param("horaInicio") LocalTime horaInicio, @Param("horaFin") LocalTime horaFin,
                                      @Param("excludeId") Long excludeId);
}
