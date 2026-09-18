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

    @Query("select coalesce(sum(i.cantidad), 0) from ApartadoItem i " +
           "where i.articulo.id = :articuloId and i.apartado.estado = 'PENDIENTE' and i.apartado.id <> :excludeApartadoId")
    int sumCantidadPendientePorArticulo(@Param("articuloId") Long articuloId, @Param("excludeApartadoId") Long excludeApartadoId);
}
