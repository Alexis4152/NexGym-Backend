-- ============================================================
--  NexoraSport — Migracion: Notificaciones proactivas v1
--  DB: nexorasport  |  host: localhost:5433
--  Run: psql -U postgres -h localhost -p 5433 -d nexorasport -f migration_notificaciones_v1.sql
--
--  Las tablas nuevas (notificaciones, notificacion_envios) las crea solo Hibernate via
--  ddl-auto=update al arrancar el backend. Las columnas nuevas en centros NO: son
--  NOT NULL y la tabla ya tenia filas (mismo caso que migration_membresias_v2.sql), asi
--  que ddl-auto=update falla con "column contains null values" si no se agregan a mano
--  primero, con DEFAULT, antes de que Hibernate intente el ALTER. Idempotente.
-- ============================================================

ALTER TABLE centros ADD COLUMN IF NOT EXISTS notificaciones_membresia_activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE centros ADD COLUMN IF NOT EXISTS notificaciones_clase_activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE centros ADD COLUMN IF NOT EXISTS notificaciones_email_activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE centros ADD COLUMN IF NOT EXISTS notificaciones_interno_activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE centros ADD COLUMN IF NOT EXISTS notificaciones_dias_antes_vencimiento VARCHAR(60) DEFAULT '7,3,1,0';
ALTER TABLE centros ADD COLUMN IF NOT EXISTS notificaciones_horas_antes_clase INTEGER DEFAULT 24;

-- Da de alta la nueva seccion NOTIFICACIONES en los roles de sistema que ya existian
-- antes de este cambio (RolService#seedRolesPorDefecto solo siembra roles nuevos, no
-- actualiza los ya creados) — sin este backfill, el Dueno/Administrador/Recepcion/
-- SUPER_ADMIN de un centro ya existente no verian la seccion hasta agregarla a mano.
INSERT INTO role_secciones (role_id, seccion)
SELECT id, 'NOTIFICACIONES' FROM roles
WHERE nombre IN ('Dueno', 'Administrador', 'Recepcion', 'SUPER_ADMIN')
  AND id NOT IN (SELECT role_id FROM role_secciones WHERE seccion = 'NOTIFICACIONES');

-- Nota: "Entrenador" NO recibe NOTIFICACIONES a proposito: los avisos administrativos
-- (CENTRO_ADMIN) incluyen datos de cobranza (saldos pendientes, resumenes de membresia)
-- que el encargo pide no exponer a instructores (seccion 13). Si un centro quiere que su
-- staff de piso vea notificaciones, puede agregarsela manualmente desde Roles.

-- Indices de soporte: Hibernate solo crea la PK y el UNIQUE de dedupe_key al generar
-- las tablas, no indices por patron de consulta. La campana hace polling cada 60s
-- (centro_id + destinatario_tipo + interno_visible + leida) y el job de reintentos
-- corre cada 15 min (estado + intentos) — sin estos indices ambas consultas escanean
-- toda la tabla conforme crece.
CREATE INDEX IF NOT EXISTS idx_notificaciones_centro_dest ON notificaciones(centro_id, destinatario_tipo, interno_visible, leida);
CREATE INDEX IF NOT EXISTS idx_notificaciones_alumno ON notificaciones(alumno_id);
CREATE INDEX IF NOT EXISTS idx_notificacion_envios_estado ON notificacion_envios(estado, intentos);
