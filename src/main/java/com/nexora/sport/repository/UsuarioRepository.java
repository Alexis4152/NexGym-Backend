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

    @Query("select u from Usuario u where u.centro.id = :centroId and u.deletedAt is null and u.activo = true " +
           "and :seccion member of u.rol.secciones")
    List<Usuario> findActivosConSeccion(@Param("centroId") Long centroId, @Param("seccion") Seccion seccion);
}
