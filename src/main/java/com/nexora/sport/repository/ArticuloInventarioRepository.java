package com.nexora.sport.repository;

import com.nexora.sport.model.ArticuloInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticuloInventarioRepository extends JpaRepository<ArticuloInventario, Long> {
    Page<ArticuloInventario> findByCentroIdAndDeletedAtIsNull(Long centroId, Pageable pageable);

    Page<ArticuloInventario> findByCentroIdAndDeletedAtIsNullAndNombreContainingIgnoreCase(
            Long centroId, String nombre, Pageable pageable);

    Optional<ArticuloInventario> findByCentroIdAndCodigoBarras(Long centroId, String codigoBarras);

    List<ArticuloInventario> findByCentroIdAndVendibleTrueAndActivoTrueAndDeletedAtIsNull(Long centroId);

    List<ArticuloInventario> findByCentroIdAndReservableTrueAndActivoTrueAndDeletedAtIsNull(Long centroId);

    @Query("select a from ArticuloInventario a where a.centro.id = :centroId and a.deletedAt is null " +
           "and a.stock <= a.stockMinimo")
    List<ArticuloInventario> findConStockBajo(@Param("centroId") Long centroId);
}
