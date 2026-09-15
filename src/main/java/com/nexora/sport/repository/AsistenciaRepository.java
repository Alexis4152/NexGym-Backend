package com.nexora.sport.repository;

import com.nexora.sport.model.Asistencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {
    Page<Asistencia> findByCentroId(Long centroId, Pageable pageable);
    long countByCentroIdAndFecha(Long centroId, LocalDate fecha);
    Page<Asistencia> findByAlumnoId(Long alumnoId, Pageable pageable);
}
