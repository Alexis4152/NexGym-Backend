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
2. Variables de entorno relevantes (todas tienen default de desarrollo en
   `application.properties`): `DB_URL`/`DB_USER`/`DB_PASSWORD`, `APP_JWT_SECRET`,
   `APP_CORS_ORIGINS`, `APP_SEED_ADMIN_EMAIL`/`APP_SEED_ADMIN_PASSWORD`.
3. `mvn spring-boot:run`. `DataSeeder` crea el rol y usuario `SUPER_ADMIN` si no
   existen.
4. Inicia sesion con el super admin, cambia la contrasena cuando se pida, y crea tu
   primer Centro (`POST /api/centros` o desde "Seleccionar centro" en el frontend).
   Al crear el Centro se siembran solos sus roles de sistema (Dueno/Administrador/
   Recepcion/Entrenador), sus categorias de movimiento de Caja, y su sucursal por
   defecto.

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
