package com.nexora.sport.repository;

import com.nexora.sport.model.Centro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CentroRepository extends JpaRepository<Centro, Long> {
    Optional<Centro> findBySlugPublicoAndActivoTrue(String slugPublico);
    boolean existsBySlugPublico(String slugPublico);

    /** Para CorteCajaAutoCloseJob: solo los centros que configuraron una hora de cierre automatico. */
    List<Centro> findByActivoTrueAndHoraCierreAutomaticoCorteIsNotNull();
}
