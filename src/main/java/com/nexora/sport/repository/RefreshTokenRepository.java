package com.nexora.sport.repository;

import com.nexora.sport.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.usuario.id = :usuarioId")
    void revokeAllForUser(Long usuarioId);
}
