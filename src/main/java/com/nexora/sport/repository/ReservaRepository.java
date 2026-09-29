package com.nexora.sport.repository;

import com.nexora.sport.model.EstadoReserva;
import com.nexora.sport.model.Reserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    Page<Reserva> findByCentroId(Long centroId, Pageable pageable);

    List<Reserva> findByClaseIdAndFechaAndEstadoNot(Long claseId, LocalDate fecha, EstadoReserva estadoExcluido);

    long countByClaseIdAndFechaAndEstadoNot(Long claseId, LocalDate fecha, EstadoReserva estadoExcluido);

    boolean existsByClaseIdAndAlumnoIdAndFecha(Long claseId, Long alumnoId, LocalDate fecha);

    Page<Reserva> findByAlumnoId(Long alumnoId, Pageable pageable);

    @Query("select case when count(r) > 0 then true else false end from Reserva r " +
           "where r.alumno.id = :alumnoId and r.fecha = :fecha and r.estado <> :estadoExcluido " +
           "and r.clase.horaInicio < :horaFin and r.clase.horaFin > :horaInicio")
    boolean existeConflictoHorarioAlumno(@Param("alumnoId") Long alumnoId, @Param("fecha") LocalDate fecha,
                                        @Param("horaInicio") LocalTime horaInicio, @Param("horaFin") LocalTime horaFin,
                                        @Param("estadoExcluido") EstadoReserva estadoExcluido);

    long countByClaseIdAndFechaBetweenAndEstadoNot(Long claseId, LocalDate desde, LocalDate hasta, EstadoReserva estadoExcluido);

    /** Reservas activas de una clase desde hoy en adelante (para cascada de cancelacion/cambio, ver ClaseService). */
    List<Reserva> findByClaseIdAndEstadoAndFechaGreaterThanEqual(Long claseId, EstadoReserva estado, LocalDate desde);

    /** Candidatas a recordatorio de clase (ver NotificacionSchedulerService): reservas activas en una ventana corta de fechas. */
    List<Reserva> findByCentroIdAndEstadoAndFechaBetween(Long centroId, EstadoReserva estado, LocalDate desde, LocalDate hasta);
}
