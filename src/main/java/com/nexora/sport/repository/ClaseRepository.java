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

public interface ClaseRepository extends JpaRepository<Clase, Long> {
    Page<Clase> findByCentroId(Long centroId, Pageable pageable);
    List<Clase> findByCentroIdAndActivoTrue(Long centroId);
    List<Clase> findByInstructorId(Long instructorId);

    @Query("select case when count(c) > 0 then true else false end from Clase c " +
           "where c.lugar.id = :lugarId and c.diaSemana = :dia and c.activo = true " +
           "and c.horaInicio < :horaFin and c.horaFin > :horaInicio " +
           "and (:excludeId is null or c.id <> :excludeId)")
    boolean existeConflictoLugar(@Param("lugarId") Long lugarId, @Param("dia") DiaSemana dia,
                                 @Param("horaInicio") LocalTime horaInicio, @Param("horaFin") LocalTime horaFin,
                                 @Param("excludeId") Long excludeId);

    @Query("select case when count(c) > 0 then true else false end from Clase c " +
           "where c.instructor.id = :instructorId and c.diaSemana = :dia and c.activo = true " +
           "and c.horaInicio < :horaFin and c.horaFin > :horaInicio " +
           "and (:excludeId is null or c.id <> :excludeId)")
    boolean existeConflictoInstructor(@Param("instructorId") Long instructorId, @Param("dia") DiaSemana dia,
                                      @Param("horaInicio") LocalTime horaInicio, @Param("horaFin") LocalTime horaFin,
                                      @Param("excludeId") Long excludeId);
}
