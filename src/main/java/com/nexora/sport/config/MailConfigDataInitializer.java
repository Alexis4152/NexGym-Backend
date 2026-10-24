package com.nexora.sport.config;

import com.nexora.sport.model.MailConfig;
import com.nexora.sport.repository.MailConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Siembra la fila unica de mail_config al primer arranque, tomando como valores iniciales
 * las variables APP_MAIL_* (spring.mail.*) que se usaban antes -- asi un despliegue que ya
 * tenia el correo configurado por variables de entorno sigue enviando sin hacer nada. Si
 * no hay cuenta configurada, queda deshabilitada hasta que el SUPER_ADMIN la llene desde
 * "Correo del sistema". Despues del primer arranque las variables ya no se leen.
 */
@Component
public class MailConfigDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MailConfigDataInitializer.class);

    private final MailConfigRepository mailConfigRepository;

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String host;
    @Value("${spring.mail.port:587}")
    private Integer port;
    @Value("${spring.mail.username:}")
    private String username;
    @Value("${spring.mail.password:}")
    private String password;

    public MailConfigDataInitializer(MailConfigRepository mailConfigRepository) {
        this.mailConfigRepository = mailConfigRepository;
    }

    @Override
    public void run(String... args) {
        if (mailConfigRepository.count() > 0) return;
        boolean conCuenta = !username.isBlank() && !password.isBlank();
        MailConfig cfg = new MailConfig();
        cfg.setEnabled(conCuenta);
        cfg.setSmtpHost(host);
        cfg.setSmtpPort(port);
        cfg.setSmtpUsername(username.isBlank() ? null : username);
        cfg.setSmtpPassword(password.isBlank() ? null : password);
        mailConfigRepository.save(cfg);
        if (conCuenta) {
            log.info("Configuracion de correo sembrada en mail_config desde APP_MAIL_* ({})", username);
        } else {
            log.info("Configuracion de correo sembrada en mail_config (deshabilitada, sin cuenta) -- configurala en \"Correo del sistema\"");
        }
    }
}
