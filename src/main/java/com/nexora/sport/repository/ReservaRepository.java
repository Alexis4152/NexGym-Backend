package com.nexora.sport.repository;

import com.nexora.sport.model.EstadoReserva;
import com.nexora.sport.model.Reserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    Page<Reserva> findByCentroId(Long centroId, Pageable pageable);

    List<Reserva> findByClaseIdAndFechaAndEstadoNot(Long claseId, LocalDate fecha, EstadoReserva estadoExcluido);

    long countByClaseIdAndFechaAndEstadoNot(Long claseId, LocalDate fecha, EstadoReserva estadoExcluido);

    boolean existsByClaseIdAndAlumnoIdAndFecha(Long claseId, Long alumnoId, LocalDate fecha);

    Page<Reserva> findByAlumnoId(Long alumnoId, Pageable pageable);
}
