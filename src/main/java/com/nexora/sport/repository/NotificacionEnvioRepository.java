package com.nexora.sport.repository;

import com.nexora.sport.model.EstadoEnvio;
import com.nexora.sport.model.NotificacionEnvio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionEnvioRepository extends JpaRepository<NotificacionEnvio, Long> {
    List<NotificacionEnvio> findByEstadoAndIntentosLessThan(EstadoEnvio estado, int maxIntentos);
}
