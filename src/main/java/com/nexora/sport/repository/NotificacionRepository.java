package com.nexora.sport.repository;

import com.nexora.sport.model.Notificacion;
import com.nexora.sport.model.Seccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    boolean existsByDedupeKey(String dedupeKey);

    /** Bandeja de la campana: CENTRO_ADMIN filtrado por Seccion/sucursal/nivel del actor
     * (ver Notificacion#seccionObjetivo/#sucursal), unido con lo INSTRUCTOR que sea suyo.
     * `sucursalIds` null = sin restriccion (Dueno/SUPER_ADMIN, ve todas las sucursales);
     * `miInstructorId` null = el actor no tiene ficha de Instructor ligada (nunca calza la
     * rama INSTRUCTOR); `esPersonalOperativo` = false bloquea TODO el bloque CENTRO_ADMIN
     * (un Entrenador con seccion CLASES/ALUMNOS por su propio rol NO debe ver los avisos
     * generales de Recepcion/Encargado/Dueno solo por compartir esas secciones -- ver
     * NotificacionService#esPersonalOperativo). Mismo patron de casts/Sets-null-seguros ya
     * usado en el resto del repo (ver ClaseRepository/InstructorRepository) para evitar el
     * bug de Postgres al no poder inferir el tipo de un parametro nulo suelto. */
    @Query("select n from Notificacion n where n.centro.id = :centroId and n.internoVisible = true and (" +
           "  (n.destinatarioTipo = com.nexora.sport.model.TipoDestinatario.CENTRO_ADMIN " +
           "    and :esPersonalOperativo = true " +
           "    and (n.seccionObjetivo is null or n.seccionObjetivo in :secciones) " +
           "    and (n.sucursal is null or :sucursalIds is null or n.sucursal.id in :sucursalIds) " +
           "    and (n.tipo <> com.nexora.sport.model.TipoNotificacion.EGRESO_PENDIENTE_APROBACION or :puedeAprobar = true)) " +
           "  or (n.destinatarioTipo = com.nexora.sport.model.TipoDestinatario.INSTRUCTOR " +
           "    and cast(:miInstructorId as long) is not null and n.instructor.id = :miInstructorId)" +
           ") and (:soloNoLeidas = false or n.leida = false) " +
           "order by n.createdAt desc")
    Page<Notificacion> buscarBandeja(@Param("centroId") Long centroId, @Param("secciones") Set<Seccion> secciones,
                                      @Param("sucursalIds") Set<Long> sucursalIds, @Param("puedeAprobar") boolean puedeAprobar,
                                      @Param("esPersonalOperativo") boolean esPersonalOperativo,
                                      @Param("miInstructorId") Long miInstructorId, @Param("soloNoLeidas") boolean soloNoLeidas,
                                      Pageable pageable);

    @Query("select count(n) from Notificacion n where n.centro.id = :centroId and n.internoVisible = true and n.leida = false and (" +
           "  (n.destinatarioTipo = com.nexora.sport.model.TipoDestinatario.CENTRO_ADMIN " +
           "    and :esPersonalOperativo = true " +
           "    and (n.seccionObjetivo is null or n.seccionObjetivo in :secciones) " +
           "    and (n.sucursal is null or :sucursalIds is null or n.sucursal.id in :sucursalIds) " +
           "    and (n.tipo <> com.nexora.sport.model.TipoNotificacion.EGRESO_PENDIENTE_APROBACION or :puedeAprobar = true)) " +
           "  or (n.destinatarioTipo = com.nexora.sport.model.TipoDestinatario.INSTRUCTOR " +
           "    and cast(:miInstructorId as long) is not null and n.instructor.id = :miInstructorId)" +
           ")")
    long contarNoLeidasBandeja(@Param("centroId") Long centroId, @Param("secciones") Set<Seccion> secciones,
                                @Param("sucursalIds") Set<Long> sucursalIds, @Param("puedeAprobar") boolean puedeAprobar,
                                @Param("esPersonalOperativo") boolean esPersonalOperativo,
                                @Param("miInstructorId") Long miInstructorId);

    @Modifying
    @Query("update Notificacion n set n.leida = true, n.leidaEn = :ahora where n.leida = false " +
           "and n.centro.id = :centroId and n.internoVisible = true and (" +
           "  (n.destinatarioTipo = com.nexora.sport.model.TipoDestinatario.CENTRO_ADMIN " +
           "    and :esPersonalOperativo = true " +
           "    and (n.seccionObjetivo is null or n.seccionObjetivo in :secciones) " +
           "    and (n.sucursal is null or :sucursalIds is null or n.sucursal.id in :sucursalIds) " +
           "    and (n.tipo <> com.nexora.sport.model.TipoNotificacion.EGRESO_PENDIENTE_APROBACION or :puedeAprobar = true)) " +
           "  or (n.destinatarioTipo = com.nexora.sport.model.TipoDestinatario.INSTRUCTOR " +
           "    and cast(:miInstructorId as long) is not null and n.instructor.id = :miInstructorId)" +
           ")")
    int marcarTodasLeidas(@Param("centroId") Long centroId, @Param("secciones") Set<Seccion> secciones,
                           @Param("sucursalIds") Set<Long> sucursalIds, @Param("puedeAprobar") boolean puedeAprobar,
                           @Param("esPersonalOperativo") boolean esPersonalOperativo,
                           @Param("miInstructorId") Long miInstructorId, @Param("ahora") LocalDateTime ahora);

    List<Notificacion> findByCentroIdAndAlumnoIdOrderByCreatedAtDesc(Long centroId, Long alumnoId);
}
