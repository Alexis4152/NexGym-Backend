package com.nexora.sport.service;

import com.nexora.sport.dto.MailConfigDto;
import com.nexora.sport.dto.MailConfigRequest;
import com.nexora.sport.model.MailConfig;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.MailConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MailConfigService {

    private final MailConfigRepository mailConfigRepository;

    public MailConfigService(MailConfigRepository mailConfigRepository) {
        this.mailConfigRepository = mailConfigRepository;
    }

    /** La fila unica de configuracion (la siembra MailConfigDataInitializer al arrancar). */
    @Transactional(readOnly = true)
    public MailConfig getEntity() {
        return mailConfigRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("La configuracion de correo no esta inicializada"));
    }

    @Transactional(readOnly = true)
    public MailConfigDto get() {
        return toDto(getEntity());
    }

    @Transactional
    public MailConfigDto update(MailConfigRequest request, Usuario actor) {
        MailConfig config = getEntity();
        config.setEnabled(Boolean.TRUE.equals(request.enabled()));
        config.setSmtpHost(request.smtpHost().trim());
        config.setSmtpPort(request.smtpPort());
        config.setSmtpUsername(request.smtpUsername().trim());
        if (request.smtpPassword() != null && !request.smtpPassword().isBlank()) {
            // Los App Passwords de Google se muestran en grupos de 4 con espacios; Gmail los
            // acepta sin ellos, asi que se limpian para que pegarlo tal cual funcione.
            config.setSmtpPassword(request.smtpPassword().replace(" ", ""));
        }
        config.setUpdatedAt(LocalDateTime.now());
        config.setUpdatedBy(actor);
        return toDto(mailConfigRepository.save(config));
    }

    private MailConfigDto toDto(MailConfig c) {
        return new MailConfigDto(
                c.isEnabled(),
                c.getSmtpHost(),
                c.getSmtpPort(),
                c.getSmtpUsername(),
                c.getSmtpPassword() != null && !c.getSmtpPassword().isBlank()
        );
    }
}
