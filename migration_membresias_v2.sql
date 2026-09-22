-- ============================================================
--  NexoraSport — Migracion: Membresias y cobranza v2
--  DB: nexorasport  |  host: localhost:5433
--  Run: psql -U postgres -h localhost -p 5433 -d nexorasport -f migration_membresias_v2.sql
--
--  Necesaria porque ddl-auto=update NO puede agregar solo columnas NOT NULL sin
--  default seguro en tablas que ya tienen filas (membresia_planes/membresias ya
--  tenian datos de las pruebas anteriores) - el mismo caso que ya se dio con
--  apartados_activo/reservable. Este script hace el backfill ANTES de que Hibernate
--  intente tocar el esquema; es idempotente (puede correrse mas de una vez sin romper
--  nada) y tambien limpia columnas que el nuevo modelo ya no usa (Hibernate nunca
--  las hubiera borrado solo).
-- ============================================================

-- ---- membresia_planes: de tipo_periodo (enum rigido) a tipo_plan + duracion flexible ----
ALTER TABLE membresia_planes ADD COLUMN IF NOT EXISTS tipo_plan VARCHAR(20);
ALTER TABLE membresia_planes ADD COLUMN IF NOT EXISTS duracion_cantidad INTEGER;
ALTER TABLE membresia_planes ADD COLUMN IF NOT EXISTS duracion_unidad VARCHAR(10);
ALTER TABLE membresia_planes ADD COLUMN IF NOT EXISTS max_disciplinas_seleccionables INTEGER;
ALTER TABLE membresia_planes ADD COLUMN IF NOT EXISTS permite_abonos BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE membresia_planes ADD COLUMN IF NOT EXISTS monto_minimo_abono NUMERIC(10,2);

-- Backfill de tipo_plan y duracion a partir del tipo_periodo/duracion_dias anteriores
-- (solo corre si esas columnas todavia existen; en una BD nueva, sin filas, no aplica nada).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='membresia_planes' AND column_name='tipo_periodo') THEN
        UPDATE membresia_planes SET tipo_plan = CASE tipo_periodo
                WHEN 'POR_CLASES' THEN 'POR_CLASES'
                WHEN 'PERSONALIZADO' THEN 'PERSONALIZADO'
                ELSE 'PERIODO' END
            WHERE tipo_plan IS NULL;

        UPDATE membresia_planes SET
                duracion_cantidad = COALESCE(duracion_dias, CASE tipo_periodo
                    WHEN 'SEMANAL' THEN 1 WHEN 'QUINCENAL' THEN 15 WHEN 'MENSUAL' THEN 1 ELSE NULL END),
                duracion_unidad = CASE WHEN duracion_dias IS NOT NULL THEN 'DIA' ELSE CASE tipo_periodo
                    WHEN 'SEMANAL' THEN 'SEMANA' WHEN 'QUINCENAL' THEN 'DIA' WHEN 'MENSUAL' THEN 'MES' ELSE NULL END END
            WHERE tipo_plan = 'PERIODO' AND duracion_cantidad IS NULL;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='membresia_planes' AND column_name='multidisciplina') THEN
        UPDATE membresia_planes SET max_disciplinas_seleccionables = CASE WHEN multidisciplina THEN 2 ELSE 1 END
            WHERE max_disciplinas_seleccionables IS NULL AND acceso_completo = FALSE;
    ELSE
        UPDATE membresia_planes SET max_disciplinas_seleccionables = 1
            WHERE max_disciplinas_seleccionables IS NULL AND acceso_completo = FALSE;
    END IF;
END $$;

UPDATE membresia_planes SET tipo_plan = 'PERSONALIZADO' WHERE tipo_plan IS NULL;
ALTER TABLE membresia_planes ALTER COLUMN tipo_plan SET NOT NULL;

ALTER TABLE membresia_planes DROP COLUMN IF EXISTS tipo_periodo;
ALTER TABLE membresia_planes DROP COLUMN IF EXISTS duracion_dias;
ALTER TABLE membresia_planes DROP COLUMN IF EXISTS multidisciplina;

