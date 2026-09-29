package com.nexora.sport.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** Envios de correo "best effort": nunca deben tumbar la operacion que los dispara. */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void send(String to, String subject, String body) {
        if (to == null || to.isBlank() || fromAddress == null || fromAddress.isBlank()) return;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("No se pudo enviar correo a {}: {}", to, e.getMessage());
        }
    }

    /**
     * Version sincrona de {@link #send}: NO atrapa el error, lo propaga. Pensada para
     * quien necesita saber si el envio realmente funciono (ver NotificacionEmailDispatcher,
     * que la llama desde su propio metodo @Async para poder registrar el estado de envio
     * sin bloquear la transaccion que disparo la notificacion). mailSender.send() lanza
     * MailException (unchecked) si falla; se deja propagar a proposito, el caller decide.
     */
    public void sendSync(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Destinatario vacio");
        }
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("No hay una cuenta de correo configurada (spring.mail.username)");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    @Async
    public void enviarConAdjunto(String to, String subject, String body, String nombreArchivo, byte[] adjunto) {
        if (to == null || to.isBlank() || fromAddress == null || fromAddress.isBlank()) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            helper.addAttachment(nombreArchivo, new ByteArrayResource(adjunto));
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("No se pudo enviar correo con adjunto a {}: {}", to, e.getMessage());
        }
    }
}
