# NexoraSport

ERP/SaaS para gimnasios y centros deportivos multi-disciplina (box, crossfit, pesas,
alberca, artes marciales, y cualquier disciplina que el administrador cree despues).
Multi-tenant: cada **Centro** es un gimnasio independiente con su propio staff, roles,
alumnos, catalogo e inventario.

Este repo es el backend (Spring Boot 3 / Java 17). El frontend (React 18 / Vite /
Tailwind) vive en `NexoraSport-Frontend`, en la carpeta hermana.

## Stack

- **Backend**: Spring Boot 3.2.5, Java 17, Spring Data JPA (Hibernate), Spring Security
  (JWT + refresh cookie), PostgreSQL.
- **Frontend**: React 18, Vite, Tailwind, React Router, axios, Recharts.
- **Base de datos**: sin Flyway ni ninguna otra herramienta de migraciones —
  `spring.jpa.hibernate.ddl-auto=update` (Hibernate crea/altera tablas solo al
  arrancar). Ver `init.sql` para el detalle completo y la convencion a seguir en el
  proximo cambio de esquema que ddl-auto=update no pueda aplicar solo.
- **Extras**: OpenPDF (tickets/QR en PDF), ZXing (codigos QR), QZ Tray (impresion
  termica ESC/POS desde el navegador).

## Primer arranque

1. Crea la base de datos (vacia):
   ```sql
   CREATE DATABASE nexorasport;
   ```
   Opcionalmente corre `init.sql` (`psql -U postgres -h localhost -p 5433 -d nexorasport -f init.sql`)
   como referencia/bootstrap explicito; si no lo corres, Hibernate crea el mismo
   esquema solo al arrancar la app.
2. Variables de entorno (ver tabla completa abajo; todas tienen default de
   desarrollo en `application.properties`, asi que el arranque local funciona sin
   configurar nada salvo que quieras correo real o cambiar la base de datos).
3. `mvn spring-boot:run`. `DataSeeder` crea el rol y usuario `SUPER_ADMIN` si no
   existen.
4. Inicia sesion con el super admin, cambia la contrasena cuando se pida, y crea tu
   primer Centro (`POST /api/centros` o desde "Seleccionar centro" en el frontend).
   Al crear el Centro se siembran solos sus roles de sistema (Dueno/Administrador/
   Recepcion/Entrenador), sus categorias de movimiento de Caja, y su sucursal por
   defecto.

## Variables de entorno

Ninguna vive en el repo: todas se leen de variables de entorno reales del sistema
(`${VAR:default}` en `application.properties`), nunca de un archivo `.env` cargado
automaticamente — Spring Boot no trae esa capacidad de fabrica. `.env.example` (en la
raiz de este repo) las lista como referencia para copiar/pegar al exportarlas o al
configurarlas en el panel de tu proveedor de hosting; `.gitignore` ya ignora cualquier
`.env` real que crees localmente.

Para desarrollo local no necesitas configurar nada: todas tienen un default seguro
para localhost. Antes de desplegar a produccion, como minimo cambia `APP_JWT_SECRET`
y `APP_SEED_ADMIN_PASSWORD` (los defaults dicen literalmente "dev-only"/"CambiaEsta").

