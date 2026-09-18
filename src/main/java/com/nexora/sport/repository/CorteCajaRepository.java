package com.nexora.sport.repository;

import com.nexora.sport.model.CorteCaja;
import com.nexora.sport.model.EstadoCorteCaja;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CorteCajaRepository extends JpaRepository<CorteCaja, Long> {
    Optional<CorteCaja> findFirstByUsuarioIdAndEstado(Long usuarioId, EstadoCorteCaja estado);
    boolean existsByUsuarioIdAndAbiertoEnBetween(Long usuarioId, LocalDateTime desde, LocalDateTime hasta);
    Page<CorteCaja> findByCentroId(Long centroId, Pageable pageable);
    List<CorteCaja> findByCentroIdAndEstado(Long centroId, EstadoCorteCaja estado);
}
