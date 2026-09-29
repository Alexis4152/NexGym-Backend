package com.nexora.sport.repository;

import com.nexora.sport.model.CorteCaja;
import com.nexora.sport.model.EstadoCorteCaja;
import com.nexora.sport.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CorteCajaRepository extends JpaRepository<CorteCaja, Long> {
    Optional<CorteCaja> findFirstByUsuarioIdAndEstado(Long usuarioId, EstadoCorteCaja estado);
    boolean existsByUsuarioIdAndAbiertoEnBetween(Long usuarioId, LocalDateTime desde, LocalDateTime hasta);
    Page<CorteCaja> findByCentroId(Long centroId, Pageable pageable);
    List<CorteCaja> findByCentroIdAndEstado(Long centroId, EstadoCorteCaja estado);

    /** Filtros homologados con DemoPV#CashCutController (desde/hasta/estado/cajero, todos opcionales). */
    @Query("select c from CorteCaja c where c.centro.id = :centroId " +
           // cast(:x as tipo) en el "is null": ver nota en VentaRepository/ApartadoRepository.
           "and (cast(:desde as timestamp) is null or c.abiertoEn >= :desde) " +
           "and (cast(:hasta as timestamp) is null or c.abiertoEn < :hasta) " +
           "and (cast(:estado as string) is null or c.estado = :estado) " +
           "and (cast(:usuarioId as long) is null or c.usuario.id = :usuarioId) " +
           "order by c.abiertoEn desc")
    Page<CorteCaja> buscar(@Param("centroId") Long centroId, @Param("desde") LocalDateTime desde,
                           @Param("hasta") LocalDateTime hasta, @Param("estado") EstadoCorteCaja estado,
                           @Param("usuarioId") Long usuarioId, Pageable pageable);

    /** Cajeros distintos que han abierto un corte en este centro, para el filtro (no requiere seccion USUARIOS). */
    @Query("select distinct c.usuario from CorteCaja c where c.centro.id = :centroId order by c.usuario.nombre")
    List<Usuario> cajerosDelCentro(@Param("centroId") Long centroId);
}
