package com.nexora.sport.repository;

import com.nexora.sport.model.EstadoPago;
import com.nexora.sport.model.PagoMembresia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PagoMembresiaRepository extends JpaRepository<PagoMembresia, Long> {

    List<PagoMembresia> findByMembresiaIdOrderByFechaDescCreatedAtDesc(Long membresiaId);

    @Query("select coalesce(sum(p.monto), 0) from PagoMembresia p " +
           "where p.membresia.id = :membresiaId and p.estado = com.nexora.sport.model.EstadoPago.VALIDO")
    BigDecimal sumValidoByMembresiaId(@Param("membresiaId") Long membresiaId);

    long countByMembresiaIdAndEstado(Long membresiaId, EstadoPago estado);

    /** Total pagado por membresia, en un solo query (evita N+1 al listar cartera de muchas membresias). */
    @Query("select p.membresia.id, coalesce(sum(p.monto), 0) from PagoMembresia p " +
           "where p.membresia.id in :membresiaIds and p.estado = com.nexora.sport.model.EstadoPago.VALIDO " +
           "group by p.membresia.id")
    List<Object[]> sumValidoAgrupadoPorMembresia(@Param("membresiaIds") List<Long> membresiaIds);

    @Query("select coalesce(sum(p.monto), 0) from PagoMembresia p " +
           "where p.membresia.centro.id = :centroId and p.estado = com.nexora.sport.model.EstadoPago.VALIDO " +
           "and p.fecha between :desde and :hasta")
    BigDecimal sumValidoByCentroAndRango(@Param("centroId") Long centroId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("select p.registradoPor.id, p.registradoPor.nombre, count(p), coalesce(sum(p.monto), 0) from PagoMembresia p " +
           "where p.membresia.centro.id = :centroId and p.estado = com.nexora.sport.model.EstadoPago.VALIDO " +
           "and p.fecha between :desde and :hasta and p.registradoPor is not null " +
           "group by p.registradoPor.id, p.registradoPor.nombre")
    List<Object[]> operacionesPorUsuario(@Param("centroId") Long centroId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("select cast(p.metodoPago as string), count(p), coalesce(sum(p.monto), 0) from PagoMembresia p " +
           "where p.membresia.centro.id = :centroId and p.estado = com.nexora.sport.model.EstadoPago.VALIDO " +
           "and p.fecha between :desde and :hasta group by p.metodoPago order by sum(p.monto) desc")
    List<Object[]> sumPorMetodoPago(@Param("centroId") Long centroId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