-- ---- membresias: snapshot historico de la contratacion + cadena de renovacion + suspension/cancelacion ----
ALTER TABLE membresias ADD COLUMN IF NOT EXISTS plan_nombre_snapshot VARCHAR(100);
ALTER TABLE membresias ADD COLUMN IF NOT EXISTS tipo_plan_snapshot VARCHAR(20);
ALTER TABLE membresias ADD COLUMN IF NOT EXISTS numero_clases_contratadas INTEGER;
ALTER TABLE membresias ADD COLUMN IF NOT EXISTS precio_original NUMERIC(10,2);
ALTER TABLE membresias ADD COLUMN IF NOT EXISTS descuento NUMERIC(10,2) NOT NULL DEFAULT 0;
ALTER TABLE membresias ADD COLUMN IF NOT EXISTS membresia_anterior_id BIGINT REFERENCES membresias(id);
ALTER TABLE membresias ADD COLUMN IF NOT EXISTS suspendida_motivo VARCHAR(300);
ALTER TABLE membresias ADD COLUMN IF NOT EXISTS suspendida_en TIMESTAMP;
ALTER TABLE membresias ADD COLUMN IF NOT EXISTS suspendida_por BIGINT REFERENCES usuarios(id);
ALTER TABLE membresias ADD COLUMN IF NOT EXISTS cancelada_motivo VARCHAR(300);
ALTER TABLE membresias ADD COLUMN IF NOT EXISTS cancelada_en TIMESTAMP;
ALTER TABLE membresias ADD COLUMN IF NOT EXISTS cancelada_por BIGINT REFERENCES usuarios(id);

-- fecha_fin ahora es opcional (POR_CLASES/PASE sin vigencia configurada, ver seccion 17 del encargo).
ALTER TABLE membresias ALTER COLUMN fecha_fin DROP NOT NULL;

-- Backfill: nombre/tipo del plan tal como esta HOY (no hay forma de recuperar el nombre historico real
-- si el plan ya se renombro antes de esta migracion; es la mejor aproximacion posible retroactivamente).
UPDATE membresias m SET plan_nombre_snapshot = p.nombre
    FROM membresia_planes p WHERE p.id = m.plan_id AND m.plan_nombre_snapshot IS NULL;
UPDATE membresias m SET tipo_plan_snapshot = p.tipo_plan
    FROM membresia_planes p WHERE p.id = m.plan_id AND m.tipo_plan_snapshot IS NULL;
UPDATE membresias SET numero_clases_contratadas = clases_restantes WHERE numero_clases_contratadas IS NULL;
-- precio_original: sin descuento historico registrado antes de esta migracion, se asume 0 (precio_original = precio_final).
UPDATE membresias SET precio_original = precio_final WHERE precio_original IS NULL;

ALTER TABLE membresias ALTER COLUMN plan_nombre_snapshot SET NOT NULL;
ALTER TABLE membresias ALTER COLUMN tipo_plan_snapshot SET NOT NULL;
ALTER TABLE membresias ALTER COLUMN precio_original SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_membresias_anterior ON membresias(membresia_anterior_id);

-- ---- membresia_disciplinas / membresia_planes ya usan M:N con disciplinas; nueva tabla equivalente para Membresia ----
CREATE TABLE IF NOT EXISTS membresia_disciplinas (
    membresia_id    BIGINT NOT NULL REFERENCES membresias(id) ON DELETE CASCADE,
    disciplina_id   BIGINT NOT NULL REFERENCES disciplinas(id),
    PRIMARY KEY (membresia_id, disciplina_id)
);

-- ---- pagos_membresia: historial de pagos/abonos, nunca se sobrescribe ni se borra ----
CREATE TABLE IF NOT EXISTS pagos_membresia (
    id                      BIGSERIAL PRIMARY KEY,
    membresia_id            BIGINT NOT NULL REFERENCES membresias(id),
    monto                   NUMERIC(10,2) NOT NULL,
    metodo_pago             VARCHAR(20) NOT NULL DEFAULT 'EFECTIVO',
    fecha                   DATE NOT NULL DEFAULT CURRENT_DATE,
    estado                  VARCHAR(20) NOT NULL DEFAULT 'VALIDO',
    movimiento_financiero_id BIGINT REFERENCES movimientos_financieros(id),
    registrado_por          BIGINT REFERENCES usuarios(id),
    motivo_cancelacion      VARCHAR(300),
    cancelado_en            TIMESTAMP,
    cancelado_por           BIGINT REFERENCES usuarios(id),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_pagos_membresia_membresia ON pagos_membresia(membresia_id);

-- ---- movimientos_financieros: soft-void para poder revertir un pago cancelado sin borrar historial ----
ALTER TABLE movimientos_financieros ADD COLUMN IF NOT EXISTS anulado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE movimientos_financieros ADD COLUMN IF NOT EXISTS anulado_en TIMESTAMP;
ALTER TABLE movimientos_financieros ADD COLUMN IF NOT EXISTS anulado_por BIGINT REFERENCES usuarios(id);

-- ---- centros: politica de acceso con adeudo (seccion 15 del encargo; el modulo de Asistencia la leera a futuro) ----
ALTER TABLE centros ADD COLUMN IF NOT EXISTS permitir_acceso_con_adeudo BOOLEAN NOT NULL DEFAULT TRUE;
