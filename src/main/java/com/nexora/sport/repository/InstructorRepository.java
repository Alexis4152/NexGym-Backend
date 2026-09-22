package com.nexora.sport.repository;

import com.nexora.sport.model.Instructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InstructorRepository extends JpaRepository<Instructor, Long> {
    Page<Instructor> findByCentroId(Long centroId, Pageable pageable);
    Page<Instructor> findByCentroIdAndNombreContainingIgnoreCase(Long centroId, String nombre, Pageable pageable);

    @Query("select i from Instructor i left join fetch i.disciplinas where i.id = :id")
    Optional<Instructor> findWithDisciplinasById(@Param("id") Long id);
}
