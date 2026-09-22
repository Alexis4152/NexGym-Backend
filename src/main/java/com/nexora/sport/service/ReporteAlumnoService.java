package com.nexora.sport.service;

import com.nexora.sport.dto.reportes.*;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.AlumnoRepository;
import com.nexora.sport.repository.AsistenciaRepository;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.MembresiaRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.nexora.sport.service.ReporteUtils.*;

/**
 * Reportes de Alumnos: activos/nuevos/bajas, retencion (basada en renovacion, ver
 * formula documentada en {@link #retencion}) y riesgo de abandono (basado en reglas,
 * sin IA, ver seccion 15 del encargo).
 */
@Service
public class ReporteAlumnoService {

    /** Ventana de gracia (dias) despues de vencer una membresia para considerar que "renovo" (seccion 8). Documentado aqui a falta de config por centro. */
    private static final int VENTANA_GRACIA_RETENCION_DIAS = 30;

    private final AlumnoRepository alumnoRepository;
    private final MembresiaRepository membresiaRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final CentroRepository centroRepository;
    private final TenantScope tenantScope;

    public ReporteAlumnoService(AlumnoRepository alumnoRepository, MembresiaRepository membresiaRepository,
                                 AsistenciaRepository asistenciaRepository, CentroRepository centroRepository,
                                 TenantScope tenantScope) {
        this.alumnoRepository = alumnoRepository;
        this.membresiaRepository = membresiaRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.centroRepository = centroRepository;
        this.tenantScope = tenantScope;
    }

    @Transactional(readOnly = true)
    public AlumnosResumenDto resumen(Usuario actor, LocalDate from, LocalDate to) {
        Long centroId = tenantScope.scopeId(actor);
        long activos = alumnoRepository.countByCentroIdAndEstadoAndDeletedAtIsNull(centroId, EstadoAlumno.ACTIVO);
        long nuevos = alumnoRepository.countByCentroIdAndFechaIngresoBetweenAndDeletedAtIsNull(centroId, from, to);
        long bajas = alumnoRepository.countByCentroIdAndFechaBajaBetween(centroId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        List<SeriePuntoDto> nuevosPorMes = alumnoRepository.nuevosPorMes(centroId, from, to).stream()
                .map(r -> new SeriePuntoDto(toLocalDate(r[0]), BigDecimal.ZERO, toLong(r[2]))).toList();
        return new AlumnosResumenDto(activos, nuevos, bajas, nuevosPorMes);
    }

    /**
     * Formula de retencion (seccion 8): elegibles = membresias cuyo fechaFin cae en
     * [from,to] y que NO fueron canceladas antes de vencer (una cancelacion no es una
     * oportunidad real de renovacion). Una elegible cuenta como "retenida" si el MISMO
     * alumno tiene otra membresia (cualquier plan/disciplina, incluso creada sin usar el
     * boton "Renovar") cuya fechaInicio cae dentro de los VENTANA_GRACIA_RETENCION_DIAS
     * siguientes a fechaFin — cambiar de plan o de disciplina SI cuenta como retencion,
     * tal como pide el encargo. Se cuenta por MEMBRESIA, no por alumno (un alumno con dos
     * membresias multidisciplina distintas aporta dos elegibles independientes).
     */
    @Transactional(readOnly = true)
    public RetencionResumenDto retencion(Usuario actor, LocalDate from, LocalDate to) {
        Long centroId = tenantScope.scopeId(actor);
        List<Membresia> elegibles = membresiaRepository.findElegiblesParaRetencion(centroId, from, to);
        List<RetencionItemDto> detalle = new ArrayList<>();
        long retenidos = 0;
        for (Membresia e : elegibles) {
            List<Membresia> delAlumno = membresiaRepository.findNoCanceladasPorAlumno(e.getAlumno().getId());
            LocalDate limite = e.getFechaFin().plusDays(VENTANA_GRACIA_RETENCION_DIAS);
            Membresia renovacion = delAlumno.stream()
                    .filter(m -> !m.getId().equals(e.getId()))
                    .filter(m -> m.getFechaInicio().isAfter(e.getFechaFin()) && !m.getFechaInicio().isAfter(limite))
                    .findFirst().orElse(null);
            boolean renovo = renovacion != null;
            if (renovo) retenidos++;
            detalle.add(new RetencionItemDto(e.getAlumno().getId(), e.getAlumno().getNombre(), e.getPlanNombreSnapshot(),
                    e.getFechaFin(), renovo, renovo ? renovacion.getFechaInicio() : null));
        }
        BigDecimal porcentaje = elegibles.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(retenidos).divide(BigDecimal.valueOf(elegibles.size()), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        return new RetencionResumenDto(elegibles.size(), retenidos, porcentaje, VENTANA_GRACIA_RETENCION_DIAS, detalle);
    }

    /** Reglas fijas (sin IA): membresia ACTIVA con fecha real + sin asistencia hace >= Centro.diasInactividadRiesgo dias. */
    @Transactional(readOnly = true)
    public List<RiesgoAbandonoItemDto> riesgoAbandono(Usuario actor) {
        Long centroId = tenantScope.scopeId(actor);
        int umbral = centroRepository.getReferenceById(centroId).getDiasInactividadRiesgo();
        LocalDate hoy = LocalDate.now();
        List<Membresia> activas = membresiaRepository.findActivasConFechaFin(centroId);

        List<RiesgoAbandonoItemDto> resultado = new ArrayList<>();
        for (Membresia m : activas) {
            LocalDate ultima = asistenciaRepository.findFirstByAlumnoIdOrderByFechaDescHoraDesc(m.getAlumno().getId())
                    .map(Asistencia::getFecha).orElse(null);
            long diasSinAsistir = ultima != null ? (hoy.toEpochDay() - ultima.toEpochDay()) : (hoy.toEpochDay() - m.getFechaInicio().toEpochDay());
            if (diasSinAsistir >= umbral) {
                resultado.add(new RiesgoAbandonoItemDto(m.getAlumno().getId(), m.getAlumno().getNombre(), m.getPlanNombreSnapshot(),
                        m.getDisciplinas().stream().map(Disciplina::getNombre).collect(Collectors.joining(", ")),
                        ultima, diasSinAsistir, m.getFechaFin()));
            }
        }
        resultado.sort((a, b) -> Long.compare(b.diasSinAsistir(), a.diasSinAsistir()));
        return resultado;
    }
}
