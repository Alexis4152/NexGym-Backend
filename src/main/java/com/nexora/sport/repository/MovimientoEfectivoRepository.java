package com.nexora.sport.repository;

import com.nexora.sport.model.MovimientoEfectivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovimientoEfectivoRepository extends JpaRepository<MovimientoEfectivo, Long> {

    List<MovimientoEfectivo> findByCorteCajaIdOrderByCreatedAtAsc(Long corteCajaId);

    /** Movimientos con la caja cerrada que vio un corte al recibirla. */
    List<MovimientoEfectivo> findByRecibidoPorCorteIdOrderByCreatedAtAsc(Long corteCajaId);

    /** Movimientos con la caja cerrada que todavia no recibe ningun turno (desglose de la entrega pendiente). */
    List<MovimientoEfectivo> findByCajaRegistradoraIdAndCorteCajaIsNullAndRecibidoPorCorteIsNullOrderByCreatedAtAsc(Long cajaId);

    /** [tipo, suma] de los movimientos de un corte (agregado: no carga las filas). */
    @Query("select m.tipo, coalesce(sum(m.monto), 0) from MovimientoEfectivo m where m.corteCaja.id = :corteId group by m.tipo")
    List<Object[]> sumarPorTipo(@Param("corteId") Long corteId);
}
