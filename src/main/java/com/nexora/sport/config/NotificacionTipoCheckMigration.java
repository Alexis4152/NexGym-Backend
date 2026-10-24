package com.nexora.sport.config;

import com.nexora.sport.model.TipoNotificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Hibernate 6 creo un CHECK sobre notificaciones.tipo con los valores que tenia el enum
 * TipoNotificacion cuando se creo la tabla, y ddl-auto=update NO lo actualiza al agregar
 * valores nuevos (ver nota en init.sql). Sin esto, insertar un tipo nuevo truena con
 * "violates check constraint" -- y como NotificacionService#crear corre dentro de la
 * transaccion de quien la dispara, tumbaria tambien esa operacion (p. ej. el apartado).
 *
 * Al arrancar compara el CHECK actual contra el enum y, solo si le falta algun valor, lo
 * reemplaza por uno con TODOS los valores actuales. Idempotente: si ya esta al dia no
 * toca nada. Reemplaza el migration_*.sql manual para este caso puntual (en Coolify no
 * hay forma comoda de correr psql en cada deploy).
 */
@Component
@Order(1)
public class NotificacionTipoCheckMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(NotificacionTipoCheckMigration.class);
    private static final String NOMBRE_CHECK = "notificaciones_tipo_check";

    private final JdbcTemplate jdbc;

    public NotificacionTipoCheckMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        try {
            List<Map<String, Object>> checks = jdbc.queryForList(
                    "select con.conname as nombre, pg_get_constraintdef(con.oid) as definicion " +
                    "from pg_constraint con join pg_class rel on rel.oid = con.conrelid " +
                    "where rel.relname = 'notificaciones' and con.contype = 'c'");
            // Solo el de la columna tipo -- no el de destinatario_tipo.
            List<Map<String, Object>> deTipo = checks.stream()
                    .filter(c -> String.valueOf(c.get("definicion")).matches("(?s).*\\(\\(?tipo\\)?::text.*"))
                    .toList();
            List<String> valores = Arrays.stream(TipoNotificacion.values()).map(Enum::name).toList();

            boolean alDia = deTipo.size() == 1 && valores.stream()
                    .allMatch(v -> String.valueOf(deTipo.get(0).get("definicion")).contains("'" + v + "'"));
            if (alDia) return;

            for (Map<String, Object> c : deTipo) {
                jdbc.execute("alter table notificaciones drop constraint if exists \"" + c.get("nombre") + "\"");
            }
            String lista = valores.stream().map(v -> "'" + v + "'").collect(Collectors.joining(","));
            jdbc.execute("alter table notificaciones add constraint " + NOMBRE_CHECK + " check (tipo in (" + lista + "))");
            log.info("CHECK de notificaciones.tipo actualizado con {} tipos", valores.size());
        } catch (Exception e) {
            // Nunca impedir el arranque por esto; queda en el log para revisarlo.
            log.error("No se pudo actualizar el CHECK de notificaciones.tipo: {}", e.getMessage());
        }
    }
}
