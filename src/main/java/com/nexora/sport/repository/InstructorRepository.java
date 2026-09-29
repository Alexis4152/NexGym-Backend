package com.nexora.sport.repository;

import com.nexora.sport.model.Instructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface InstructorRepository extends JpaRepository<Instructor, Long> {
    Page<Instructor> findByCentroId(Long centroId, Pageable pageable);
    Page<Instructor> findByCentroIdAndNombreContainingIgnoreCase(Long centroId, String nombre, Pageable pageable);
    List<Instructor> findByCentroIdAndActivoTrue(Long centroId);

    @Query("select i from Instructor i left join fetch i.disciplinas where i.id = :id")
    Optional<Instructor> findWithDisciplinasById(@Param("id") Long id);

    /** Ficha de instructor ligada a esta cuenta de login (si existe), para acotar "mis clases". */
    Optional<Instructor> findByUsuarioId(Long usuarioId);

    /** Alcance por sucursal (ver TenantScope#sucursalesPermitidas): un instructor sin
     * ninguna sucursal asignada (i.sucursales vacio) se considera "disponible en todas",
     * asi que siempre es visible sin importar el filtro. `sucursalIds` NUNCA debe venir
     * null aqui (para eso ya estan findByCentroId*).
     * `cast(:q as string)`: sin el cast, cuando :q llega null Postgres no logra inferir
     * su tipo dentro de lower(...) y truena con "no existe la funcion lower(bytea)"
     * (bug de tipificacion de parametros de postgres/JDBC con valores null ambiguos,
     * no exclusivo de esta query -- ver reporte final). */
    @Query("select distinct i from Instructor i left join i.sucursales s where i.centro.id = :centroId " +
           "and (i.sucursales is empty or s.id in :sucursalIds) " +
           "and (cast(:q as string) is null or lower(i.nombre) like lower(concat('%', cast(:q as string), '%')))")
    Page<Instructor> buscarEnAlcance(@Param("centroId") Long centroId, @Param("sucursalIds") Set<Long> sucursalIds,
                                      @Param("q") String q, Pageable pageable);
}
