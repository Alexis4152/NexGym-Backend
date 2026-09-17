CREATE TABLE lugares (
    id BIGSERIAL PRIMARY KEY,
    centro_id BIGINT NOT NULL REFERENCES centros(id),
    nombre VARCHAR(150) NOT NULL,
    direccion VARCHAR(300),
    notas VARCHAR(300),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_lugares_centro ON lugares(centro_id);

-- Migra los valores de texto libre que ya existian en clases.lugar hacia el catalogo nuevo
INSERT INTO lugares (centro_id, nombre)
SELECT DISTINCT centro_id, lugar FROM clases WHERE lugar IS NOT NULL AND lugar <> '';

ALTER TABLE clases ADD COLUMN lugar_id BIGINT REFERENCES lugares(id);

UPDATE clases c
SET lugar_id = l.id
FROM lugares l
WHERE l.centro_id = c.centro_id AND l.nombre = c.lugar;

ALTER TABLE clases DROP COLUMN lugar;
