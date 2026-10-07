package com.nexora.sport.repository;

import com.nexora.sport.model.ApartadoPlan;
import com.nexora.sport.model.EstadoApartadoPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ApartadoPlanRepository extends JpaRepository<ApartadoPlan, Long> {

    /** Mismo patron de filtros opcionales que ApartadoRepository#buscar (ver ahi el porque
     * del cast(:x as tipo) en cada "is null"). */
    @Query("select a from ApartadoPlan a where a.centro.id = :centroId " +
           "and (cast(:estado as string) is null or a.estado = :estado) " +
           "and (cast(:desde as timestamp) is null or a.solicitadoEn >= :desde) " +
           "and (cast(:hasta as timestamp) is null or a.solicitadoEn < :hasta) " +
           "and (cast(:q as string) is null or lower(a.clienteNombre) like lower(concat('%', cast(:q as string), '%')) " +
           "     or a.clienteTelefono like concat('%', cast(:q as string), '%') " +
           "     or lower(a.planNombreSnapshot) like lower(concat('%', cast(:q as string), '%'))) " +
           "order by a.solicitadoEn desc")
    Page<ApartadoPlan> buscar(@Param("centroId") Long centroId, @Param("estado") EstadoApartadoPlan estado,
                               @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                               @Param("q") String q, Pageable pageable);
}
