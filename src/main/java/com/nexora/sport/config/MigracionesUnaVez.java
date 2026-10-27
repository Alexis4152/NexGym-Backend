package com.nexora.sport.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ajustes de datos/esquema que deben correr UNA sola vez por base (ddl-auto=update no los
 * hace y en Coolify no hay forma comoda de correr psql en cada deploy). Cada uno se anota en
 * migraciones_aplicadas para no repetirse: p. ej. si el dueno vuelve a darle al Entrenador un
 * permiso que aqui se le quito, el siguiente arranque no se lo vuelve a quitar.
 */
@Component
@Order(2)
public class MigracionesUnaVez implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionesUnaVez.class);

    private final JdbcTemplate jdbc;

    public MigracionesUnaVez(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        try {
            jdbc.execute("create table if not exists migraciones_aplicadas (" +
                    "clave varchar(100) primary key, aplicada_en timestamp not null default now())");
        } catch (Exception e) {
            log.error("No se pudo crear migraciones_aplicadas: {}", e.getMessage());
            return;
        }

        // Un diploma por instructor sin importar sus disciplinas: la columna deja de ser obligatoria.
        aplicar("2026-10-documento-instructor-sin-disciplina",
                "alter table documentos_instructor alter column disciplina_id drop not null");

        // El Entrenador ya no registra asistencias por defecto: el dueno se lo da en Roles si quiere.
        aplicar("2026-10-entrenador-sin-registrar-asistencias",
                "delete from role_permisos where permiso = 'ASISTENCIAS_REGISTRAR' " +
                "and role_id in (select id from roles where es_sistema = true and nombre = 'Entrenador')");
    }

    private void aplicar(String clave, String sql) {
        try {
            Integer ya = jdbc.queryForObject("select count(*) from migraciones_aplicadas where clave = ?", Integer.class, clave);
            if (ya != null && ya > 0) return;
            jdbc.execute(sql);
            jdbc.update("insert into migraciones_aplicadas (clave) values (?)", clave);
            log.info("Migracion aplicada: {}", clave);
        } catch (Exception e) {
            // Nunca impedir el arranque por esto; se reintenta en el siguiente.
            log.error("No se pudo aplicar la migracion {}: {}", clave, e.getMessage());
        }
    }
}
