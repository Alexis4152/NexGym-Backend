package com.nexora.sport.repository;

import com.nexora.sport.model.Alumno;
import com.nexora.sport.model.EstadoAlumno;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlumnoRepository extends JpaRepository<Alumno, Long> {
    Page<Alumno> findByCentroIdAndDeletedAtIsNull(Long centroId, Pageable pageable);

    Page<Alumno> findByCentroIdAndDeletedAtIsNullAndNombreContainingIgnoreCase(
            Long centroId, String nombre, Pageable pageable);

    long countByCentroIdAndEstadoAndDeletedAtIsNull(Long centroId, EstadoAlumno estado);

    long countByCentroIdAndDeletedAtIsNull(Long centroId);
}
