package com.nexora.sport.repository;

import com.nexora.sport.model.Apartado;
import com.nexora.sport.model.EstadoApartado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ApartadoRepository extends JpaRepository<Apartado, Long> {
    Page<Apartado> findByCentroIdOrderBySolicitadoEnDesc(Long centroId, Pageable pageable);

    Page<Apartado> findByCentroIdAndEstadoOrderBySolicitadoEnDesc(Long centroId, EstadoApartado estado, Pageable pageable);

    List<Apartado> findByEstadoAndExpiraEnBefore(EstadoApartado estado, LocalDateTime momento);

    /** Filtros homologados con DemoPV#Apartados (Estado/Desde/Hasta/"cliente o producto", todos opcionales). */
    @Query("select distinct a from Apartado a left join a.items i where a.centro.id = :centroId " +
           // cast(:x as tipo) en el "is null": la ocurrencia suelta del parametro (sin
           // comparar contra ninguna columna) no le da a Postgres forma de inferir su
           // tipo y truena "no se pudo determinar el tipo del parametro $n" para
           // cualquier tipo no-string (enum, fecha) -- ver reporte final.
           "and (cast(:estado as string) is null or a.estado = :estado) " +
           "and (cast(:desde as timestamp) is null or a.solicitadoEn >= :desde) " +
           "and (cast(:hasta as timestamp) is null or a.solicitadoEn < :hasta) " +
           "and (cast(:q as string) is null or lower(a.clienteNombre) like lower(concat('%', cast(:q as string), '%')) " +
           "     or a.clienteTelefono like concat('%', cast(:q as string), '%') " +
           "     or lower(i.articuloNombre) like lower(concat('%', cast(:q as string), '%'))) " +
           "order by a.solicitadoEn desc")
    Page<Apartado> buscar(@Param("centroId") Long centroId, @Param("estado") EstadoApartado estado,
                           @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                           @Param("q") String q, Pageable pageable);

    @Query("select coalesce(sum(i.cantidad), 0) from ApartadoItem i " +
           "where i.articulo.id = :articuloId and i.apartado.estado = 'PENDIENTE' and i.apartado.id <> :excludeApartadoId")
    int sumCantidadPendientePorArticulo(@Param("articuloId") Long articuloId, @Param("excludeApartadoId") Long excludeApartadoId);
}
