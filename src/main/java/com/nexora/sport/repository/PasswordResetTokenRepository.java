package com.nexora.sport.repository;

import com.nexora.sport.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);

    /** Evita que queden varios codigos validos a la vez si el usuario pide reset mas de una vez (paridad con RefreshToken#revokeAllForUser). */
    @Modifying
    @Query("update PasswordResetToken t set t.used = true where t.usuario.id = :usuarioId and t.used = false")
    void invalidateAllForUser(Long usuarioId);
}
