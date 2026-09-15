package com.nexora.sport.repository;

import com.nexora.sport.model.MembresiaPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembresiaPlanRepository extends JpaRepository<MembresiaPlan, Long> {
    Page<MembresiaPlan> findByCentroId(Long centroId, Pageable pageable);
    List<MembresiaPlan> findByCentroIdAndActivoTrue(Long centroId);
}
