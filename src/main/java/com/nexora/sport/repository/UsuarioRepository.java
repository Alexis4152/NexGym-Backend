package com.nexora.sport.repository;

import com.nexora.sport.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailAndDeletedAtIsNull(String email);
    boolean existsByEmail(String email);
    Page<Usuario> findByCentroIdAndDeletedAtIsNull(Long centroId, Pageable pageable);
}
