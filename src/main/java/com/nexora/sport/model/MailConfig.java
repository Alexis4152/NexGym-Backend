package com.nexora.sport.model;

import com.nexora.sport.security.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Cuenta SMTP unica y global con la que salen TODOS los correos de la plataforma (alta de
 * usuarios, recuperacion de contrasena, tickets, notificaciones). Antes vivia fija en
 * variables de entorno (spring.mail.*); ahora es una fila editable en caliente por el
 * SUPER_ADMIN desde /api/admin/mail-config, sin reiniciar el backend (igual que DemoPV).
 * Solo existe una fila: la siembra MailConfigDataInitializer al primer arranque.
 */
@Entity
@Table(name = "mail_config")
@Getter
@Setter
public class MailConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "smtp_host", nullable = false, length = 150)
    private String smtpHost;

    @Column(name = "smtp_port", nullable = false)
    private Integer smtpPort;

    @Column(name = "smtp_username", length = 150)
    private String smtpUsername;

    /** Cifrada en la BD (AES-GCM, ver EncryptedStringConverter). Nunca se devuelve por la API. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "smtp_password", length = 255)
    private String smtpPassword;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Usuario updatedBy;
}
