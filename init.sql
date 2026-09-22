-- ============================================================
--  NexoraSport — Esquema PostgreSQL (documentacion/bootstrap)
--  DB: nexorasport  |  host: localhost:5433
--  Run: psql -U postgres -h localhost -p 5433 -d nexorasport -f init.sql
--
--  Este proyecto NO usa Flyway ni ninguna herramienta de migraciones: Hibernate
--  crea y altera las tablas solo al arrancar la app (spring.jpa.hibernate.ddl-auto=
--  update), igual que en DemoPV. Este archivo documenta el esquema REAL que ya
--  existe hoy en la BD viva (verificado con psql \d+ tabla por tabla, no derivado a
--  ciegas de las anotaciones JPA) y sirve como referencia/bootstrap para levantar
--  una BD nueva desde cero: Hibernate reconoce las tablas ya creadas aqui y no
--  vuelve a tocarlas, salvo que falte una columna/indice nuevo (ese es el caso en el
--  que se necesitaria un migration_*.sql suelto, igual que en DemoPV, para parches
--  que ddl-auto=update no puede aplicar solo, p.ej. una columna NOT NULL nueva en
--  una tabla que ya tiene datos).
--
--  A diferencia de DemoPV (tiendas <-> users), en este esquema NO se encontro
--  ninguna dependencia circular entre tablas: "centros" no tiene columnas de
--  auditoria (creado_por/actualizado_por) apuntando a "usuarios", asi que el orden
--  centros -> roles -> usuarios -> el resto se resuelve de forma lineal y TODAS las
--  foreign keys de abajo se declaran inline, sin necesidad de que Hibernate agregue
--  ninguna despues al arrancar.
--
--  flyway_schema_history es un residuo de cuando el proyecto SI usaba Flyway (ya
--  se elimino esa dependencia hace tiempo) — a proposito NO se incluye aqui.
--
--  Sembrado de datos: DataSeeder (CommandLineRunner) crea, cada vez que arranca la
--  app, el rol global SUPER_ADMIN (centro_id NULL, con TODAS las secciones) y un
--  primer usuario super admin (admin@nexorasport.com / password configurable via
--  app.seed.super-admin-*) si todavia no existen — es idempotente por su cuenta
--  (orElseGet / existsByEmail), asi que este script NO duplica ese INSERT. El resto
--  del contenido (roles Dueno/Administrador/Recepcion/Entrenador, la sucursal
--  "Sucursal Principal" y las categorias de movimiento de sistema) NO se siembra al
--  arrancar la app: se crea recien cuando alguien da de alta un Centro nuevo via API
--  (CentroService#crear encadena RolService#seedRolesPorDefecto,
--  CajaService#seedCategoriasPorDefecto y SucursalService#seedSucursalPorDefecto).
--  Por eso este script solo crea la estructura vacia de esas tablas: no hay ningun
--  INSERT semilla indispensable que falte para que la app arranque sobre una BD
--  nueva.
-- ============================================================

-- Centro deportivo/gimnasio: la frontera de tenant (multi-tenant) de todo el
-- sistema; el resto de las tablas de negocio cuelga de centro_id. slug_publico es
-- el URL amigable (unico) del catalogo/apartados publicos; catalogo_publico_activo
-- y apartados_activo son flags independientes para exhibir el catalogo y aceptar
-- apartados desde esa vitrina sin login del cliente.
CREATE TABLE IF NOT EXISTS centros (
    id                                      BIGSERIAL PRIMARY KEY,
    nombre                                  VARCHAR(150) NOT NULL,
    slug_publico                            VARCHAR(80) UNIQUE,
    catalogo_publico_activo                 BOOLEAN NOT NULL DEFAULT FALSE,
    color_primario                          VARCHAR(9) DEFAULT '#1c6690',
    logo_url                                VARCHAR(300),
    telefono                                VARCHAR(30),
    email_contacto                          VARCHAR(150),
    direccion                               VARCHAR(250),
    activo                                  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                              TIMESTAMP NOT NULL DEFAULT NOW(),
    monto_maximo_descuento_apartado         NUMERIC(10,2),
    porcentaje_maximo_descuento_apartado    NUMERIC(5,2),
    apartados_activo                        BOOLEAN NOT NULL DEFAULT FALSE,
    horas_apartado_default                  INTEGER NOT NULL DEFAULT 24
);

-- Rol por centro (autorizacion fina via role_secciones); centro_id NULL identifica
-- al unico rol global de plataforma, SUPER_ADMIN. El paquete de roles de sistema de
-- un centro nuevo (Dueno/Administrador/Recepcion/Entrenador) lo siembra
-- RolService#seedRolesPorDefecto al crearse el Centro via API — este script solo
-- crea la tabla, vacia.
CREATE TABLE IF NOT EXISTS roles (
    id          BIGSERIAL PRIMARY KEY,
    centro_id   BIGINT REFERENCES centros(id),
    nombre      VARCHAR(60) NOT NULL,
    es_sistema  BOOLEAN NOT NULL DEFAULT FALSE,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (centro_id, nombre)
);

-- Usuario que inicia sesion: staff de un centro, o el SUPER_ADMIN de plataforma con
-- centro_id NULL. must_change_password se activa cuando un admin (o DataSeeder) da
-- de alta la cuenta con una contrasena temporal.
CREATE TABLE IF NOT EXISTS usuarios (
    id                      BIGSERIAL PRIMARY KEY,
    centro_id               BIGINT REFERENCES centros(id),
    role_id                 BIGINT NOT NULL REFERENCES roles(id),
    nombre                  VARCHAR(150) NOT NULL,
    email                   VARCHAR(150) NOT NULL UNIQUE,
    password_hash           VARCHAR(255) NOT NULL,
    must_change_password    BOOLEAN NOT NULL DEFAULT FALSE,
    activo                  BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at              TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Secciones (enum Seccion en Java: DASHBOARD, ALUMNOS, MEMBRESIAS, CAJA, COMPRAS,
-- DISCIPLINAS, INSTRUCTORES, CLASES, ASISTENCIA, INVENTARIO, TIENDA, REPORTES,
-- USUARIOS, ROLES, CENTROS) habilitadas por rol — controla que modulos ve cada
-- usuario. Sin CHECK en la BD real: Hibernate mapea este @ElementCollection de
-- enum como texto plano, sin restriccion a nivel de columna.
CREATE TABLE IF NOT EXISTS role_secciones (
    role_id     BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    seccion     VARCHAR(40) NOT NULL,
    PRIMARY KEY (role_id, seccion)
);

-- Sucursal fisica de un centro (un centro puede tener una o varias). La primera
-- ("Sucursal Principal") la siembra SucursalService#seedSucursalPorDefecto al crear
-- el Centro via API.
CREATE TABLE IF NOT EXISTS sucursales (
    id          BIGSERIAL PRIMARY KEY,
    centro_id   BIGINT NOT NULL REFERENCES centros(id),
    nombre      VARCHAR(150) NOT NULL,
    direccion   VARCHAR(300),
    notas       VARCHAR(300),
    activo      BOOLEAN NOT NULL DEFAULT TRUE
);

-- Instalacion/espacio fisico dentro de una sucursal (ej. "Alberca A", "Ring de
-- box"); puede limitarse a ciertas disciplinas via lugar_disciplinas.
CREATE TABLE IF NOT EXISTS lugares (
    id                  BIGSERIAL PRIMARY KEY,
    centro_id           BIGINT NOT NULL REFERENCES centros(id),
    sucursal_id         BIGINT NOT NULL REFERENCES sucursales(id),
    nombre              VARCHAR(150) NOT NULL,
    direccion           VARCHAR(300),
    notas               VARCHAR(300),
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    capacidad_maxima    INTEGER
);

-- Disciplina configurable (box, crossfit, pesas...) — a proposito NO es un enum de
-- codigo: el admin debe poder dar de alta disciplinas nuevas sin tocar el backend.
-- modalidad distingue si maneja horario de CLASES (con cupo/reserva) o es
-- ACCESO_LIBRE (solo asistencia libre, sin cupo ni horario fijo).
CREATE TABLE IF NOT EXISTS disciplinas (
    id                      BIGSERIAL PRIMARY KEY,
    centro_id               BIGINT NOT NULL REFERENCES centros(id),
    nombre                  VARCHAR(100) NOT NULL,
    descripcion             VARCHAR(500),
    icono                   VARCHAR(10),
    color                   VARCHAR(9),
    modalidad               VARCHAR(20) NOT NULL DEFAULT 'CLASES',
    limite_alumnos          INTEGER,
    activo                  BOOLEAN NOT NULL DEFAULT TRUE,
    requiere_instalacion    BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (centro_id, nombre)
);

-- N:N — en que lugares se puede impartir cada disciplina.
CREATE TABLE IF NOT EXISTS lugar_disciplinas (
    lugar_id        BIGINT NOT NULL REFERENCES lugares(id) ON DELETE CASCADE,
    disciplina_id   BIGINT NOT NULL REFERENCES disciplinas(id) ON DELETE CASCADE,
    PRIMARY KEY (lugar_id, disciplina_id)
);

CREATE TABLE IF NOT EXISTS instructores (
    id              BIGSERIAL PRIMARY KEY,
    centro_id       BIGINT NOT NULL REFERENCES centros(id),
    nombre          VARCHAR(150) NOT NULL,
    telefono        VARCHAR(30),
    email           VARCHAR(150),
    especialidad    VARCHAR(200),
    foto_url        VARCHAR(300),
    activo          BOOLEAN NOT NULL DEFAULT TRUE
);

-- N:N — que disciplinas puede impartir cada instructor.
CREATE TABLE IF NOT EXISTS instructor_disciplinas (
    instructor_id   BIGINT NOT NULL REFERENCES instructores(id) ON DELETE CASCADE,
    disciplina_id   BIGINT NOT NULL REFERENCES disciplinas(id) ON DELETE CASCADE,
    PRIMARY KEY (instructor_id, disciplina_id)
);

-- Diploma/titulo de un instructor, asociado a una de las disciplinas que imparte.
-- created_at es NOT NULL pero SIN default a nivel de BD (lo pone el field
-- initializer de LocalDateTime.now() en la entidad Java, no una columna DEFAULT).
CREATE TABLE IF NOT EXISTS documentos_instructor (
    id                  BIGSERIAL PRIMARY KEY,
    instructor_id       BIGINT NOT NULL REFERENCES instructores(id),
    disciplina_id       BIGINT NOT NULL REFERENCES disciplinas(id),
    ruta                VARCHAR(300) NOT NULL,
    nombre_original     VARCHAR(200),
    created_at          TIMESTAMP(6) NOT NULL
);

-- Horario recurrente semanal de una disciplina (ej. "Box lunes 18:00-19:00").
CREATE TABLE IF NOT EXISTS clases (
    id                  BIGSERIAL PRIMARY KEY,
    centro_id           BIGINT NOT NULL REFERENCES centros(id),
    disciplina_id       BIGINT NOT NULL REFERENCES disciplinas(id),
    instructor_id       BIGINT REFERENCES instructores(id),
    dia_semana          VARCHAR(12) NOT NULL,
    hora_inicio         TIME NOT NULL,
    hora_fin            TIME NOT NULL,
    capacidad_maxima    INTEGER NOT NULL DEFAULT 20,
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    lugar_id            BIGINT REFERENCES lugares(id),
    sucursal_id         BIGINT NOT NULL REFERENCES sucursales(id)
);

-- Alumno inscrito en el centro. deleted_at es borrado logico (nunca se elimina la
-- fila: hay membresias/asistencias/movimientos financieros que la referencian).
CREATE TABLE IF NOT EXISTS alumnos (
    id                              BIGSERIAL PRIMARY KEY,
    centro_id                       BIGINT NOT NULL REFERENCES centros(id),
    nombre                          VARCHAR(150) NOT NULL,
    fecha_nacimiento                DATE,
    telefono                        VARCHAR(30),
    email                           VARCHAR(150),
    contacto_emergencia_nombre      VARCHAR(150),
    contacto_emergencia_telefono    VARCHAR(30),
    foto_url                        VARCHAR(300),
    observaciones                   VARCHAR(1000),
    estado                          VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_ingreso                   DATE NOT NULL DEFAULT CURRENT_DATE,
    deleted_at                      TIMESTAMP
);

-- N:N — disciplinas en las que participa cada alumno.
CREATE TABLE IF NOT EXISTS alumno_disciplinas (
    alumno_id       BIGINT NOT NULL REFERENCES alumnos(id) ON DELETE CASCADE,
    disciplina_id   BIGINT NOT NULL REFERENCES disciplinas(id) ON DELETE CASCADE,
    PRIMARY KEY (alumno_id, disciplina_id)
);

-- Registro rapido de asistencia. disciplina_id/clase_id son nullables porque las
-- disciplinas de acceso libre (pesas, cardio) no requieren una Reserva ni una Clase
-- previa: solo se checa que el alumno entro.
CREATE TABLE IF NOT EXISTS asistencias (
    id              BIGSERIAL PRIMARY KEY,
    centro_id       BIGINT NOT NULL REFERENCES centros(id),
    alumno_id       BIGINT NOT NULL REFERENCES alumnos(id),
    disciplina_id   BIGINT REFERENCES disciplinas(id),
    clase_id        BIGINT REFERENCES clases(id),
    fecha           DATE NOT NULL DEFAULT CURRENT_DATE,
    hora            TIME NOT NULL DEFAULT CURRENT_TIME,
    registrado_por  BIGINT REFERENCES usuarios(id)
);

-- Inscripcion de un alumno a una ocurrencia concreta (fecha) de una Clase. La
-- unicidad (clase_id, alumno_id, fecha) evita que se reserve dos veces el mismo
-- cupo de la misma clase el mismo dia.
CREATE TABLE IF NOT EXISTS reservas (
    id          BIGSERIAL PRIMARY KEY,
    centro_id   BIGINT NOT NULL REFERENCES centros(id),
    clase_id    BIGINT NOT NULL REFERENCES clases(id),
    alumno_id   BIGINT NOT NULL REFERENCES alumnos(id),
    fecha       DATE NOT NULL,
    estado      VARCHAR(20) NOT NULL DEFAULT 'RESERVADA',
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (clase_id, alumno_id, fecha)
);

-- Plan de membresia vendible (mensualidad, paquete de clases, etc). duracion_dias y
-- numero_clases_incluidas son nullables porque no todo plan se vence por dias ni
-- limita por numero de clases (ver tipo_periodo/acceso_completo).
CREATE TABLE IF NOT EXISTS membresia_planes (
    id                          BIGSERIAL PRIMARY KEY,
    centro_id                   BIGINT NOT NULL REFERENCES centros(id),
    nombre                      VARCHAR(100) NOT NULL,
    tipo_periodo                VARCHAR(20) NOT NULL,
    duracion_dias               INTEGER,
    numero_clases_incluidas     INTEGER,
    precio                      NUMERIC(10,2) NOT NULL,
    multidisciplina             BOOLEAN NOT NULL DEFAULT FALSE,
    acceso_completo             BOOLEAN NOT NULL DEFAULT FALSE,
    limite_alumnos              INTEGER,
    activo                      BOOLEAN NOT NULL DEFAULT TRUE
);

-- N:N — a que disciplinas da acceso cada plan (irrelevante si acceso_completo=true).
CREATE TABLE IF NOT EXISTS membresia_plan_disciplinas (
    plan_id         BIGINT NOT NULL REFERENCES membresia_planes(id) ON DELETE CASCADE,
    disciplina_id   BIGINT NOT NULL REFERENCES disciplinas(id) ON DELETE CASCADE,
    PRIMARY KEY (plan_id, disciplina_id)
);

-- La suscripcion concreta de un alumno a un plan.
CREATE TABLE IF NOT EXISTS membresias (
    id                  BIGSERIAL PRIMARY KEY,
    centro_id           BIGINT NOT NULL REFERENCES centros(id),
    alumno_id           BIGINT NOT NULL REFERENCES alumnos(id),
    plan_id             BIGINT NOT NULL REFERENCES membresia_planes(id),
    fecha_inicio        DATE NOT NULL,
    fecha_fin           DATE NOT NULL,
    clases_restantes    INTEGER,
    precio_final        NUMERIC(10,2) NOT NULL,
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS categorias_inventario (
    id          BIGSERIAL PRIMARY KEY,
    centro_id   BIGINT NOT NULL REFERENCES centros(id),
    nombre      VARCHAR(100) NOT NULL,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (centro_id, nombre)
);

-- Inventario general unico: equipo/herramienta de disciplina y producto de tienda
-- (ropa) conviven aqui, distinguidos por tipo y vendible. codigo_barras es unico
-- POR CENTRO (no global): Postgres permite multiples NULL en un UNIQUE, asi que no
-- afecta a los articulos sin codigo. reservable/descuento_apartado_porcentaje
-- alimentan el catalogo publico de apartados (ver Apartado mas abajo).
CREATE TABLE IF NOT EXISTS articulos_inventario (
    id                              BIGSERIAL PRIMARY KEY,
    centro_id                       BIGINT NOT NULL REFERENCES centros(id),
    categoria_id                    BIGINT REFERENCES categorias_inventario(id),
    nombre                          VARCHAR(150) NOT NULL,
    tipo                            VARCHAR(20) NOT NULL DEFAULT 'EQUIPO',
    codigo_barras                   VARCHAR(80),
    stock                           INTEGER NOT NULL DEFAULT 0,
    stock_minimo                    INTEGER NOT NULL DEFAULT 0,
    costo                           NUMERIC(10,2),
    precio_venta                    NUMERIC(10,2),
    vendible                        BOOLEAN NOT NULL DEFAULT FALSE,
    imagen_url                      VARCHAR(300),
    activo                          BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at                      TIMESTAMP,
    descuento_apartado_porcentaje   NUMERIC(5,2),
    reservable                      BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (centro_id, codigo_barras)
);

-- N:N — en que disciplinas se clasifica cada articulo (ej. un kettlebell puede
-- servir para varias disciplinas a la vez).
CREATE TABLE IF NOT EXISTS articulo_disciplinas (
    articulo_id     BIGINT NOT NULL REFERENCES articulos_inventario(id) ON DELETE CASCADE,
    disciplina_id   BIGINT NOT NULL REFERENCES disciplinas(id) ON DELETE CASCADE,
    PRIMARY KEY (articulo_id, disciplina_id)
);

-- Galeria de fotos de un articulo (sobre todo para el catalogo publico de
-- apartados); es_principal marca la portada. created_at NOT NULL sin default en BD
-- (lo pone el field initializer en Java).
CREATE TABLE IF NOT EXISTS imagenes_articulo (
    id              BIGSERIAL PRIMARY KEY,
    articulo_id     BIGINT NOT NULL REFERENCES articulos_inventario(id),
    ruta            VARCHAR(300) NOT NULL,
    es_principal    BOOLEAN NOT NULL,
    orden           INTEGER NOT NULL,
    created_at      TIMESTAMP(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS proveedores (
    id          BIGSERIAL PRIMARY KEY,
    centro_id   BIGINT NOT NULL REFERENCES centros(id),
    nombre      VARCHAR(150) NOT NULL,
    contacto    VARCHAR(150),
    telefono    VARCHAR(30),
    email       VARCHAR(150),
    notas       VARCHAR(500),
    activo      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS compras (
    id                  BIGSERIAL PRIMARY KEY,
    centro_id           BIGINT NOT NULL REFERENCES centros(id),
    proveedor_id        BIGINT NOT NULL REFERENCES proveedores(id),
    estado              VARCHAR(20) NOT NULL DEFAULT 'PLANEADA',
    fecha_planeada      DATE,
    fecha_realizada     DATE,
    total               NUMERIC(10,2) NOT NULL DEFAULT 0,
    notas               VARCHAR(500),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Linea de una compra a proveedor. articulo_id es nullable: una compra puede
-- registrarse antes de que el articulo exista en el catalogo de inventario.
CREATE TABLE IF NOT EXISTS compra_items (
    id              BIGSERIAL PRIMARY KEY,
    compra_id       BIGINT NOT NULL REFERENCES compras(id) ON DELETE CASCADE,
    articulo_id     BIGINT REFERENCES articulos_inventario(id),
    descripcion     VARCHAR(200) NOT NULL,
    cantidad        INTEGER NOT NULL,
    costo_unitario  NUMERIC(10,2) NOT NULL
);

-- Bitacora inmutable de cada cambio de stock (entrada/salida/ajuste/compra), para
-- trazabilidad.
CREATE TABLE IF NOT EXISTS movimientos_inventario (
    id              BIGSERIAL PRIMARY KEY,
    articulo_id     BIGINT NOT NULL REFERENCES articulos_inventario(id),
    tipo            VARCHAR(20) NOT NULL,
    stock_anterior  INTEGER NOT NULL,
    stock_nuevo     INTEGER NOT NULL,
    razon           VARCHAR(300),
    registrado_por  BIGINT REFERENCES usuarios(id),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Categoria configurable de un movimiento de caja (ej. "Mensualidad", "Renta",
-- "Nomina"). Las categorias de sistema (es_sistema=true) las siembra
-- CajaService#seedCategoriasPorDefecto al crear el Centro via API; el admin puede
-- agregar mas propias del centro.
CREATE TABLE IF NOT EXISTS categorias_movimiento (
    id          BIGSERIAL PRIMARY KEY,
    centro_id   BIGINT NOT NULL REFERENCES centros(id),
    nombre      VARCHAR(100) NOT NULL,
    tipo        VARCHAR(10) NOT NULL,
    es_sistema  BOOLEAN NOT NULL DEFAULT FALSE,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (centro_id, nombre, tipo)
);

-- Libro de caja: cada ingreso (mensualidad, inscripcion, clase particular, venta de
-- producto...) y cada egreso (renta, nomina, mantenimiento...) es una fila aqui —
-- fuente de verdad del dashboard y los reportes. alumno_id/membresia_id/
-- proveedor_id/compra_id son nullables porque no todo movimiento nace de esas
-- entidades (ej. un gasto de mantenimiento no tiene alumno ni proveedor).
CREATE TABLE IF NOT EXISTS movimientos_financieros (
    id              BIGSERIAL PRIMARY KEY,
    centro_id       BIGINT NOT NULL REFERENCES centros(id),
    categoria_id    BIGINT NOT NULL REFERENCES categorias_movimiento(id),
    tipo            VARCHAR(10) NOT NULL,
    monto           NUMERIC(10,2) NOT NULL,
    metodo_pago     VARCHAR(20) NOT NULL DEFAULT 'EFECTIVO',
    descripcion     VARCHAR(300),
    fecha           DATE NOT NULL DEFAULT CURRENT_DATE,
    alumno_id       BIGINT REFERENCES alumnos(id),
    membresia_id    BIGINT REFERENCES membresias(id),
    proveedor_id    BIGINT REFERENCES proveedores(id),
    compra_id       BIGINT REFERENCES compras(id),
    registrado_por  BIGINT REFERENCES usuarios(id),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Turno de caja de un cajero: abre con un fondo inicial, acumula las ventas
-- registradas mientras esta abierto, y se cierra con un resumen. Varios cortes
-- pueden estar abiertos a la vez (uno por cajero). cerrado_por es NULL mientras
-- sigue abierto.
CREATE TABLE IF NOT EXISTS cortes_caja (
    id                      BIGSERIAL PRIMARY KEY,
    centro_id               BIGINT NOT NULL REFERENCES centros(id),
    usuario_id              BIGINT NOT NULL REFERENCES usuarios(id),
    cerrado_por             BIGINT REFERENCES usuarios(id),
    monto_inicial           NUMERIC(12,2) NOT NULL DEFAULT 0,
    monto_final             NUMERIC(12,2),
    gastos                  NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_ventas            NUMERIC(12,2) NOT NULL DEFAULT 0,
    ventas_efectivo         NUMERIC(12,2) NOT NULL DEFAULT 0,
    ventas_tarjeta          NUMERIC(12,2) NOT NULL DEFAULT 0,
    ventas_transferencia    NUMERIC(12,2) NOT NULL DEFAULT 0,
    ventas_otro             NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_transacciones     INTEGER NOT NULL DEFAULT 0,
    canceladas              INTEGER NOT NULL DEFAULT 0,
    total_cancelado         NUMERIC(12,2) NOT NULL DEFAULT 0,
    estado                  VARCHAR(10) NOT NULL DEFAULT 'ABIERTO',
    notas                   VARCHAR(300),
    abierto_en              TIMESTAMP NOT NULL DEFAULT NOW(),
    cerrado_en              TIMESTAMP
);

-- Venta de mostrador (punto de venta) del catalogo de Inventario/Tienda. Siempre
-- cuelga de un corte de caja abierto (corte_caja_id NOT NULL).
CREATE TABLE IF NOT EXISTS ventas (
    id              BIGSERIAL PRIMARY KEY,
    centro_id       BIGINT NOT NULL REFERENCES centros(id),
    usuario_id      BIGINT NOT NULL REFERENCES usuarios(id),
    corte_caja_id   BIGINT NOT NULL REFERENCES cortes_caja(id),
    cliente_nombre  VARCHAR(150),
    cliente_email   VARCHAR(150),
    subtotal        NUMERIC(12,2) NOT NULL DEFAULT 0,
    descuento       NUMERIC(12,2) NOT NULL DEFAULT 0,
    impuesto        NUMERIC(12,2) NOT NULL DEFAULT 0,
    total           NUMERIC(12,2) NOT NULL DEFAULT 0,
    monto_recibido  NUMERIC(12,2),
    cambio          NUMERIC(12,2),
    metodo_pago     VARCHAR(20) NOT NULL DEFAULT 'EFECTIVO',
    tipo_ticket     VARCHAR(10) NOT NULL DEFAULT 'NINGUNO',
    estado          VARCHAR(12) NOT NULL DEFAULT 'COMPLETADA',
    notas           VARCHAR(300),
    cancelada_por   BIGINT REFERENCES usuarios(id),
    cancelada_en    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Linea de una venta. articulo_nombre y precio_unitario quedan congelados al
-- momento de vender, para que renombrar o repreciar el articulo despues no
-- reescriba tickets historicos.
CREATE TABLE IF NOT EXISTS venta_items (
    id              BIGSERIAL PRIMARY KEY,
    venta_id        BIGINT NOT NULL REFERENCES ventas(id) ON DELETE CASCADE,
    articulo_id     BIGINT NOT NULL REFERENCES articulos_inventario(id),
    articulo_nombre VARCHAR(150) NOT NULL,
    precio_unitario NUMERIC(12,2) NOT NULL,
    cantidad        INTEGER NOT NULL,
    descuento       NUMERIC(12,2) NOT NULL DEFAULT 0,
    subtotal        NUMERIC(12,2) NOT NULL
);

-- Apartado (reserva/"layaway") de uno o varios articulos, solicitado desde la
-- vitrina publica de un centro (sin login del cliente) y gestionado despues por el
-- staff. A diferencia de una Venta, un apartado NO es una venta: es una promesa de
-- compra. El stock se descuenta hasta que un cajero lo confirma (PENDIENTE ->
-- ACTIVO), nunca al momento de la solicitud publica, para que una solicitud falsa o
-- de broma (no hay login ni pago que filtre) no bloquee inventario sin revision. Al
-- completarse (el cliente recoge y paga) se genera una Venta real SIN volver a
-- descontar stock (ya se descarto al confirmar). Timestamps (cancelado_en,
-- confirmado_en, expira_en, solicitado_en) son TIMESTAMP(6) sin default de BD: los
-- pone la app.
CREATE TABLE IF NOT EXISTS apartados (
    id                      BIGSERIAL PRIMARY KEY,
    centro_id               BIGINT NOT NULL REFERENCES centros(id),
    cliente_nombre          VARCHAR(150) NOT NULL,
    cliente_telefono        VARCHAR(30) NOT NULL,
    cliente_email           VARCHAR(150),
    notas                   VARCHAR(500),
    estado                  VARCHAR(12) NOT NULL
                                CHECK (estado IN ('PENDIENTE','ACTIVO','COMPLETADO','CANCELADO','VENCIDO')),
    subtotal                NUMERIC(12,2) NOT NULL,
    descuento               NUMERIC(12,2) NOT NULL,
    total                   NUMERIC(12,2) NOT NULL,
    horas_vigencia          INTEGER,
    solicitado_en           TIMESTAMP(6) NOT NULL,
    confirmado_en           TIMESTAMP(6),
    expira_en               TIMESTAMP(6),
    confirmado_por          BIGINT REFERENCES usuarios(id),
    completado_por          BIGINT REFERENCES usuarios(id),
    cancelado_por           BIGINT REFERENCES usuarios(id),
    cancelado_en            TIMESTAMP(6),
    motivo_cancelacion      VARCHAR(500),
    -- Id de la venta generada al completarse el apartado — SIN FK: en la entidad
    -- Java es un Long suelto (no una relacion @ManyToOne a Venta), asi que Hibernate
    -- nunca gestiona esta columna como foreign key; solo guarda el dato.
    venta_id                BIGINT
);

-- Linea de un apartado. articulo_nombre y precio_unitario quedan congelados al
-- momento de solicitarse, igual que en venta_items.
CREATE TABLE IF NOT EXISTS apartado_items (
    id                  BIGSERIAL PRIMARY KEY,
    apartado_id         BIGINT NOT NULL REFERENCES apartados(id),
    articulo_id         BIGINT NOT NULL REFERENCES articulos_inventario(id),
    articulo_nombre     VARCHAR(150) NOT NULL,
    precio_unitario     NUMERIC(12,2) NOT NULL,
    cantidad            INTEGER NOT NULL,
    descuento           NUMERIC(12,2) NOT NULL,
    subtotal            NUMERIC(12,2) NOT NULL
);

-- Token de un solo uso para "olvide mi contrasena" (ver flujo de auth en el
-- backend). used=true en cuanto se usa; la app invalida los anteriores sin usar del
-- mismo usuario al pedirse uno nuevo — este script solo crea la estructura.
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token       VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE
);

-- Refresh token de sesion: string opaco entregado al hacer login, revocable de
-- verdad (a diferencia del access token JWT, que solo se valida por firma).
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token       VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
--  Indices (los que ya existen hoy en la BD, ademas de los implicitos de cada
--  PRIMARY KEY/UNIQUE declarado arriba)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_alumnos_centro            ON alumnos(centro_id);
CREATE INDEX IF NOT EXISTS idx_articulos_centro           ON articulos_inventario(centro_id);
CREATE INDEX IF NOT EXISTS idx_clases_centro              ON clases(centro_id);
CREATE INDEX IF NOT EXISTS idx_compras_centro             ON compras(centro_id);
CREATE INDEX IF NOT EXISTS idx_cortes_caja_centro         ON cortes_caja(centro_id);
CREATE INDEX IF NOT EXISTS idx_cortes_caja_usuario_estado ON cortes_caja(usuario_id, estado);
CREATE INDEX IF NOT EXISTS idx_lugares_centro             ON lugares(centro_id);
CREATE INDEX IF NOT EXISTS idx_membresias_alumno          ON membresias(alumno_id);
CREATE INDEX IF NOT EXISTS idx_membresias_centro_estado   ON membresias(centro_id, estado);
CREATE INDEX IF NOT EXISTS idx_movfin_centro_fecha        ON movimientos_financieros(centro_id, fecha);
CREATE INDEX IF NOT EXISTS idx_reservas_clase_fecha       ON reservas(clase_id, fecha);
CREATE INDEX IF NOT EXISTS idx_sucursales_centro          ON sucursales(centro_id);
CREATE INDEX IF NOT EXISTS idx_usuarios_centro            ON usuarios(centro_id);
CREATE INDEX IF NOT EXISTS idx_venta_items_venta          ON venta_items(venta_id);
CREATE INDEX IF NOT EXISTS idx_ventas_centro              ON ventas(centro_id);
CREATE INDEX IF NOT EXISTS idx_ventas_corte_caja          ON ventas(corte_caja_id);
