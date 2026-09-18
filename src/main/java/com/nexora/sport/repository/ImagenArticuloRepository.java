package com.nexora.sport.repository;

import com.nexora.sport.model.ImagenArticulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImagenArticuloRepository extends JpaRepository<ImagenArticulo, Long> {
    List<ImagenArticulo> findByArticuloIdOrderByOrdenAsc(Long articuloId);
    Optional<ImagenArticulo> findFirstByArticuloIdAndEsPrincipalTrue(Long articuloId);
    Optional<ImagenArticulo> findFirstByArticuloIdOrderByOrdenAsc(Long articuloId);
    long countByArticuloId(Long articuloId);
}
