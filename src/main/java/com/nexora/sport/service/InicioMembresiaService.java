package com.nexora.sport.service;

import com.nexora.sport.dto.InicioMembresiaDto;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.ClaseRepository;
import com.nexora.sport.repository.ReservaRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Regla de inicio de vigencia de una membresia dada de alta en mostrador:
 *
 *  1. Se revisa si HOY todavia queda una clase activa de una disciplina de la membresia, que
 *     aun no haya empezado, con lugares disponibles (capacidad - reservas no canceladas de
 *     hoy), en la sucursal de preferencia del alumno (si no tiene, en cualquiera del centro).
 *  2. Si NO queda: la membresia arranca el siguiente dia habil (los domingos no cuentan).
 *  3. Si SI queda: queda PENDIENTE con inicio el siguiente dia habil, pero si el alumno
 *     registra asistencia HOY la vigencia arranca en ese momento (ver
 *     Membresia#fechaInicioAnticipable y AsistenciaService). Si no asiste, arranca al dia
 *     siguiente habil como en el punto 2.
 */
@Service
public class InicioMembresiaService {

    private static final DateTimeFormatter FECHA_TEXTO =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", new Locale("es", "MX"));

    private final ClaseRepository claseRepository;
    private final ReservaRepository reservaRepository;

    public InicioMembresiaService(ClaseRepository claseRepository, ReservaRepository reservaRepository) {
        this.claseRepository = claseRepository;
        this.reservaRepository = reservaRepository;
    }

    public record ClaseHoy(Clase clase, long lugares) {}

    public InicioMembresiaDto calcular(Long centroId, MembresiaPlan plan, Set<Disciplina> disciplinasElegidas,
                                       Sucursal sucursalPreferida) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();
        LocalDate siguiente = siguienteDiaHabil(hoy);
        Optional<ClaseHoy> clase = buscarClaseDisponibleHoy(centroId, plan, disciplinasElegidas, sucursalPreferida, ahora);

        if (clase.isEmpty()) {
            String donde = sucursalPreferida != null ? " en " + sucursalPreferida.getNombre() : "";
            return new InicioMembresiaDto(false, siguiente, null, null, null, null, null,
                    "Ya no hay clases disponibles hoy" + donde + " para su disciplina. "
                            + "Su membresía empezará a correr el " + FECHA_TEXTO.format(siguiente) + ".");
        }
        Clase c = clase.get().clase();
        return new InicioMembresiaDto(true, siguiente, hoy,
                c.getDisciplina().getNombre(), c.getSucursal().getNombre(), String.valueOf(c.getHoraInicio()),
                clase.get().lugares(),
                "Todavía hay clase de " + c.getDisciplina().getNombre() + " hoy a las " + c.getHoraInicio()
                        + " en " + c.getSucursal().getNombre() + " (" + clase.get().lugares() + " lugares). "
                        + "Si registra su asistencia hoy, su membresía empieza a correr desde hoy; "
                        + "si no asiste, empezará el " + FECHA_TEXTO.format(siguiente) + ".");
    }

    /** La primera clase de hoy (por hora) que cumple la regla del punto 1, si hay. */
    public Optional<ClaseHoy> buscarClaseDisponibleHoy(Long centroId, MembresiaPlan plan, Set<Disciplina> disciplinasElegidas,
                                                       Sucursal sucursalPreferida, LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        DiaSemana diaHoy = DiaSemana.values()[hoy.getDayOfWeek().getValue() - 1];
        Set<Long> disciplinasPermitidas = disciplinasPermitidas(plan, disciplinasElegidas);
        return claseRepository.findByCentroIdAndActivoTrue(centroId).stream()
                .filter(c -> c.getDiaSemana() == diaHoy)
                .filter(c -> c.getHoraInicio().isAfter(ahora.toLocalTime()))
                .filter(c -> disciplinasPermitidas == null || disciplinasPermitidas.contains(c.getDisciplina().getId()))
                .filter(c -> sucursalPreferida == null || c.getSucursal().getId().equals(sucursalPreferida.getId()))
                .sorted(Comparator.comparing(Clase::getHoraInicio))
                .map(c -> new ClaseHoy(c, c.getCapacidadMaxima()
                        - reservaRepository.countByClaseIdAndFechaAndEstadoNot(c.getId(), hoy, EstadoReserva.CANCELADA)))
                .filter(ch -> ch.lugares() > 0)
                .findFirst();
    }

    /** null = cualquier disciplina (plan de acceso completo, o plan sin disciplinas configuradas). */
    public static Set<Long> disciplinasPermitidas(MembresiaPlan plan, Set<Disciplina> disciplinasElegidas) {
        if (plan.isAccesoCompleto()) return null;
        if (disciplinasElegidas != null && !disciplinasElegidas.isEmpty()) {
            return disciplinasElegidas.stream().map(Disciplina::getId).collect(Collectors.toSet());
        }
        if (plan.getDisciplinas() == null || plan.getDisciplinas().isEmpty()) return null;
        return plan.getDisciplinas().stream().map(Disciplina::getId).collect(Collectors.toSet());
    }

    /** Dia siguiente; si cae en domingo, el lunes. */
    public static LocalDate siguienteDiaHabil(LocalDate desde) {
        LocalDate d = desde.plusDays(1);
        return d.getDayOfWeek() == DayOfWeek.SUNDAY ? d.plusDays(1) : d;
    }
}
