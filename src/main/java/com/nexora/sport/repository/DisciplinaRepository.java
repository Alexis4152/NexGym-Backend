package com.nexora.sport.repository;

import com.nexora.sport.model.Disciplina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisciplinaRepository extends JpaRepository<Disciplina, Long> {
    Page<Disciplina> findByCentroId(Long centroId, Pageable pageable);
    List<Disciplina> findByCentroIdAndActivoTrue(Long centroId);
    boolean existsByCentroIdAndNombreIgnoreCase(Long centroId, String nombre);
}
