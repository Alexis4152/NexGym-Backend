package com.nexora.sport.service;

import com.nexora.sport.dto.NotificacionDto;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.NotificacionEnvioRepository;
import com.nexora.sport.repository.NotificacionRepository;
import com.nexora.sport.security.TenantScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nexora.sport.service.NotificacionTemplates.*;

/**
 * Punto de entrada UNICO para generar notificaciones (seccion 5 del encargo: evento ->
 * notificacion -> canal -> destinatario -> estado de envio). Todo lo que dispara un aviso
 * (MembresiaService, PagoMembresiaService, ReservaService, ClaseService, los jobs
 * programados) llama a uno de los metodos "notificarXxx" de aqui, nunca arma la fila a mano.
 *
 * {@link #crear} NUNCA propaga excepciones (seccion 24: un fallo generando/enviando un
 * aviso jamas debe tumbar el pago/reserva/membresia que lo origino).
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionRepository notificacionRepository;
    private final NotificacionEnvioRepository notificacionEnvioRepository;
    private final NotificacionEmailDispatcher emailDispatcher;
    private final TenantScope tenantScope;

    public NotificacionService(NotificacionRepository notificacionRepository,
                                NotificacionEnvioRepository notificacionEnvioRepository,
                                NotificacionEmailDispatcher emailDispatcher, TenantScope tenantScope) {
        this.notificacionRepository = notificacionRepository;
        this.notificacionEnvioRepository = notificacionEnvioRepository;
        this.emailDispatcher = emailDispatcher;
        this.tenantScope = tenantScope;
    }

    // =====================================================================
    // Nucleo: crear + despachar. No propaga excepciones (seccion 24).
    // =====================================================================

    @Transactional
    public void crear(Centro centro, TipoNotificacion tipo, TipoDestinatario destinatarioTipo,
                       Alumno alumno, Instructor instructor, String titulo, String mensaje,
                       String entidadTipo, Long entidadId, String dedupeKey,
                       boolean categoriaActiva, String emailDestino) {
        try {
            if (!categoriaActiva) return;
            if (dedupeKey != null && notificacionRepository.existsByDedupeKey(dedupeKey)) return;

            Notificacion n = new Notificacion();
            n.setCentro(centro);
            n.setTipo(tipo);
            n.setDestinatarioTipo(destinatarioTipo);
            n.setAlumno(alumno);
            n.setInstructor(instructor);
            n.setTitulo(titulo);
            n.setMensaje(mensaje);
            n.setEntidadTipo(entidadTipo);
            n.setEntidadId(entidadId);
            n.setDedupeKey(dedupeKey);
            n.setInternoVisible(centro.isNotificacionesInternoActivo());
            n = notificacionRepository.save(n);

            if (centro.isNotificacionesEmailActivo() && emailDestino != null && !emailDestino.isBlank()) {
                NotificacionEnvio envio = new NotificacionEnvio();
                envio.setNotificacion(n);
                envio.setCanal(CanalNotificacion.EMAIL);
                envio.setDestino(emailDestino);
                envio = notificacionEnvioRepository.save(envio);
                emailDispatcher.despachar(envio.getId());
            }
        } catch (Exception e) {
            log.error("No se pudo generar la notificacion {} para el centro {}: {}",
                    tipo, centro != null ? centro.getId() : null, e.getMessage(), e);
        }
    }

    private static Map<String, String> mapOf(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    // =====================================================================
    // Membresias (eventos inmediatos: creacion/renovacion; llamados por MembresiaService)
    // =====================================================================

    public void notificarMembresiaNueva(Membresia m) {
        Centro centro = m.getCentro();
        Alumno alumno = m.getAlumno();
        String vigencia = m.getFechaFin() != null ? "Vigencia: " + m.getFechaInicio() + " a " + m.getFechaFin() + "."
                : "Inicia: " + m.getFechaInicio() + ".";
        Map<String, String> v = mapOf("alumno", alumno.getNombre(), "plan", m.getPlanNombreSnapshot(),
                "centro", centro.getNombre(), "vigencia", vigencia);
        crear(centro, TipoNotificacion.MEMBRESIA_NUEVA, TipoDestinatario.ALUMNO, alumno, null,
                MEMBRESIA_NUEVA_TITULO, render(MEMBRESIA_NUEVA_MSG, v),
                "MEMBRESIA", m.getId(), "MEMBRESIA_NUEVA:" + m.getId(),
                centro.isNotificacionesMembresiaActivo(), alumno.getEmail());
    }

    public void notificarMembresiaRenovada(Membresia m) {
        Centro centro = m.getCentro();
        Alumno alumno = m.getAlumno();
        String vigencia = m.getFechaFin() != null ? "Vigencia: " + m.getFechaInicio() + " a " + m.getFechaFin() + "."
                : "Inicia: " + m.getFechaInicio() + ".";
        Map<String, String> v = mapOf("alumno", alumno.getNombre(), "plan", m.getPlanNombreSnapshot(),
                "centro", centro.getNombre(), "vigencia", vigencia);
        crear(centro, TipoNotificacion.MEMBRESIA_RENOVADA, TipoDestinatario.ALUMNO, alumno, null,
                MEMBRESIA_RENOVADA_TITULO, render(MEMBRESIA_RENOVADA_MSG, v),
                "MEMBRESIA", m.getId(), "MEMBRESIA_RENOVADA:" + m.getId(),
                centro.isNotificacionesMembresiaActivo(), alumno.getEmail());
    }

    // =====================================================================
    // Vencimientos de membresia (evento temporal, llamado por NotificacionSchedulerService)
    // =====================================================================

    public void notificarMembresiaProximaAVencer(Membresia m, long dias, LocalDate hoy) {
        Centro centro = m.getCentro();
        Alumno alumno = m.getAlumno();
        Map<String, String> v = mapOf("alumno", alumno.getNombre(), "plan", m.getPlanNombreSnapshot(),
                "centro", centro.getNombre(), "dias", String.valueOf(dias), "fecha", String.valueOf(m.getFechaFin()));
        crear(centro, TipoNotificacion.MEMBRESIA_PROXIMA_VENCER, TipoDestinatario.ALUMNO, alumno, null,
                MEMBRESIA_PROXIMA_VENCER_TITULO, render(MEMBRESIA_PROXIMA_VENCER_MSG, v),
                "MEMBRESIA", m.getId(), "MEMBRESIA_VENC:" + m.getId() + ":" + m.getFechaFin() + ":" + dias,
                centro.isNotificacionesMembresiaActivo(), alumno.getEmail());
    }

    public void notificarMembresiaVenceHoy(Membresia m, LocalDate hoy) {
        Centro centro = m.getCentro();
        Alumno alumno = m.getAlumno();
        Map<String, String> v = mapOf("alumno", alumno.getNombre(), "plan", m.getPlanNombreSnapshot(),
                "centro", centro.getNombre(), "fecha", String.valueOf(m.getFechaFin()));
        crear(centro, TipoNotificacion.MEMBRESIA_VENCE_HOY, TipoDestinatario.ALUMNO, alumno, null,
                MEMBRESIA_VENCE_HOY_TITULO, render(MEMBRESIA_VENCE_HOY_MSG, v),
                "MEMBRESIA", m.getId(), "MEMBRESIA_VENC:" + m.getId() + ":" + m.getFechaFin() + ":0",
                centro.isNotificacionesMembresiaActivo(), alumno.getEmail());
    }

    public void notificarMembresiaVencida(Membresia m) {
        Centro centro = m.getCentro();
        Alumno alumno = m.getAlumno();
        Map<String, String> v = mapOf("alumno", alumno.getNombre(), "plan", m.getPlanNombreSnapshot(),
                "centro", centro.getNombre(), "fecha", String.valueOf(m.getFechaFin()));
        crear(centro, TipoNotificacion.MEMBRESIA_VENCIDA, TipoDestinatario.ALUMNO, alumno, null,
                MEMBRESIA_VENCIDA_TITULO, render(MEMBRESIA_VENCIDA_MSG, v),
                "MEMBRESIA", m.getId(), "MEMBRESIA_VENCIDA:" + m.getId(),
                centro.isNotificacionesMembresiaActivo(), alumno.getEmail());
    }

    // =====================================================================
    // Pagos (evento inmediato, llamado por PagoMembresiaService tras exito confirmado)
    // =====================================================================

    /**
     * esPrimerPago = era el primer pago VALIDO de la membresia (antes de guardar este).
     * saldoRestante = saldo ya recalculado DESPUES de guardar este pago.
     */
    public void notificarPago(Membresia m, PagoMembresia pago, boolean esPrimerPago, BigDecimal saldoRestante) {
        Centro centro = m.getCentro();
        Alumno alumno = m.getAlumno();
        String monto = pago.getMonto().setScale(2, java.math.RoundingMode.HALF_UP).toString();

        if (saldoRestante.signum() > 0) {
            Map<String, String> v = mapOf("monto", monto, "centro", centro.getNombre(), "plan", m.getPlanNombreSnapshot(),
                    "saldo", saldoRestante.setScale(2, java.math.RoundingMode.HALF_UP).toString());
            crear(centro, TipoNotificacion.ABONO_REGISTRADO, TipoDestinatario.ALUMNO, alumno, null,
                    ABONO_REGISTRADO_TITULO, render(ABONO_REGISTRADO_MSG, v),
                    "PAGO", pago.getId(), "PAGO:" + pago.getId(),
                    centro.isNotificacionesMembresiaActivo(), alumno.getEmail());
        } else if (esPrimerPago) {
            Map<String, String> v = mapOf("monto", monto, "centro", centro.getNombre(), "plan", m.getPlanNombreSnapshot());
            crear(centro, TipoNotificacion.PAGO_REGISTRADO, TipoDestinatario.ALUMNO, alumno, null,
                    PAGO_REGISTRADO_TITULO, render(PAGO_REGISTRADO_MSG, v),
                    "PAGO", pago.getId(), "PAGO:" + pago.getId(),
                    centro.isNotificacionesMembresiaActivo(), alumno.getEmail());
        } else {
            Map<String, String> v = mapOf("centro", centro.getNombre(), "plan", m.getPlanNombreSnapshot());
            crear(centro, TipoNotificacion.PAGO_LIQUIDADO, TipoDestinatario.ALUMNO, alumno, null,
                    PAGO_LIQUIDADO_TITULO, render(PAGO_LIQUIDADO_MSG, v),
                    "PAGO", pago.getId(), "PAGO:" + pago.getId(),
                    centro.isNotificacionesMembresiaActivo(), alumno.getEmail());
        }
    }

    // =====================================================================
    // Clases y reservas (eventos inmediatos, llamados por ReservaService/ClaseService)
    // =====================================================================

    public void notificarReservaConfirmada(Reserva r) {
        Centro centro = r.getCentro();
        Alumno alumno = r.getAlumno();
        Clase c = r.getClase();
        Map<String, String> v = mapOf("disciplina", c.getDisciplina().getNombre(),
                "sucursal", c.getSucursal().getNombre(),
                "lugar", c.getLugar() != null ? " · " + c.getLugar().getNombre() : "",
                "fecha", String.valueOf(r.getFecha()), "hora", String.valueOf(c.getHoraInicio()));
        crear(centro, TipoNotificacion.RESERVA_CONFIRMADA, TipoDestinatario.ALUMNO, alumno, null,
                RESERVA_CONFIRMADA_TITULO, render(RESERVA_CONFIRMADA_MSG, v),
                "RESERVA", r.getId(), "RESERVA_CONFIRMADA:" + r.getId(),
                centro.isNotificacionesClaseActivo(), alumno.getEmail());
    }

    public void notificarReservaCancelada(Reserva r) {
        Centro centro = r.getCentro();
        Alumno alumno = r.getAlumno();
        Clase c = r.getClase();
        Map<String, String> v = mapOf("disciplina", c.getDisciplina().getNombre(),
                "fecha", String.valueOf(r.getFecha()), "hora", String.valueOf(c.getHoraInicio()));
        crear(centro, TipoNotificacion.RESERVA_CANCELADA, TipoDestinatario.ALUMNO, alumno, null,
                RESERVA_CANCELADA_TITULO, render(RESERVA_CANCELADA_MSG, v),
                "RESERVA", r.getId(), "RESERVA_CANCELADA:" + r.getId(),
                centro.isNotificacionesClaseActivo(), alumno.getEmail());
    }

    public void notificarClaseCanceladaPorReserva(Reserva r) {
        Centro centro = r.getCentro();
        Alumno alumno = r.getAlumno();
        Clase c = r.getClase();
        Map<String, String> v = mapOf("disciplina", c.getDisciplina().getNombre(), "centro", centro.getNombre(),
                "hora", String.valueOf(c.getHoraInicio()), "fecha", String.valueOf(r.getFecha()));
        crear(centro, TipoNotificacion.CLASE_CANCELADA, TipoDestinatario.ALUMNO, alumno, null,
                CLASE_CANCELADA_TITULO, render(CLASE_CANCELADA_MSG, v),
                "RESERVA", r.getId(), "CLASE_CANCELADA:" + r.getId(),
                centro.isNotificacionesClaseActivo(), alumno.getEmail());
    }

    public void notificarClaseHorarioCambiado(Reserva r, String horaAnterior, String horaNueva) {
        Centro centro = r.getCentro();
        Alumno alumno = r.getAlumno();
        Clase c = r.getClase();
        Map<String, String> v = mapOf("disciplina", c.getDisciplina().getNombre(),
                "horaAnterior", horaAnterior, "horaNueva", horaNueva, "fecha", String.valueOf(r.getFecha()));
        crear(centro, TipoNotificacion.CLASE_HORARIO_CAMBIADO, TipoDestinatario.ALUMNO, alumno, null,
                CLASE_HORARIO_CAMBIADO_TITULO, render(CLASE_HORARIO_CAMBIADO_MSG, v),
                "RESERVA", r.getId(), null,
                centro.isNotificacionesClaseActivo(), alumno.getEmail());
    }

    public void notificarClaseInstructorCambiado(Reserva r, String instructorNuevo) {
        Centro centro = r.getCentro();
        Alumno alumno = r.getAlumno();
        Clase c = r.getClase();
        Map<String, String> v = mapOf("disciplina", c.getDisciplina().getNombre(),
                "instructorNuevo", instructorNuevo != null ? instructorNuevo : "por confirmar",
                "fecha", String.valueOf(r.getFecha()));
        crear(centro, TipoNotificacion.CLASE_INSTRUCTOR_CAMBIADO, TipoDestinatario.ALUMNO, alumno, null,
                CLASE_INSTRUCTOR_CAMBIADO_TITULO, render(CLASE_INSTRUCTOR_CAMBIADO_MSG, v),
                "RESERVA", r.getId(), null,
                centro.isNotificacionesClaseActivo(), alumno.getEmail());
    }

    public void notificarClaseRecordatorio(Reserva r) {
        Centro centro = r.getCentro();
        Alumno alumno = r.getAlumno();
        Clase c = r.getClase();
        Map<String, String> v = mapOf("disciplina", c.getDisciplina().getNombre(),
                "fecha", String.valueOf(r.getFecha()), "hora", String.valueOf(c.getHoraInicio()));
        crear(centro, TipoNotificacion.CLASE_RECORDATORIO, TipoDestinatario.ALUMNO, alumno, null,
                CLASE_RECORDATORIO_TITULO, render(CLASE_RECORDATORIO_MSG, v),
                "RESERVA", r.getId(), "CLASE_RECORDATORIO:" + r.getId(),
                centro.isNotificacionesClaseActivo(), alumno.getEmail());
    }

    // =====================================================================
    // Avisos administrativos (broadcast CENTRO_ADMIN, solo canal interno en este MVP)
    // =====================================================================

    public void notificarAdminMembresiasPorVencer(Centro centro, long cantidad, LocalDate hoy) {
        if (cantidad <= 0) return;
        Map<String, String> v = mapOf("cantidad", String.valueOf(cantidad));
        crear(centro, TipoNotificacion.ADMIN_MEMBRESIAS_POR_VENCER, TipoDestinatario.CENTRO_ADMIN, null, null,
                ADMIN_MEMBRESIAS_POR_VENCER_TITULO, render(ADMIN_MEMBRESIAS_POR_VENCER_MSG, v),
                "CENTRO", centro.getId(), "ADMIN_MEMB_SEMANA:" + centro.getId() + ":" + hoy,
                centro.isNotificacionesMembresiaActivo(), null);
    }

    public void notificarAdminSaldoPendiente(Centro centro, long cantidad, LocalDate hoy) {
        if (cantidad <= 0) return;
        Map<String, String> v = mapOf("cantidad", String.valueOf(cantidad));
        crear(centro, TipoNotificacion.ADMIN_ALUMNOS_SALDO_PENDIENTE, TipoDestinatario.CENTRO_ADMIN, null, null,
                ADMIN_ALUMNOS_SALDO_PENDIENTE_TITULO, render(ADMIN_ALUMNOS_SALDO_PENDIENTE_MSG, v),
                "CENTRO", centro.getId(), "ADMIN_SALDO:" + centro.getId() + ":" + hoy,
                centro.isNotificacionesMembresiaActivo(), null);
    }

    public void notificarAdminRiesgoAbandono(Centro centro, Alumno alumno, long diasSinAsistir, LocalDate hoy) {
        Map<String, String> v = mapOf("alumno", alumno.getNombre(), "dias", String.valueOf(diasSinAsistir));
        crear(centro, TipoNotificacion.ADMIN_ALUMNO_RIESGO_ABANDONO, TipoDestinatario.CENTRO_ADMIN, alumno, null,
                ADMIN_ALUMNO_RIESGO_ABANDONO_TITULO, render(ADMIN_ALUMNO_RIESGO_ABANDONO_MSG, v),
                "ALUMNO", alumno.getId(), "ADMIN_RIESGO:" + alumno.getId() + ":" + hoy,
                centro.isNotificacionesMembresiaActivo(), null);
    }

    public void notificarAdminCupoLleno(Clase c, LocalDate fecha) {
        Centro centro = c.getCentro();
        Map<String, String> v = mapOf("disciplina", c.getDisciplina().getNombre(), "hora", String.valueOf(c.getHoraInicio()),
                "fecha", String.valueOf(fecha), "capacidad", String.valueOf(c.getCapacidadMaxima()));
        crear(centro, TipoNotificacion.ADMIN_CLASE_CUPO_LLENO, TipoDestinatario.CENTRO_ADMIN, null, null,
                ADMIN_CLASE_CUPO_LLENO_TITULO, render(ADMIN_CLASE_CUPO_LLENO_MSG, v),
                "CLASE", c.getId(), "ADMIN_CUPO:" + c.getId() + ":" + fecha,
                centro.isNotificacionesClaseActivo(), null);
    }

    // =====================================================================
    // Consultas para el controller (bandeja/campana)
    // =====================================================================

    @Transactional(readOnly = true)
    public PageResponse<NotificacionDto> listarBandeja(Usuario actor, boolean soloNoLeidas, Pageable pageable) {
        Long centroId = tenantScope.scopeId(actor);
        var page = soloNoLeidas
                ? notificacionRepository.findByCentroIdAndDestinatarioTipoAndInternoVisibleTrueAndLeidaFalseOrderByCreatedAtDesc(
                        centroId, TipoDestinatario.CENTRO_ADMIN, pageable)
                : notificacionRepository.findByCentroIdAndDestinatarioTipoAndInternoVisibleTrueOrderByCreatedAtDesc(
                        centroId, TipoDestinatario.CENTRO_ADMIN, pageable);
        return PageResponse.of(page, this::toDto);
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(Usuario actor) {
        return notificacionRepository.countByCentroIdAndDestinatarioTipoAndInternoVisibleTrueAndLeidaFalse(
                tenantScope.scopeId(actor), TipoDestinatario.CENTRO_ADMIN);
    }

    @Transactional
    public void marcarLeida(Usuario actor, Long id) {
        Notificacion n = notificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion no encontrada"));
        if (!n.getCentro().getId().equals(tenantScope.scopeId(actor))) {
            throw new ResourceNotFoundException("Notificacion no encontrada");
        }
        if (!n.isLeida()) {
            n.setLeida(true);
            n.setLeidaEn(LocalDateTime.now());
            notificacionRepository.save(n);
        }
    }

    @Transactional
    public void marcarTodasLeidas(Usuario actor) {
        notificacionRepository.marcarTodasLeidas(tenantScope.scopeId(actor), TipoDestinatario.CENTRO_ADMIN, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<NotificacionDto> historialPorAlumno(Usuario actor, Long alumnoId) {
        return notificacionRepository.findByCentroIdAndAlumnoIdOrderByCreatedAtDesc(tenantScope.scopeId(actor), alumnoId)
                .stream().map(this::toDto).toList();
    }

    public NotificacionDto toDto(Notificacion n) {
        return new NotificacionDto(
                n.getId(), n.getTipo().name(), n.getDestinatarioTipo().name(),
                n.getAlumno() != null ? n.getAlumno().getId() : null,
                n.getAlumno() != null ? n.getAlumno().getNombre() : null,
                n.getInstructor() != null ? n.getInstructor().getId() : null,
                n.getInstructor() != null ? n.getInstructor().getNombre() : null,
                n.getTitulo(), n.getMensaje(), n.getEntidadTipo(), n.getEntidadId(),
                n.isLeida(), n.getLeidaEn(), n.getCreatedAt()
        );
    }
}
