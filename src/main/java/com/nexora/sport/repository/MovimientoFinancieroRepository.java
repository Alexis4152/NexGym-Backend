package com.nexora.sport.repository;

import com.nexora.sport.model.MovimientoFinanciero;
import com.nexora.sport.model.TipoMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface MovimientoFinancieroRepository extends JpaRepository<MovimientoFinanciero, Long> {

    Page<MovimientoFinanciero> findByCentroIdAndAnuladoFalseOrderByFechaDesc(Long centroId, Pageable pageable);

    Page<MovimientoFinanciero> findByCentroIdAndFechaBetweenAndAnuladoFalseOrderByFechaDesc(
            Long centroId, LocalDate desde, LocalDate hasta, Pageable pageable);

    @Query("select coalesce(sum(m.monto), 0) from MovimientoFinanciero m " +
           "where m.centro.id = :centroId and m.tipo = :tipo and m.fecha between :desde and :hasta and m.anulado = false")
    BigDecimal sumMontoByTipoAndRango(@Param("centroId") Long centroId,
                                       @Param("tipo") TipoMovimiento tipo,
                                       @Param("desde") LocalDate desde,
                                       @Param("hasta") LocalDate hasta);
}
