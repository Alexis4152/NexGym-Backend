package com.nexora.sport.service;

import java.util.Map;

/**
 * Plantillas de texto plano por tipo de evento (seccion 17 del encargo). A proposito NO
 * es un motor de plantillas: solo reemplazo de {@code {{variable}}} por texto, suficiente
 * para el MVP y facil de migrar a HTML/Thymeleaf despues sin tocar los llamadores (todos
 * pasan por {@link #render}).
 */
public final class NotificacionTemplates {

    private NotificacionTemplates() {}

    public static String render(String template, Map<String, String> vars) {
        String result = template;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            result = result.replace("{{" + e.getKey() + "}}", e.getValue() != null ? e.getValue() : "");
        }
        return result;
    }

    // ---- Membresias ----
    public static final String MEMBRESIA_NUEVA_TITULO = "Tu membresía fue activada";
    public static final String MEMBRESIA_NUEVA_MSG = "Hola {{alumno}}, tu membresía {{plan}} en {{centro}} ha sido activada. {{vigencia}}";

    public static final String MEMBRESIA_RENOVADA_TITULO = "Tu membresía fue renovada";
    public static final String MEMBRESIA_RENOVADA_MSG = "Hola {{alumno}}, tu membresía {{plan}} en {{centro}} fue renovada correctamente. {{vigencia}}";

    public static final String MEMBRESIA_PROXIMA_VENCER_TITULO = "Tu membresía vence pronto";
    public static final String MEMBRESIA_PROXIMA_VENCER_MSG = "Hola {{alumno}}, tu membresía {{plan}} en {{centro}} vence en {{dias}} día(s), el {{fecha}}.";

    public static final String MEMBRESIA_VENCE_HOY_TITULO = "Tu membresía vence hoy";
    public static final String MEMBRESIA_VENCE_HOY_MSG = "Hola {{alumno}}, tu membresía {{plan}} en {{centro}} vence hoy ({{fecha}}). Renuévala para no perder tu acceso.";

    public static final String MEMBRESIA_VENCIDA_TITULO = "Tu membresía venció";
    public static final String MEMBRESIA_VENCIDA_MSG = "Hola {{alumno}}, tu membresía {{plan}} en {{centro}} venció el {{fecha}}. Renuévala cuando gustes para reactivar tu acceso.";

    // ---- Pagos ----
    public static final String PAGO_REGISTRADO_TITULO = "Pago registrado";
    public static final String PAGO_REGISTRADO_MSG = "Hemos registrado tu pago de ${{monto}} en {{centro}} por tu membresía {{plan}}.";

    public static final String ABONO_REGISTRADO_TITULO = "Abono registrado";
    public static final String ABONO_REGISTRADO_MSG = "Hemos registrado un abono de ${{monto}} en {{centro}} por tu membresía {{plan}}. Saldo pendiente: ${{saldo}}.";

    public static final String PAGO_LIQUIDADO_TITULO = "Membresía liquidada";
    public static final String PAGO_LIQUIDADO_MSG = "Tu membresía {{plan}} en {{centro}} ha sido liquidada. ¡Gracias por tu pago!";

    // ---- Clases / reservas ----
    public static final String RESERVA_CONFIRMADA_TITULO = "Reserva confirmada";
    public static final String RESERVA_CONFIRMADA_MSG = "Tu lugar para {{disciplina}} ha sido reservado.\n{{sucursal}}{{lugar}}\n{{fecha}} {{hora}}";

    public static final String RESERVA_CANCELADA_TITULO = "Reserva cancelada";
    public static final String RESERVA_CANCELADA_MSG = "Tu reserva de {{disciplina}} del {{fecha}} {{hora}} fue cancelada.";

    public static final String CLASE_CANCELADA_TITULO = "Clase cancelada";
    public static final String CLASE_CANCELADA_MSG = "La clase de {{disciplina}} de las {{hora}} en {{centro}} fue cancelada. Tu reserva del {{fecha}} quedó cancelada.";

    public static final String CLASE_HORARIO_CAMBIADO_TITULO = "Cambio de horario de clase";
    public static final String CLASE_HORARIO_CAMBIADO_MSG = "La clase de {{disciplina}} cambió de horario: de las {{horaAnterior}} a las {{horaNueva}}. Tu reserva del {{fecha}} sigue vigente.";

    public static final String CLASE_INSTRUCTOR_CAMBIADO_TITULO = "Cambio de instructor";
    public static final String CLASE_INSTRUCTOR_CAMBIADO_MSG = "La clase de {{disciplina}} del {{fecha}} ahora será impartida por {{instructorNuevo}}.";

    public static final String CLASE_RECORDATORIO_TITULO = "Recordatorio de clase";
    public static final String CLASE_RECORDATORIO_MSG = "Tu clase de {{disciplina}} comienza el {{fecha}} a las {{hora}}.";

    // ---- Avisos administrativos (CENTRO_ADMIN, solo interno) ----
    public static final String ADMIN_MEMBRESIAS_POR_VENCER_TITULO = "Membresías por vencer esta semana";
    public static final String ADMIN_MEMBRESIAS_POR_VENCER_MSG = "{{cantidad}} membresía(s) vencen en los próximos 7 días.";

    public static final String ADMIN_ALUMNOS_SALDO_PENDIENTE_TITULO = "Alumnos con saldo pendiente";
    public static final String ADMIN_ALUMNOS_SALDO_PENDIENTE_MSG = "{{cantidad}} alumno(s) tienen saldo pendiente en su membresía.";

    public static final String ADMIN_CLASE_CUPO_LLENO_TITULO = "Clase con cupo lleno";
    public static final String ADMIN_CLASE_CUPO_LLENO_MSG = "La clase de {{disciplina}} de las {{hora}} del {{fecha}} alcanzó su cupo máximo ({{capacidad}}).";

    public static final String ADMIN_ALUMNO_RIESGO_ABANDONO_TITULO = "Alumno en riesgo de abandono";
    public static final String ADMIN_ALUMNO_RIESGO_ABANDONO_MSG = "{{alumno}} lleva {{dias}} día(s) sin asistir.";

    public static final String ADMIN_CLASE_CUPO_DISPONIBLE_TITULO = "Se liberó un lugar";
    public static final String ADMIN_CLASE_CUPO_DISPONIBLE_MSG = "La clase de {{disciplina}} de las {{hora}} del {{fecha}} tiene un lugar disponible de nuevo.";

    public static final String ADMIN_MEMBRESIA_NUEVA_TITULO = "Nueva membresía registrada";
    public static final String ADMIN_MEMBRESIA_NUEVA_MSG = "Se registró la membresía {{plan}} para {{alumno}}.";

    public static final String ADMIN_MEMBRESIA_AGOTADA_TITULO = "Membresía agotada";
    public static final String ADMIN_MEMBRESIA_AGOTADA_MSG = "{{alumno}} ya usó todas las clases de su membresía {{plan}}.";

    public static final String ADMIN_MEMBRESIA_CANCELADA_O_VENCIDA_TITULO = "Membresía cancelada o vencida";
    public static final String ADMIN_MEMBRESIA_CANCELADA_O_VENCIDA_MSG = "La membresía {{plan}} de {{alumno}} {{motivo}}.";

    public static final String ADMIN_PLAN_LLENO_TITULO = "Plan de membresía lleno";
    public static final String ADMIN_PLAN_LLENO_MSG = "El plan {{plan}} alcanzó su límite de {{limite}} alumno(s).";

    public static final String ADMIN_STOCK_BAJO_TITULO = "Stock bajo";
    public static final String ADMIN_STOCK_BAJO_MSG = "\"{{articulo}}\" tiene stock bajo: {{stock}} unidad(es) (mínimo {{minimo}}).";

    public static final String ADMIN_STOCK_AGOTADO_TITULO = "Producto agotado";
    public static final String ADMIN_STOCK_AGOTADO_MSG = "\"{{articulo}}\" se quedó sin stock (0 unidades).";

    public static final String ADMIN_STOCK_RECUPERADO_TITULO = "Producto con stock de nuevo";
    public static final String ADMIN_STOCK_RECUPERADO_MSG = "\"{{articulo}}\" ya tiene stock disponible de nuevo: {{stock}} unidad(es).";

    public static final String ADMIN_PRODUCTO_NUEVO_TITULO = "Nuevo producto agregado";
    public static final String ADMIN_PRODUCTO_NUEVO_MSG = "Se agregó \"{{articulo}}\" al inventario.";

    public static final String ADMIN_CLASE_NUEVA_TITULO = "Nueva clase agregada";
    public static final String ADMIN_CLASE_NUEVA_MSG = "Se agregó una clase de {{disciplina}} los {{dia}} a las {{hora}} en {{sucursal}}.";

    public static final String ADMIN_DISCIPLINA_NUEVA_TITULO = "Nueva disciplina agregada";
    public static final String ADMIN_DISCIPLINA_NUEVA_MSG = "Se agregó la disciplina \"{{disciplina}}\".";

    public static final String ADMIN_ALUMNO_NUEVO_TITULO = "Alumno registrado";
    public static final String ADMIN_ALUMNO_NUEVO_MSG = "Se registró un nuevo alumno: {{alumno}}.";

    public static final String EGRESO_PENDIENTE_APROBACION_TITULO = "Egreso pendiente de aprobación";
    public static final String EGRESO_PENDIENTE_APROBACION_MSG = "{{registrador}} registró un egreso de ${{monto}} ({{categoria}}) sin comprobante. Requiere tu aprobación.";

    // ---- Entrenador (destinatarioTipo=INSTRUCTOR) ----
    public static final String INSTRUCTOR_CLASE_ASIGNADA_TITULO = "Nueva clase asignada";
    public static final String INSTRUCTOR_CLASE_ASIGNADA_MSG = "Se te asignó la clase de {{disciplina}} los {{dia}} a las {{hora}} en {{sucursal}}.";

    public static final String INSTRUCTOR_CLASE_CANCELADA_TITULO = "Clase cancelada";
    public static final String INSTRUCTOR_CLASE_CANCELADA_MSG = "Tu clase de {{disciplina}} de las {{hora}} en {{sucursal}} fue cancelada.";

    public static final String INSTRUCTOR_ALUMNO_INSCRITO_TITULO = "Nuevo alumno en tu clase";
    public static final String INSTRUCTOR_ALUMNO_INSCRITO_MSG = "{{alumno}} se inscribió a tu clase de {{disciplina}} del {{fecha}}.";

    public static final String INSTRUCTOR_ALUMNO_REMOVIDO_TITULO = "Alumno removido de tu clase";
    public static final String INSTRUCTOR_ALUMNO_REMOVIDO_MSG = "{{alumno}} fue removido de tu clase de {{disciplina}} del {{fecha}}.";

    public static final String INSTRUCTOR_DISCIPLINA_ASIGNADA_TITULO = "Nueva disciplina asignada";
    public static final String INSTRUCTOR_DISCIPLINA_ASIGNADA_MSG = "Ahora estás asignado a la disciplina \"{{disciplina}}\".";

    public static final String INSTRUCTOR_SUCURSAL_CAMBIADA_TITULO = "Cambio de sucursal";
    public static final String INSTRUCTOR_SUCURSAL_CAMBIADA_MSG = "Tus sucursales asignadas cambiaron: {{sucursales}}.";
}
