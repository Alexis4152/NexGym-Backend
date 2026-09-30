package com.nexora.sport.repository;

import com.nexora.sport.model.Alumno;
import com.nexora.sport.model.EstadoAlumno;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AlumnoRepository extends JpaRepository<Alumno, Long> {
    Page<Alumno> findByCentroIdAndDeletedAtIsNull(Long centroId, Pageable pageable);

    Page<Alumno> findByCentroIdAndDeletedAtIsNullAndNombreContainingIgnoreCase(
            Long centroId, String nombre, Pageable pageable);

    java.util.Optional<Alumno> findByCodigoQrAndDeletedAtIsNull(String codigoQr);

    long countByCentroIdAndEstadoAndDeletedAtIsNull(Long centroId, EstadoAlumno estado);

    long countByCentroIdAndDeletedAtIsNull(Long centroId);

    long countByCentroIdAndFechaIngresoBetweenAndDeletedAtIsNull(Long centroId, LocalDate desde, LocalDate hasta);

    long countByCentroIdAndFechaBajaBetween(Long centroId, LocalDateTime desde, LocalDateTime hasta);

    @Query(nativeQuery = true, value =
            // CAST(...) en vez de "::date": Spring Data JPA confunde "::" con un named parameter ":xxx" en native queries.
            "select CAST(date_trunc('month', CAST(a.fecha_ingreso as timestamp)) as date) as mes, 0 as total, count(*) as cantidad " +
            "from alumnos a where a.centro_id = :centroId and a.deleted_at is null " +
            "and a.fecha_ingreso >= :desde and a.fecha_ingreso <= :hasta group by 1 order by 1")
    List<Object[]> nuevosPorMes(@Param("centroId") Long centroId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
