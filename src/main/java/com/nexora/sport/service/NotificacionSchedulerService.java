package com.nexora.sport.service;

import com.nexora.sport.dto.reportes.RiesgoAbandonoItemDto;
import com.nexora.sport.model.Centro;
import com.nexora.sport.model.EstadoReserva;
import com.nexora.sport.model.Membresia;
import com.nexora.sport.model.Reserva;
import com.nexora.sport.repository.AlumnoRepository;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.MembresiaRepository;
import com.nexora.sport.repository.PagoMembresiaRepository;
import com.nexora.sport.repository.ReservaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Logica de los eventos TEMPORALES (seccion 19 del encargo): los que no ocurren por una
 * accion del usuario sino que hay que "descubrir" barriendo la base con un
 * {@code @Scheduled}. Los jobs (NotificacionMembresiaJob, NotificacionClaseRecordatorioJob)
 * son deliberadamente delgados y solo llaman aqui, igual que MembresiaExpiryJob delega en
 * MembresiaService.
 *
 * Zona horaria: el proyecto no tiene configuracion de timezone por Centro (ver
 * Centro.java), asi que todo "hoy"/"en N horas" se calcula con la misma zona que ya usa
 * Hibernate (hibernate.jdbc.time_zone=America/Mexico_City en application.properties).
 * Limitacion documentada: si en el futuro un centro opera en otra zona, esto habria que
 * moverlo a un campo por Centro.
 */
@Service
public class NotificacionSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionSchedulerService.class);
    private static final ZoneId ZONE = ZoneId.of("America/Mexico_City");

    private final CentroRepository centroRepository;
    private final MembresiaRepository membresiaRepository;
    private final PagoMembresiaRepository pagoMembresiaRepository;
    private final ReservaRepository reservaRepository;
    private final AlumnoRepository alumnoRepository;
    private final ReporteAlumnoService reporteAlumnoService;
    private final NotificacionService notificacionService;

    public NotificacionSchedulerService(CentroRepository centroRepository, MembresiaRepository membresiaRepository,
                                         PagoMembresiaRepository pagoMembresiaRepository, ReservaRepository reservaRepository,
                                         AlumnoRepository alumnoRepository, ReporteAlumnoService reporteAlumnoService,
                                         NotificacionService notificacionService) {
        this.centroRepository = centroRepository;
        this.membresiaRepository = membresiaRepository;
        this.pagoMembresiaRepository = pagoMembresiaRepository;
        this.reservaRepository = reservaRepository;
        this.alumnoRepository = alumnoRepository;
        this.reporteAlumnoService = reporteAlumnoService;
        this.notificacionService = notificacionService;
    }

    /** Corre una vez al dia (ver NotificacionMembresiaJob): vencimientos + resumenes administrativos. */
    @Transactional
    public void procesarVencimientosMembresias() {
        LocalDate hoy = LocalDate.now(ZONE);
        for (Centro centro : centroRepository.findAll()) {
            if (!centro.isActivo() || !centro.isNotificacionesMembresiaActivo()) continue;
            try {
                procesarCentro(centro, hoy);
            } catch (Exception e) {
                log.error("Fallo procesando vencimientos de membresia del centro {}: {}", centro.getId(), e.getMessage(), e);
            }
        }
    }

    private void procesarCentro(Centro centro, LocalDate hoy) {
        Set<Integer> diasConfigurados = parseDias(centro.getNotificacionesDiasAntesVencimiento());
        List<Membresia> activas = membresiaRepository.findActivasConFechaFin(centro.getId());

        for (Membresia m : activas) {
            long dias = ChronoUnit.DAYS.between(hoy, m.getFechaFin());
            if (dias == 0 && diasConfigurados.contains(0)) {
                notificacionService.notificarMembresiaVenceHoy(m, hoy);
            } else if (dias > 0 && diasConfigurados.contains((int) dias)) {
                notificacionService.notificarMembresiaProximaAVencer(m, dias, hoy);
            }
        }

        // "Membresia vencida": una sola vez, el dia siguiente a fecha_fin (ya la marco VENCIDA MembresiaExpiryJob a las 00:05).
        membresiaRepository.findVencidasEnFecha(centro.getId(), hoy.minusDays(1))
                .forEach(notificacionService::notificarMembresiaVencida);

        // Resumenes administrativos (solo canal interno, seccion 16).
        long vencenEstaSemana = membresiaRepository.countActivasFechaFinEntre(centro.getId(), hoy, hoy.plusDays(7));
        notificacionService.notificarAdminMembresiasPorVencer(centro, vencenEstaSemana, hoy);

        long conSaldoPendiente = activas.stream()
                .filter(m -> pagoMembresiaRepository.sumValidoByMembresiaId(m.getId()).compareTo(m.getPrecioFinal()) < 0)
                .count();
        notificacionService.notificarAdminSaldoPendiente(centro, conSaldoPendiente, hoy);

        // Riesgo de abandono: reutiliza la MISMA regla que Reportes (seccion 30), no la duplica.
        List<RiesgoAbandonoItemDto> riesgos = reporteAlumnoService.riesgoAbandonoPorCentro(centro.getId());
        for (RiesgoAbandonoItemDto r : riesgos) {
            notificacionService.notificarAdminRiesgoAbandono(centro, alumnoRepository.getReferenceById(r.alumnoId()),
                    r.diasSinAsistir(), hoy);
        }
    }

    private Set<Integer> parseDias(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Integer::parseInt).collect(Collectors.toSet());
    }

    /** Corre cada 15 min (ver NotificacionClaseRecordatorioJob): recordatorios de clases reservadas. */
    @Transactional
    public void procesarRecordatoriosClase() {
        LocalDateTime ahora = LocalDateTime.now(ZONE);
        LocalDate hoy = ahora.toLocalDate();
        LocalDate manana = hoy.plusDays(1);

        for (Centro centro : centroRepository.findAll()) {
            if (!centro.isActivo() || !centro.isNotificacionesClaseActivo()) continue;
            Integer horas = centro.getNotificacionesHorasAntesClase();
            if (horas == null || horas <= 0) continue;
            try {
                List<Reserva> candidatas = reservaRepository.findByCentroIdAndEstadoAndFechaBetween(
                        centro.getId(), EstadoReserva.RESERVADA, hoy, manana);
                for (Reserva r : candidatas) {
                    LocalDateTime inicioClase = LocalDateTime.of(r.getFecha(), r.getClase().getHoraInicio());
                    long minutosFaltantes = ChronoUnit.MINUTES.between(ahora, inicioClase);
                    if (minutosFaltantes > 0 && minutosFaltantes <= horas * 60L) {
                        notificacionService.notificarClaseRecordatorio(r);
                    }
                }
            } catch (Exception e) {
                log.error("Fallo procesando recordatorios de clase del centro {}: {}", centro.getId(), e.getMessage(), e);
            }
        }
    }
}
