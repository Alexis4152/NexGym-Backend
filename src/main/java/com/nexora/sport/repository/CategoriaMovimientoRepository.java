package com.nexora.sport.repository;

import com.nexora.sport.model.CategoriaMovimiento;
import com.nexora.sport.model.TipoMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaMovimientoRepository extends JpaRepository<CategoriaMovimiento, Long> {
    List<CategoriaMovimiento> findByCentroIdAndActivoTrue(Long centroId);
    List<CategoriaMovimiento> findByCentroIdAndTipoAndActivoTrue(Long centroId, TipoMovimiento tipo);
    Optional<CategoriaMovimiento> findByCentroIdAndNombreAndTipo(Long centroId, String nombre, TipoMovimiento tipo);
}
