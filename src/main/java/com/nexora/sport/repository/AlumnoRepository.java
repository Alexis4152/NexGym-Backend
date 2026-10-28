package com.nexora.sport.repository;

import com.nexora.sport.model.Alumno;
import com.nexora.sport.model.EstadoAlumno;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AlumnoRepository extends JpaRepository<Alumno, Long> {
    Page<Alumno> findByCentroIdAndDeletedAtIsNull(Long centroId, Pageable pageable);

    Page<Alumno> findByCentroIdAndDeletedAtIsNullAndNombreContainingIgnoreCase(
            Long centroId, String nombre, Pageable pageable);

    /** Alumnos "registrados" a un grupo de clases (ver AlcanceInstructorService): inscritos a la
     * disciplina, con membresia vigente/pendiente que la cubra, o con reserva activa en alguna de
     * esas clases. `disciplinaIds`/`claseIds` NUNCA vacios (el servicio corta antes). */
    @Query("select a from Alumno a where a.centro.id = :centroId and a.deletedAt is null " +
           "and (cast(:q as string) is null or lower(a.nombre) like lower(concat('%', cast(:q as string), '%'))) " +
           "and (exists (select 1 from Alumno a2 join a2.disciplinas d where a2 = a and d.id in :disciplinaIds) " +
           "  or exists (select 1 from Membresia m join m.disciplinas md where m.alumno = a " +
           "             and m.estado in (com.nexora.sport.model.EstadoMembresia.ACTIVA, com.nexora.sport.model.EstadoMembresia.PENDIENTE) " +
           "             and md.id in :disciplinaIds) " +
           "  or exists (select 1 from Reserva r where r.alumno = a and r.clase.id in :claseIds " +
           "             and r.estado <> com.nexora.sport.model.EstadoReserva.CANCELADA))")
    Page<Alumno> buscarDeClases(@Param("centroId") Long centroId, @Param("q") String q,
                                @Param("disciplinaIds") java.util.Collection<Long> disciplinaIds,
                                @Param("claseIds") java.util.Collection<Long> claseIds, Pageable pageable);

    java.util.Optional<Alumno> findByCodigoQrAndDeletedAtIsNull(String codigoQr);

    /** Posibles coincidencias con el cliente de un apartado de plan (para no duplicar alumnos
     * al convertirlo): mismo correo, mismo telefono (ultimos 10 digitos, sin importar lada o
     * separadores) o nombre parecido. Los parametros vacios no empatan nada. */
    @Query("select a from Alumno a where a.centro.id = :centroId and a.deletedAt is null and (" +
           "  (:email <> '' and lower(a.email) = lower(:email)) " +
           // replace anidado (funcion estandar de HQL, tipada como string) en vez de
           // regexp_replace, que Hibernate trata como Object y rechaza en un "like".
           "  or (:telefono <> '' and replace(replace(replace(replace(replace(a.telefono, ' ', ''), '-', ''), '(', ''), ')', ''), '+', '') " +
           "      like concat('%', :telefono)) " +
           "  or (:nombre <> '' and lower(a.nombre) like lower(concat('%', :nombre, '%')))" +
           ") order by a.nombre")
    List<Alumno> buscarCoincidencias(@Param("centroId") Long centroId, @Param("email") String email,
                                      @Param("telefono") String telefono, @Param("nombre") String nombre,
                                      Pageable pageable);

    long countByCentroIdAndEstadoAndDeletedAtIsNull(Long centroId, EstadoAlumno estado);

    long countByCentroIdAndDeletedAtIsNull(Long centroId);

    long countByCentroIdAndFechaIngresoBetweenAndDeletedAtIsNull(Long centroId, LocalDate desde, LocalDate hasta);

    long countByCentroIdAndFechaBajaBetween(Long centroId, LocalDateTime desde, LocalDateTime hasta);

    @Query(nativeQuery = true, value =
            // CAST(...) en vez de "::date": Spring Data JPA confunde "::" con un named parameter ":xxx" en native queries.
            "select CAST(date_trunc('month', CAST(a.fecha_ingreso as timestamp)) as date) as mes, 0 as total, count(*) as cantidad " +
            "from alumnos a where a.centro_id = :centroId and a.deleted_at is null " +
            "and a.fecha_ingreso >= :desde and a.fecha_ingreso <= :hasta group by 1 order by 1")
    List<Object[]> nuevosPorMes(@Param("centroId") Long centroId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** Para ARCO: el correo de un alumno identifica al titular en el formulario publico (cualquier centro). */
    @Query("select a from Alumno a where a.deletedAt is null and lower(a.email) = lower(:email)")
    List<Alumno> buscarPorEmailArco(@Param("email") String email);

    /** Para ARCO (resumen de Acceso): fichas de alumno por correo o telefono. Vacio = no filtra por ese dato. */
    @Query("select a from Alumno a where a.deletedAt is null and ((:email <> '' and lower(a.email) = lower(:email)) " +
           "or (:telefono <> '' and a.telefono = :telefono))")
    List<Alumno> buscarTitularArco(@Param("email") String email, @Param("telefono") String telefono);
}
