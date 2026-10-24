package com.nexora.sport.repository;

import com.nexora.sport.model.MailConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MailConfigRepository extends JpaRepository<MailConfig, Long> {
    Optional<MailConfig> findFirstByOrderByIdAsc();
}
