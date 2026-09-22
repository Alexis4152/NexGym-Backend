package com.nexora.sport.repository;

import com.nexora.sport.model.Asistencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {
    Page<Asistencia> findByCentroId(Long centroId, Pageable pageable);
    long countByCentroIdAndFecha(Long centroId, LocalDate fecha);
    Page<Asistencia> findByAlumnoId(Long alumnoId, Pageable pageable);

    long countByCentroIdAndFechaBetween(Long centroId, LocalDate desde, LocalDate hasta);

    @Query("select count(distinct a.alumno.id) from Asistencia a where a.centro.id = :centroId and a.fecha between :desde and :hasta")
    long countAlumnosUnicos(@Param("centroId") Long centroId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query(nativeQuery = true, value =
            "select a.fecha, 0 as total, count(*) as cantidad from asistencias a " +
            "where a.centro_id = :centroId and a.fecha between :desde and :hasta group by a.fecha order by a.fecha")
    List<Object[]> porDia(@Param("centroId") Long centroId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("select a.disciplina.id, a.disciplina.nombre, count(a), 0 from Asistencia a " +
           "where a.centro.id = :centroId and a.fecha between :desde and :hasta and a.disciplina is not null " +
           "group by a.disciplina.id, a.disciplina.nombre order by count(a) desc")
    List<Object[]> porDisciplina(@Param("centroId") Long centroId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** Solo cubre asistencias ligadas a una Clase (unica via donde hoy existe sucursal); ver limitacion documentada en el reporte. */
    @Query("select c.sucursal.id, c.sucursal.nombre, count(a), 0 from Asistencia a join a.clase c " +
           "where a.centro.id = :centroId and a.fecha between :desde and :hasta group by c.sucursal.id, c.sucursal.nombre order by count(a) desc")
    List<Object[]> porSucursal(@Param("centroId") Long centroId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    long countByClaseIdAndFechaBetween(Long claseId, LocalDate desde, LocalDate hasta);

    long countByCentroIdAndDisciplinaIdAndFechaBetween(Long centroId, Long disciplinaId, LocalDate desde, LocalDate hasta);

    Optional<Asistencia> findFirstByAlumnoIdOrderByFechaDescHoraDesc(Long alumnoId);

    @Query("select count(a) from Asistencia a join a.clase c where c.instructor.id = :instructorId and a.fecha between :desde and :hasta")
    long countByInstructorAndRango(@Param("instructorId") Long instructorId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
