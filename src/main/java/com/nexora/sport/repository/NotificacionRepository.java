package com.nexora.sport.repository;

import com.nexora.sport.model.Notificacion;
import com.nexora.sport.model.TipoDestinatario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    boolean existsByDedupeKey(String dedupeKey);

    /** Bandeja de la campana: solo lo que el centro marco como visible internamente. */
    Page<Notificacion> findByCentroIdAndDestinatarioTipoAndInternoVisibleTrueOrderByCreatedAtDesc(
            Long centroId, TipoDestinatario destinatarioTipo, Pageable pageable);

    Page<Notificacion> findByCentroIdAndDestinatarioTipoAndInternoVisibleTrueAndLeidaFalseOrderByCreatedAtDesc(
            Long centroId, TipoDestinatario destinatarioTipo, Pageable pageable);

    long countByCentroIdAndDestinatarioTipoAndInternoVisibleTrueAndLeidaFalse(Long centroId, TipoDestinatario destinatarioTipo);

    List<Notificacion> findByCentroIdAndAlumnoIdOrderByCreatedAtDesc(Long centroId, Long alumnoId);

    @Modifying
    @Query("update Notificacion n set n.leida = true, n.leidaEn = :ahora " +
           "where n.centro.id = :centroId and n.destinatarioTipo = :destinatarioTipo and n.leida = false")
    int marcarTodasLeidas(@Param("centroId") Long centroId, @Param("destinatarioTipo") TipoDestinatario destinatarioTipo,
                           @Param("ahora") LocalDateTime ahora);
}
