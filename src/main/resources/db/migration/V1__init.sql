-- NexoraSport - esquema inicial
-- Convencion: toda tabla de negocio cuelga de centros.id (multi-tenant), salvo
-- roles/usuarios de plataforma (centro_id NULL = SUPER_ADMIN).

CREATE TABLE centros (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    slug_publico VARCHAR(80) UNIQUE,
    catalogo_publico_activo BOOLEAN NOT NULL DEFAULT FALSE,
    color_primario VARCHAR(9) DEFAULT '#1c6690',
    logo_url VARCHAR(300),
    telefono VARCHAR(30),
    email_contacto VARCHAR(150),
    direccion VARCHAR(250),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    centro_id BIGINT REFERENCES centros(id),
    nombre VARCHAR(60) NOT NULL,
    es_sistema BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (centro_id, nombre)
);

CREATE TABLE role_secciones (
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    seccion VARCHAR(40) NOT NULL,
    PRIMARY KEY (role_id, seccion)
);

CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    centro_id BIGINT REFERENCES centros(id),
    role_id BIGINT NOT NULL REFERENCES roles(id),
    nombre VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE
);

-- ---------------------------------------------------------------------------
CREATE TABLE disciplinas (
    id BIGSERIAL PRIMARY KEY,
    centro_id BIGINT NOT NULL REFERENCES centros(id),
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500),
    icono VARCHAR(10),
    color VARCHAR(9),
    modalidad VARCHAR(20) NOT NULL DEFAULT 'CLASES', -- CLASES | ACCESO_LIBRE
    limite_alumnos INTEGER,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (centro_id, nombre)
);