| Variable | Default (dev) | Para que sirve |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/nexorasport` | Cadena de conexion JDBC a Postgres |
| `DB_USER` | `postgres` | Usuario de la base de datos |
| `DB_PASSWORD` | `root123` | Password de la base de datos |
| `APP_JWT_SECRET` | `dev-only-secret-...` | Firma de los access tokens JWT — **cambiar en produccion** |
| `APP_JWT_ACCESS_MIN` | `30` | Minutos de vigencia del access token |
| `APP_JWT_REFRESH_HOURS` | `8` | Horas de vigencia del refresh token (cookie httpOnly) |
| `APP_COOKIE_SECURE` | `false` | `true` en produccion (HTTPS) para la cookie de refresh |
| `APP_COOKIE_SAMESITE` | `Lax` | Politica SameSite de la cookie de refresh |
| `APP_CORS_ORIGINS` | `http://localhost:5174` | Origen(es) permitidos para CORS (la URL del frontend) |
| `APP_UPLOADS_DIR` | `uploads` | Carpeta donde se guardan logos/imagenes/comprobantes subidos |
| `APP_MAIL_HOST` | `smtp.gmail.com` | Host SMTP para correo (reset de password, notificaciones) |
| `APP_MAIL_PORT` | `587` | Puerto SMTP |
| `APP_MAIL_USERNAME` | *(vacio)* | Cuenta SMTP — sin esto el envio de correo falla silenciosamente |
| `APP_MAIL_PASSWORD` | *(vacio)* | Password/App Password de la cuenta SMTP (nunca la de la cuenta si es Gmail, usa un App Password) |
| `APP_QZ_CERT_PATH` | `qz-keys/digital-certificate.txt` | Certificado para impresion termica via QZ Tray (carpeta ya ignorada por git) |
| `APP_QZ_KEY_PATH` | `qz-keys/private-key.pem` | Llave privada para QZ Tray (idem) |
| `APP_FRONTEND_URL` | `http://localhost:5174` | URL del frontend, usada en los links de los correos |
| `APP_SEED_ADMIN_EMAIL` | `admin@nexorasport.com` | Correo del `SUPER_ADMIN` que crea `DataSeeder` al primer arranque |
| `APP_SEED_ADMIN_PASSWORD` | `CambiaEsta123` | Password inicial del `SUPER_ADMIN` — **cambiar en produccion**, y de todas formas el sistema obliga a cambiarla en el primer login |
| `PORT` | `8081` | Puerto HTTP del backend |

## Estructura (backend)

```
controller/   endpoints REST (uno por recurso)
service/      logica de negocio, multi-tenant via TenantScope
repository/   Spring Data JPA
model/        entidades JPA + enums
dto/          contratos de entrada/salida (records)
security/     JWT, RBAC dinamico por secciones, TenantScope
exception/    manejo global de errores
config/       seguridad, seeding inicial (DataSeeder)
```

## Modulos implementados

- **Auth**: JWT (access corto) + refresh token opaco en cookie httpOnly, cambio de
  contrasena obligatorio para altas nuevas, recuperacion por correo.
- **Usuarios y Roles**: RBAC por secciones configurable por rol (no hardcodeado),
  `SUPER_ADMIN` global (sin `Centro`) + roles de sistema por Centro.
- **Centros**: frontera de tenant; catalogo/apartados publicos vía `slug` unico,
  branding (logo, color primario), limites de descuento configurables.
- **Sucursales / Instalaciones / Disciplinas**: jerarquia completa Sucursal →
  Instalacion (`Lugar`, con capacidad y disciplinas compatibles) → Disciplina
  (catalogo libre, cada una marca si `requiereInstalacion`).
- **Instructores**: telefono con lada (`PhoneInput`), disciplinas que imparte
  (obligatorio al menos una), galeria de diplomas/titulos por disciplina (valida que
  el instructor de verdad imparta esa disciplina antes de aceptar el archivo).
- **Clases y horarios**: valida cupo vs. capacidad de la instalacion, compatibilidad
  instalacion↔disciplina↔sucursal, **choque de horario del instructor** (no puede
  tener dos clases que se traslapen) y el combo de instructor en el formulario solo
  muestra a quienes imparten la disciplina elegida.
- **Reservas** (inscripcion de alumnos a clases): control de cupo por fecha y
  **choque de horario del alumno** (no puede inscribirse a dos clases que se
  traslapen el mismo dia).
- **Alumnos**: telefono normalizado (lada + 10 digitos), disciplinas de interes,
  historial de asistencia y membresias.
- **Membresias y planes**: alta con registro automatico del cobro en Caja; ver
  "Pendiente" abajo para lo que falta de este modulo.
