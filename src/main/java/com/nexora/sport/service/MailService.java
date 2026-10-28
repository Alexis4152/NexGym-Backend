package com.nexora.sport.service;

import com.nexora.sport.model.Centro;
import com.nexora.sport.model.MailConfig;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * Envios de correo. La cuenta SMTP se lee de {@link MailConfig} (tabla mail_config) en CADA
 * envio, no del JavaMailSender autoconfigurado por spring.mail.* -- asi el SUPER_ADMIN puede
 * cambiar de cuenta o rotar la contrasena desde "Correo del sistema" sin reiniciar.
 *
 * El "De:" siempre es la cuenta autenticada (Gmail no deja mandar con otro remitente); lo
 * que cambia por centro es el nombre visible y el "Responder a" (Centro#emailContacto), ver
 * {@link Remitente} -- igual que DemoPV con Tienda#contactEmail. Los correos de cuenta/
 * plataforma (recuperar contrasena, accesos) salen sin Remitente, como "NexoraSport".
 *
 * send/enviarConAdjunto son "best effort": nunca deben tumbar la operacion que los dispara.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private static final String NOMBRE_REMITENTE = "NexoraSport";

    /**
     * Nombre visible y "Responder a" de un correo operativo de un centro (ticket, apartado,
     * QR del alumno, notificaciones). Se arma en el hilo de quien llama, con la transaccion
     * abierta: los envios son @Async y ahi el Centro (LAZY) ya no se podria leer.
     */
    public record Remitente(String nombre, String responderA) {
        public static Remitente de(Centro centro) {
            if (centro == null) return null;
            return new Remitente(centro.getNombre(), centro.getEmailContacto());
        }
    }

    private final MailConfigService mailConfigService;

    public MailService(MailConfigService mailConfigService) {
        this.mailConfigService = mailConfigService;
    }

    @Async
    public void send(String to, String subject, String body) {
        send(null, to, subject, body);
    }

    @Async
    public void send(Remitente remitente, String to, String subject, String body) {
        if (to == null || to.isBlank()) return;
        MailConfig cfg = mailConfigService.getEntity();
        if (!listo(cfg)) {
            log.warn("Envio de correo deshabilitado o sin cuenta configurada; se omite \"{}\" para {}", subject, to);
            return;
        }
        try {
            enviar(cfg, remitente, to, subject, body, null, null);
        } catch (Exception e) {
            log.warn("No se pudo enviar correo a {}: {}", to, e.getMessage());
        }
    }

    public void sendSync(String to, String subject, String body) {
        sendSync(null, to, subject, body);
    }

    /**
     * Version sincrona de {@link #send}: NO atrapa el error, lo propaga. Pensada para quien
     * necesita saber si el envio realmente funciono (NotificacionEmailDispatcher, y el boton
     * "Enviar correo de prueba" de la configuracion de correo).
     */
    public void sendSync(Remitente remitente, String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Destinatario vacio");
        }
        MailConfig cfg = mailConfigService.getEntity();
        if (!cfg.isEnabled()) {
            throw new IllegalStateException("El envio de correo esta deshabilitado (Correo del sistema)");
        }
        if (!listo(cfg)) {
            throw new IllegalStateException("No hay una cuenta de correo configurada (Correo del sistema)");
        }
        try {
            enviar(cfg, remitente, to, subject, body, null, null);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Async
    public void enviarConAdjunto(Remitente remitente, String to, String subject, String body,
                                 String nombreArchivo, byte[] adjunto) {
        if (to == null || to.isBlank()) return;
        MailConfig cfg = mailConfigService.getEntity();
        if (!listo(cfg)) {
            log.warn("Envio de correo deshabilitado o sin cuenta configurada; se omite \"{}\" para {}", subject, to);
            return;
        }
        try {
            enviar(cfg, remitente, to, subject, body, nombreArchivo, adjunto);
        } catch (Exception e) {
            log.warn("No se pudo enviar correo con adjunto a {}: {}", to, e.getMessage());
        }
    }

    private boolean listo(MailConfig cfg) {
        return cfg.isEnabled()
                && cfg.getSmtpUsername() != null && !cfg.getSmtpUsername().isBlank()
                && cfg.getSmtpPassword() != null && !cfg.getSmtpPassword().isBlank();
    }

    private void enviar(MailConfig cfg, Remitente remitente, String to, String subject, String body,
                        String nombreArchivo, byte[] adjunto) throws Exception {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(cfg.getSmtpHost());
        sender.setPort(cfg.getSmtpPort());
        sender.setUsername(cfg.getSmtpUsername());
        sender.setPassword(cfg.getSmtpPassword());
        sender.setDefaultEncoding("UTF-8");
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.writetimeout", "15000");

        MimeMessage message = sender.createMimeMessage();
        boolean multipart = adjunto != null;
        MimeMessageHelper helper = new MimeMessageHelper(message, multipart, "UTF-8");
        String nombreVisible = remitente != null && remitente.nombre() != null && !remitente.nombre().isBlank()
                ? remitente.nombre() : NOMBRE_REMITENTE;
        helper.setFrom(new InternetAddress(cfg.getSmtpUsername(), nombreVisible, "UTF-8"));
        if (remitente != null && remitente.responderA() != null && !remitente.responderA().isBlank()) {
            // Un correo de contacto mal capturado no debe impedir que salga el correo.
            try {
                helper.setReplyTo(new InternetAddress(remitente.responderA().trim(), true));
            } catch (Exception e) {
                log.warn("Correo de contacto invalido para \"Responder a\" ({}): se omite", remitente.responderA());
            }
        }
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body);
        if (multipart) {
            helper.addAttachment(nombreArchivo, new ByteArrayResource(adjunto));
        }
        sender.send(message);
    }
}
