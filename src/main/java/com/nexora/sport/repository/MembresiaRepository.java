package com.nexora.sport.repository;

import com.nexora.sport.model.EstadoMembresia;
import com.nexora.sport.model.Membresia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MembresiaRepository extends JpaRepository<Membresia, Long> {

    Page<Membresia> findByCentroId(Long centroId, Pageable pageable);

    Page<Membresia> findByAlumnoId(Long alumnoId, Pageable pageable);

    List<Membresia> findByAlumnoIdAndEstado(Long alumnoId, EstadoMembresia estado);

    long countByCentroIdAndEstado(Long centroId, EstadoMembresia estado);

    @Query("select m from Membresia m where m.centro.id = :centroId and m.estado = 'ACTIVA' " +
           "and m.fechaFin between :hoy and :limite order by m.fechaFin asc")
    List<Membresia> findProximasAVencer(@Param("centroId") Long centroId,
                                         @Param("hoy") LocalDate hoy,
                                         @Param("limite") LocalDate limite);

    @Query("select m from Membresia m where m.centro.id = :centroId and m.estado = 'ACTIVA' " +
           "and m.fechaFin < :hoy order by m.fechaFin asc")
    List<Membresia> findVencidasNoActualizadas(@Param("centroId") Long centroId, @Param("hoy") LocalDate hoy);

    @Query("select m from Membresia m where m.estado = 'ACTIVA' and m.fechaFin < :hoy")
    List<Membresia> findAllVencidasNoActualizadas(@Param("hoy") LocalDate hoy);

    @Query("select m from Membresia m where m.centro.id = :centroId and m.estado = 'PENDIENTE' and m.fechaInicio <= :hoy")
    List<Membresia> findPendientesParaActivar(@Param("centroId") Long centroId, @Param("hoy") LocalDate hoy);

    @Query("select m from Membresia m where m.estado = 'PENDIENTE' and m.fechaInicio <= :hoy")
    List<Membresia> findAllPendientesParaActivar(@Param("hoy") LocalDate hoy);

    /** La vigencia "actual" del alumno para el expediente: la de fecha_fin mas lejana entre ACTIVA/PENDIENTE/SUSPENDIDA. */
    @Query("select m from Membresia m where m.alumno.id = :alumnoId " +
           "and m.estado in (com.nexora.sport.model.EstadoMembresia.ACTIVA, com.nexora.sport.model.EstadoMembresia.PENDIENTE, com.nexora.sport.model.EstadoMembresia.SUSPENDIDA) " +
           "order by m.fechaFin desc")
    List<Membresia> findVigentesPorAlumnoOrdenadas(@Param("alumnoId") Long alumnoId);

    /** true si alguna otra membresia la referencia como membresiaAnterior (para el indicador "renovada"). */
    boolean existsByMembresiaAnteriorId(Long membresiaAnteriorId);
}