- **Asistencia**: registro de entrada por alumno/clase.
- **Inventario**: articulos de equipo/tienda clasificables por disciplina, galeria
  multi-imagen (portada + secundarias, auto-promocion al borrar la portada),
  ajuste de stock (bloqueado en negativo salvo Dueno/Administrador), busqueda por
  codigo de barras.
- **Proveedores y Compras**: al marcarse "realizada" una compra, incrementa
  inventario y registra el egreso en Caja.
- **Caja**: ingresos/egresos con categorias configurables por Centro.
- **Tienda / Punto de venta**: escaneo/seleccion de codigo de barras, venta fisica
  con descuento automatico de inventario, **cortes de caja por turno** (un corte
  abierto por cajero, limite de 1/dia para no-admins), ticket fisico/digital/ambos
  (impresion termica ESC/POS vía QZ Tray + PDF con QR), ventas y tickets persistidos
  permanentemente.
- **Apartados** (layaway publico): tienda publica sin login (`/apartar/:slug`) donde
  un cliente solicita apartar productos; el stock se reserva solo al **confirmar**
  (no al solicitar, para no bloquear inventario con solicitudes falsas), ciclo
  completo `PENDIENTE → ACTIVO → COMPLETADO/CANCELADO/VENCIDO` con vencimiento
  automatico (`ApartadoExpiryJob`, cada 5 min), notificacion a staff, PDF+QR
  promocional descargable.
- **Catalogo publico**: escaparate sin login por `slug` de Centro
  (`/api/public/centros/{slug}`), pensado para compartirse por link/QR.
- **Dashboard**: ingresos/egresos/balance del mes.
- **Reportes**: hoy es una vista ligera que reusa Dashboard + movimientos de Caja
  agrupados por categoria (grafica de barras). No es un modulo de analitica propio
  todavia — ver "Pendiente".

## Pendiente / roadmap

Ordenado por lo mas cercano a completarse primero:

1. **Membresias y cobranza (Fase 1, diseño ya aprobado, sin implementar)**: hoy
   `EstadoMembresia` solo tiene `ACTIVA/VENCIDA/CANCELADA` y no hay pagos parciales.
   Falta: estado `SUSPENDIDA` (manual, con `motivo`, solo Dueno/Administrador),
   pagos parciales/abonos con saldo pendiente, renovacion contigua desde el dia
   siguiente al vencimiento anterior con re-seleccion de disciplina cuando aplique.
2. **Reportes avanzados**: retencion de alumnos, top disciplinas/productos,
   tendencia de asistencia, rentabilidad por compra — hoy no existe un
   `ReporteController` propio, solo el combo Dashboard+Caja.
3. **Pruebas automatizadas**: no hay ningun test (`src/test` vacio en ambos repos).
4. **Portal de alumno con login propio**: autoservicio para ver su membresia,
   reservar clases y ver su historial, sin depender de que el staff lo capture.
5. **Pagos recurrentes por pasarela** (Stripe/Conekta/MercadoPago): hoy todo cobro
   de membresia se registra manualmente en Caja.
6. **Notificaciones proactivas**: `MailService` hoy solo cubre reset de password y
   envio de tickets; falta recordatorio de vencimiento de membresia, confirmacion
   de clase, etc.
7. **Migraciones formales**: se documento `init.sql` (bootstrap/referencia), pero el
   proximo cambio de esquema que `ddl-auto=update` no pueda aplicar solo (ej. una
   columna `NOT NULL` nueva en una tabla con datos) sigue requiriendo un
   `migration_<nombre>.sql` manual — no hay una herramienta que lo automatice.
8. **Rate limiting / hardening** para produccion (login, recuperacion de password,
   endpoints publicos de catalogo/apartados).
9. **Capa de "Organizacion" por encima de Centro**: si se va a vender a un mismo
   dueño con varios gimnasios bajo una sola cuenta/facturacion consolidada (hoy
   `TenantScope` soporta que un `SUPER_ADMIN` vea varios Centros, pero no hay un
   agrupador de facturacion/reportes consolidados entre ellos).
