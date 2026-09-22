package com.nexora.sport.repository;

import com.nexora.sport.model.EstadoPago;
import com.nexora.sport.model.PagoMembresia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PagoMembresiaRepository extends JpaRepository<PagoMembresia, Long> {

    List<PagoMembresia> findByMembresiaIdOrderByFechaDescCreatedAtDesc(Long membresiaId);

    @Query("select coalesce(sum(p.monto), 0) from PagoMembresia p " +
           "where p.membresia.id = :membresiaId and p.estado = com.nexora.sport.model.EstadoPago.VALIDO")
    BigDecimal sumValidoByMembresiaId(@Param("membresiaId") Long membresiaId);

    long countByMembresiaIdAndEstado(Long membresiaId, EstadoPago estado);
}