CREATE TABLE instructores (
    id BIGSERIAL PRIMARY KEY,
    centro_id BIGINT NOT NULL REFERENCES centros(id),
    nombre VARCHAR(150) NOT NULL,
    telefono VARCHAR(30),
    email VARCHAR(150),
    especialidad VARCHAR(200),
    foto_url VARCHAR(300),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE instructor_disciplinas (
    instructor_id BIGINT NOT NULL REFERENCES instructores(id) ON DELETE CASCADE,
    disciplina_id BIGINT NOT NULL REFERENCES disciplinas(id) ON DELETE CASCADE,
    PRIMARY KEY (instructor_id, disciplina_id)
);

CREATE TABLE alumnos (
    id BIGSERIAL PRIMARY KEY,
    centro_id BIGINT NOT NULL REFERENCES centros(id),
    nombre VARCHAR(150) NOT NULL,
    fecha_nacimiento DATE,
    telefono VARCHAR(30),
    email VARCHAR(150),
    contacto_emergencia_nombre VARCHAR(150),
    contacto_emergencia_telefono VARCHAR(30),
    foto_url VARCHAR(300),
    observaciones VARCHAR(1000),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO', -- ACTIVO | INACTIVO | SUSPENDIDO
    fecha_ingreso DATE NOT NULL DEFAULT CURRENT_DATE,
    deleted_at TIMESTAMP
);

CREATE TABLE alumno_disciplinas (
    alumno_id BIGINT NOT NULL REFERENCES alumnos(id) ON DELETE CASCADE,
    disciplina_id BIGINT NOT NULL REFERENCES disciplinas(id) ON DELETE CASCADE,
    PRIMARY KEY (alumno_id, disciplina_id)
);

-- ---------------------------------------------------------------------------
CREATE TABLE membresia_planes (
    id BIGSERIAL PRIMARY KEY,
    centro_id BIGINT NOT NULL REFERENCES centros(id),
    nombre VARCHAR(100) NOT NULL,
    tipo_periodo VARCHAR(20) NOT NULL, -- MENSUAL|SEMANAL|QUINCENAL|POR_CLASES|PERSONALIZADO
    duracion_dias INTEGER,
    numero_clases_incluidas INTEGER,
    precio NUMERIC(10,2) NOT NULL,
    multidisciplina BOOLEAN NOT NULL DEFAULT FALSE,
    acceso_completo BOOLEAN NOT NULL DEFAULT FALSE,
    limite_alumnos INTEGER,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE membresia_plan_disciplinas (
    plan_id BIGINT NOT NULL REFERENCES membresia_planes(id) ON DELETE CASCADE,
    disciplina_id BIGINT NOT NULL REFERENCES disciplinas(id) ON DELETE CASCADE,
    PRIMARY KEY (plan_id, disciplina_id)
);

CREATE TABLE membresias (
    id BIGSERIAL PRIMARY KEY,
    centro_id BIGINT NOT NULL REFERENCES centros(id),
    alumno_id BIGINT NOT NULL REFERENCES alumnos(id),
    plan_id BIGINT NOT NULL REFERENCES membresia_planes(id),
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    clases_restantes INTEGER,
    precio_final NUMERIC(10,2) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA', -- ACTIVA|VENCIDA|CANCELADA
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
CREATE TABLE clases (
    id BIGSERIAL PRIMARY KEY,
    centro_id BIGINT NOT NULL REFERENCES centros(id),
    disciplina_id BIGINT NOT NULL REFERENCES disciplinas(id),
    instructor_id BIGINT REFERENCES instructores(id),
    dia_semana VARCHAR(12) NOT NULL, -- LUNES..DOMINGO
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    lugar VARCHAR(100),
    capacidad_maxima INTEGER NOT NULL DEFAULT 20,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE reservas (
    id BIGSERIAL PRIMARY KEY,
    centro_id BIGINT NOT NULL REFERENCES centros(id),
    clase_id BIGINT NOT NULL REFERENCES clases(id),
    alumno_id BIGINT NOT NULL REFERENCES alumnos(id),
    fecha DATE NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'RESERVADA', -- RESERVADA|ASISTIO|CANCELADA|NO_ASISTIO
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (clase_id, alumno_id, fecha)
);

CREATE TABLE asistencias (
    id BIGSERIAL PRIMARY KEY,
    centro_id BIGINT NOT NULL REFERENCES centros(id),
    alumno_id BIGINT NOT NULL REFERENCES alumnos(id),
    disciplina_id BIGINT REFERENCES disciplinas(id),
    clase_id BIGINT REFERENCES clases(id),
    fecha DATE NOT NULL DEFAULT CURRENT_DATE,
    hora TIME NOT NULL DEFAULT CURRENT_TIME,
    registrado_por BIGINT REFERENCES usuarios(id)
);

-- ---------------------------------------------------------------------------
CREATE TABLE categorias_inventario (
    id BIGSERIAL PRIMARY KEY,
    centro_id BIGINT NOT NULL REFERENCES centros(id),
    nombre VARCHAR(100) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (centro_id, nombre)
);

CREATE TABLE articulos_inventario (
    id BIGSERIAL PRIMARY KEY,
    centro_id BIGINT NOT NULL REFERENCES centros(id),
    categoria_id BIGINT REFERENCES categorias_inventario(id),
    nombre VARCHAR(150) NOT NULL,
    tipo VARCHAR(20) NOT NULL DEFAULT 'EQUIPO', -- EQUIPO|ROPA|CONSUMIBLE|OTRO
    codigo_barras VARCHAR(80),
    stock INTEGER NOT NULL DEFAULT 0,
    stock_minimo INTEGER NOT NULL DEFAULT 0,
    costo NUMERIC(10,2),
    precio_venta NUMERIC(10,2),
    vendible BOOLEAN NOT NULL DEFAULT FALSE,
    imagen_url VARCHAR(300),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP,
    UNIQUE (centro_id, codigo_barras)
);

CREATE TABLE articulo_disciplinas (
    articulo_id BIGINT NOT NULL REFERENCES articulos_inventario(id) ON DELETE CASCADE,
    disciplina_id BIGINT NOT NULL REFERENCES disciplinas(id) ON DELETE CASCADE,
    PRIMARY KEY (articulo_id, disciplina_id)
);

CREATE TABLE movimientos_inventario (
    id BIGSERIAL PRIMARY KEY,
    articulo_id BIGINT NOT NULL REFERENCES articulos_inventario(id),
    tipo VARCHAR(20) NOT NULL, -- ENTRADA|SALIDA|AJUSTE|COMPRA
    stock_anterior INTEGER NOT NULL,
    stock_nuevo INTEGER NOT NULL,
    razon VARCHAR(300),
    registrado_por BIGINT REFERENCES usuarios(id),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
CREATE TABLE proveedores (
    id BIGSERIAL PRIMARY KEY,
    centro_id BIGINT NOT NULL REFERENCES centros(id),
    nombre VARCHAR(150) NOT NULL,
    contacto VARCHAR(150),
    telefono VARCHAR(30),
    email VARCHAR(150),
    notas VARCHAR(500),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE compras (
    id BIGSERIAL PRIMARY KEY,
    centro_id BIGINT NOT NULL REFERENCES centros(id),
    proveedor_id BIGINT NOT NULL REFERENCES proveedores(id),
    estado VARCHAR(20) NOT NULL DEFAULT 'PLANEADA', -- PLANEADA|PENDIENTE|REALIZADA|CANCELADA
    fecha_planeada DATE,
    fecha_realizada DATE,
    total NUMERIC(10,2) NOT NULL DEFAULT 0,
    notas VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE compra_items (
    id BIGSERIAL PRIMARY KEY,
    compra_id BIGINT NOT NULL REFERENCES compras(id) ON DELETE CASCADE,
    articulo_id BIGINT REFERENCES articulos_inventario(id),
    descripcion VARCHAR(200) NOT NULL,
    cantidad INTEGER NOT NULL,
    costo_unitario NUMERIC(10,2) NOT NULL
);

-- ---------------------------------------------------------------------------
CREATE TABLE categorias_movimiento (
    id BIGSERIAL PRIMARY KEY,
    centro_id BIGINT NOT NULL REFERENCES centros(id),
    nombre VARCHAR(100) NOT NULL,
    tipo VARCHAR(10) NOT NULL, -- INGRESO|EGRESO
    es_sistema BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (centro_id, nombre, tipo)
);

CREATE TABLE movimientos_financieros (
    id BIGSERIAL PRIMARY KEY,
    centro_id BIGINT NOT NULL REFERENCES centros(id),
    categoria_id BIGINT NOT NULL REFERENCES categorias_movimiento(id),
    tipo VARCHAR(10) NOT NULL, -- INGRESO|EGRESO
    monto NUMERIC(10,2) NOT NULL,
    metodo_pago VARCHAR(20) NOT NULL DEFAULT 'EFECTIVO', -- EFECTIVO|TARJETA|TRANSFERENCIA|OTRO
    descripcion VARCHAR(300),
    fecha DATE NOT NULL DEFAULT CURRENT_DATE,
    alumno_id BIGINT REFERENCES alumnos(id),
    membresia_id BIGINT REFERENCES membresias(id),
    proveedor_id BIGINT REFERENCES proveedores(id),
    compra_id BIGINT REFERENCES compras(id),
    registrado_por BIGINT REFERENCES usuarios(id),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_usuarios_centro ON usuarios(centro_id);
CREATE INDEX idx_alumnos_centro ON alumnos(centro_id);
CREATE INDEX idx_membresias_alumno ON membresias(alumno_id);
CREATE INDEX idx_membresias_centro_estado ON membresias(centro_id, estado);
CREATE INDEX idx_clases_centro ON clases(centro_id);
CREATE INDEX idx_reservas_clase_fecha ON reservas(clase_id, fecha);
CREATE INDEX idx_articulos_centro ON articulos_inventario(centro_id);
CREATE INDEX idx_movfin_centro_fecha ON movimientos_financieros(centro_id, fecha);
CREATE INDEX idx_compras_centro ON compras(centro_id);
