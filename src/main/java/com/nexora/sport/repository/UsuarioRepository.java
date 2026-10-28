package com.nexora.sport.repository;

import com.nexora.sport.model.Seccion;
import com.nexora.sport.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailAndDeletedAtIsNull(String email);
    boolean existsByEmail(String email);
    Page<Usuario> findByCentroIdAndDeletedAtIsNull(Long centroId, Pageable pageable);

    /** Usuarios vigentes con un rol (no se elimina un rol que alguien sigue usando). */
    long countByRolIdAndDeletedAtIsNull(Long rolId);

    @Query("select u from Usuario u where u.centro.id = :centroId and u.deletedAt is null and u.activo = true " +
           "and :seccion member of u.rol.secciones")
    List<Usuario> findActivosConSeccion(@Param("centroId") Long centroId, @Param("seccion") Seccion seccion);

    /** Para ARCO: el correo puede venir con otras mayusculas que las guardadas. */
    @Query("select u from Usuario u where u.deletedAt is null and lower(u.email) = lower(:email)")
    Optional<Usuario> buscarPorEmailArco(@Param("email") String email);

    /** SUPER_ADMIN activos: atienden las solicitudes ARCO (obligacion de la plataforma). */
    @Query("select u from Usuario u where u.deletedAt is null and u.activo = true " +
           "and u.rol.nivel = com.nexora.sport.model.NivelJerarquia.SUPER_ADMIN")
    List<Usuario> superAdminsActivos();
}
