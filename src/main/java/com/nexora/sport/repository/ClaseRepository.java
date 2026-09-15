package com.nexora.sport.repository;

import com.nexora.sport.model.Clase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaseRepository extends JpaRepository<Clase, Long> {
    Page<Clase> findByCentroId(Long centroId, Pageable pageable);
    List<Clase> findByCentroIdAndActivoTrue(Long centroId);
    List<Clase> findByInstructorId(Long instructorId);
}
