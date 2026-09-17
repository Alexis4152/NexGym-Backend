CREATE TABLE sucursales (
    id BIGSERIAL PRIMARY KEY,
    centro_id BIGINT NOT NULL REFERENCES centros(id),
    nombre VARCHAR(150) NOT NULL,
    direccion VARCHAR(300),
    notas VARCHAR(300),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_sucursales_centro ON sucursales(centro_id);

-- Sucursal Principal por defecto para cada centro existente (preserva datos historicos)
INSERT INTO sucursales (centro_id, nombre)
SELECT id, 'Sucursal Principal' FROM centros;

ALTER TABLE lugares ADD COLUMN sucursal_id BIGINT REFERENCES sucursales(id);
ALTER TABLE lugares ADD COLUMN capacidad_maxima INTEGER;

UPDATE lugares l
SET sucursal_id = s.id
FROM sucursales s
WHERE s.centro_id = l.centro_id AND s.nombre = 'Sucursal Principal';

ALTER TABLE lugares ALTER COLUMN sucursal_id SET NOT NULL;

CREATE TABLE lugar_disciplinas (
    lugar_id BIGINT NOT NULL REFERENCES lugares(id) ON DELETE CASCADE,
    disciplina_id BIGINT NOT NULL REFERENCES disciplinas(id) ON DELETE CASCADE,
    PRIMARY KEY (lugar_id, disciplina_id)
);

ALTER TABLE disciplinas ADD COLUMN requiere_instalacion BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE clases ADD COLUMN sucursal_id BIGINT REFERENCES sucursales(id);

UPDATE clases c
SET sucursal_id = s.id
FROM sucursales s
WHERE s.centro_id = c.centro_id AND s.nombre = 'Sucursal Principal';

ALTER TABLE clases ALTER COLUMN sucursal_id SET NOT NULL;
