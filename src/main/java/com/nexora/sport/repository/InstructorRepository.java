package com.nexora.sport.repository;

import com.nexora.sport.model.Instructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstructorRepository extends JpaRepository<Instructor, Long> {
    Page<Instructor> findByCentroId(Long centroId, Pageable pageable);
    Page<Instructor> findByCentroIdAndNombreContainingIgnoreCase(Long centroId, String nombre, Pageable pageable);
}
