package com.nexora.sport.repository;

import com.nexora.sport.model.EstadoSolicitudArco;
import com.nexora.sport.model.SolicitudArco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Sin filtro de centro: las solicitudes ARCO son de la plataforma completa. */
public interface SolicitudArcoRepository extends JpaRepository<SolicitudArco, Long> {

    /** Todas (o las de un estado), con las abiertas primero y luego de la mas reciente a la mas antigua. */
    @Query("select s from SolicitudArco s where (:estado is null or s.estado = :estado) " +
           "order by case when s.estado in (com.nexora.sport.model.EstadoSolicitudArco.PENDIENTE, " +
           "com.nexora.sport.model.EstadoSolicitudArco.INFO_SOLICITADA, " +
           "com.nexora.sport.model.EstadoSolicitudArco.APROBADA) then 0 else 1 end, s.createdAt desc")
    List<SolicitudArco> buscar(@Param("estado") EstadoSolicitudArco estado);

    @Query("select count(s) from SolicitudArco s where s.estado in (" +
           "com.nexora.sport.model.EstadoSolicitudArco.PENDIENTE, " +
           "com.nexora.sport.model.EstadoSolicitudArco.INFO_SOLICITADA, " +
           "com.nexora.sport.model.EstadoSolicitudArco.APROBADA)")
    long contarAbiertas();

    List<SolicitudArco> findBySolicitanteUsuarioIdOrderByCreatedAtDesc(Long usuarioId);

    Optional<SolicitudArco> findByTokenHash(String tokenHash);

    List<SolicitudArco> findByEstadoAndInfoFechaLimiteBefore(EstadoSolicitudArco estado, LocalDate antes);

    @Modifying
    @Query("delete from SolicitudArco s where s.estado = :estado and s.createdAt < :antes")
    int borrarPorEstadoAntesDe(@Param("estado") EstadoSolicitudArco estado, @Param("antes") LocalDateTime antes);
}
