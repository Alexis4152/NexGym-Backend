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

    List<Membresia> findByCentroIdAndEstado(Long centroId, EstadoMembresia estado);

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

    /** Cartera: toda membresia con posibilidad de tener saldo (CANCELADA se excluye, no se cobra mas). */
    @Query("select m from Membresia m where m.centro.id = :centroId and m.estado <> com.nexora.sport.model.EstadoMembresia.CANCELADA")
    List<Membresia> findParaCartera(@Param("centroId") Long centroId);

    /** "Ventas" de membresia = contrataciones NUEVAS (no renovaciones) creadas en el periodo. */
    @Query("select m from Membresia m where m.centro.id = :centroId and m.membresiaAnterior is null " +
           "and m.createdAt >= :desde and m.createdAt < :hasta")
    List<Membresia> findNuevasEnPeriodo(@Param("centroId") Long centroId, @Param("desde") java.time.LocalDateTime desde, @Param("hasta") java.time.LocalDateTime hasta);

    @Query("select m from Membresia m where m.centro.id = :centroId and m.membresiaAnterior is not null " +
           "and m.createdAt >= :desde and m.createdAt < :hasta")
    List<Membresia> findRenovacionesEnPeriodo(@Param("centroId") Long centroId, @Param("desde") java.time.LocalDateTime desde, @Param("hasta") java.time.LocalDateTime hasta);

    /** Elegibles para retencion: vencieron dentro del rango y no fueron canceladas antes de vencer. */
    @Query("select m from Membresia m where m.centro.id = :centroId and m.fechaFin between :desde and :hasta " +
           "and m.estado <> com.nexora.sport.model.EstadoMembresia.CANCELADA")
    List<Membresia> findElegiblesParaRetencion(@Param("centroId") Long centroId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** Todas las membresias (de cualquier estado no cancelado) de un alumno, para el calculo de renovacion en retencion. */
    @Query("select m from Membresia m where m.alumno.id = :alumnoId and m.estado <> com.nexora.sport.model.EstadoMembresia.CANCELADA")
    List<Membresia> findNoCanceladasPorAlumno(@Param("alumnoId") Long alumnoId);

    /** Base para "riesgo de abandono": membresias con vigencia real (fecha_fin no nula) actualmente activas. */
    @Query("select m from Membresia m where m.centro.id = :centroId and m.estado = com.nexora.sport.model.EstadoMembresia.ACTIVA and m.fechaFin is not null")
    List<Membresia> findActivasConFechaFin(@Param("centroId") Long centroId);

    /** Para el aviso "membresia vencida" (una sola vez, el dia despues de vencer): ver NotificacionSchedulerService. */
    @Query("select m from Membresia m where m.centro.id = :centroId and m.estado = com.nexora.sport.model.EstadoMembresia.VENCIDA and m.fechaFin = :fecha")
    List<Membresia> findVencidasEnFecha(@Param("centroId") Long centroId, @Param("fecha") LocalDate fecha);

    /** Para el resumen administrativo "N membresias vencen esta semana". */
    @Query("select count(m) from Membresia m where m.centro.id = :centroId and m.estado = com.nexora.sport.model.EstadoMembresia.ACTIVA " +
           "and m.fechaFin between :desde and :hasta")
    long countActivasFechaFinEntre(@Param("centroId") Long centroId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** Para "Disciplinas": membresias ACTIVA que incluyen una disciplina dada (via membresia_disciplinas), o accesoCompleto. */
    @Query("select m from Membresia m where m.centro.id = :centroId and m.estado = com.nexora.sport.model.EstadoMembresia.ACTIVA " +
           "and (m.plan.accesoCompleto = true or :disciplinaId in (select d.id from m.disciplinas d))")
    List<Membresia> findActivasPorDisciplina(@Param("centroId") Long centroId, @Param("disciplinaId") Long disciplinaId);

    /** Cupo ocupado de un plan: ACTIVA/PENDIENTE/SUSPENDIDA cuentan (la misma nocion de
     * "vigente" que findVigentesPorAlumnoOrdenadas); VENCIDA/CANCELADA liberan el cupo. */
    @Query("select count(m) from Membresia m where m.plan.id = :planId and m.estado in " +
           "(com.nexora.sport.model.EstadoMembresia.ACTIVA, com.nexora.sport.model.EstadoMembresia.PENDIENTE, com.nexora.sport.model.EstadoMembresia.SUSPENDIDA)")
    long countVigentesPorPlan(@Param("planId") Long planId);
}
