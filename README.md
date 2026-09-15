# NexoraSport — Backend

API REST (Spring Boot 3 / Java 17) para la gestion de centros deportivos multi-disciplina
(box, crossfit, pesas, cardio, alberca, artes marciales, tienda, y cualquier disciplina que
el administrador cree despues). Ver el analisis de arquitectura en el documento publicado
para el contexto completo de por que esta estructurado asi.

## Requisitos

- Java 17
- Maven (o usa el wrapper si lo agregas)
- PostgreSQL 14+

## Primer arranque

1. Crea la base de datos:
   ```sql
   CREATE DATABASE nexorasport;
   ```
2. Copia las variables de entorno relevantes (o exporta las que necesites; todas tienen
   default de desarrollo en `application.properties`):
   - `DB_URL`, `DB_USER`, `DB_PASSWORD`
   - `APP_JWT_SECRET` (cambia el valor por defecto antes de ir a produccion)
   - `APP_CORS_ORIGINS` (por defecto `http://localhost:5173`, el frontend corre en 5174 — ajusta si aplica)
   - `APP_SEED_ADMIN_EMAIL` / `APP_SEED_ADMIN_PASSWORD` (usuario SUPER_ADMIN inicial)
3. Ejecuta:
   ```
   mvn spring-boot:run
   ```
   Flyway crea el esquema automaticamente (`src/main/resources/db/migration`) y el
   `DataSeeder` crea el usuario SUPER_ADMIN si no existe.
4. Inicia sesion con `APP_SEED_ADMIN_EMAIL` / `APP_SEED_ADMIN_PASSWORD`, te pedira cambiar
   la contrasena, y desde ahi crea tu primer centro deportivo (`POST /api/centros` o desde
   el frontend en "Seleccionar centro").

## Estructura

```
controller/   endpoints REST (uno por recurso)
service/      logica de negocio, multi-tenant via TenantScope
repository/   Spring Data JPA
model/        entidades JPA + enums
dto/          contratos de entrada/salida (records)
security/     JWT, RBAC dinamico por secciones, TenantScope
exception/    manejo global de errores
config/       seguridad, seeding inicial
```

## Modulos implementados

Auth (JWT + refresh cookie), Usuarios/Roles (RBAC por secciones configurable), Centros
(multi-tenant), Disciplinas (catalogo configurable, no hardcodeado), Instructores, Alumnos,
Membresias y planes (con registro automatico del cobro en Caja), Clases/Horarios, Reservas
(con control de cupo), Asistencia, Inventario general (equipo + tienda, clasificable por
disciplina), Proveedores y Compras (actualiza inventario y caja al marcarse realizadas),
Caja (ingresos/egresos configurables), Dashboard, y un catalogo publico sin login por
`slug` de centro (`/api/public/centros/{slug}`) pensado para compartirse por link/QR.

## Pendiente para siguientes iteraciones

Ver la seccion "Funcionalidades recomendadas para versiones posteriores" del documento de
analisis: portal de alumno con login propio, pagos recurrentes por pasarela, reportes
avanzados de retencion, pruebas automatizadas, rate limiting, y (si se va a vender a
gimnasios independientes) una capa de "Organizacion" por encima de Centro.
