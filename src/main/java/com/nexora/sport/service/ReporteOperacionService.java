package com.nexora.sport.service;

import com.nexora.sport.dto.reportes.*;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.*;
import com.nexora.sport.security.TenantScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.nexora.sport.service.ReporteUtils.*;

/**
 * Reportes de Operacion: asistencias, ocupacion de clases, instructores y disciplinas.
 * "Clases impartidas" aqui SIEMPRE significa sesiones reales ocurridas en el rango de
 * fechas (Clase es una plantilla semanal recurrente, no un log de sesiones), calculado
 * contando cuantas fechas del rango caen en el dia de la semana de cada Clase.
 */
@Service
public class ReporteOperacionService {

    private final AsistenciaRepository asistenciaRepository;
    private final ReservaRepository reservaRepository;
    private final ClaseRepository claseRepository;
    private final InstructorRepository instructorRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final MembresiaRepository membresiaRepository;
    private final VentaRepository ventaRepository;
    private final PagoMembresiaRepository pagoMembresiaRepository;
    private final TenantScope tenantScope;

    public ReporteOperacionService(AsistenciaRepository asistenciaRepository, ReservaRepository reservaRepository,
                                    ClaseRepository claseRepository, InstructorRepository instructorRepository,
                                    DisciplinaRepository disciplinaRepository, MembresiaRepository membresiaRepository,
                                    VentaRepository ventaRepository, PagoMembresiaRepository pagoMembresiaRepository,
                                    TenantScope tenantScope) {
        this.asistenciaRepository = asistenciaRepository;
        this.reservaRepository = reservaRepository;
        this.claseRepository = claseRepository;
        this.instructorRepository = instructorRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.membresiaRepository = membresiaRepository;
        this.ventaRepository = ventaRepository;
        this.pagoMembresiaRepository = pagoMembresiaRepository;
        this.tenantScope = tenantScope;
    }

    @Transactional(readOnly = true)
    public AsistenciasResumenDto asistencias(Usuario actor, LocalDate from, LocalDate to) {
        Long centroId = tenantScope.scopeId(actor);
        long total = asistenciaRepository.countByCentroIdAndFechaBetween(centroId, from, to);
        long unicos = asistenciaRepository.countAlumnosUnicos(centroId, from, to);
        BigDecimal promedio = unicos > 0 ? BigDecimal.valueOf(total).divide(BigDecimal.valueOf(unicos), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        List<SeriePuntoDto> porDia = asistenciaRepository.porDia(centroId, from, to).stream()
                .map(r -> new SeriePuntoDto(toLocalDate(r[0]), BigDecimal.ZERO, toLong(r[2]))).toList();
        List<EtiquetaValorDto> porDisciplina = asistenciaRepository.porDisciplina(centroId, from, to).stream()
                .map(r -> new EtiquetaValorDto((Long) r[0], (String) r[1], toLong(r[2]), BigDecimal.ZERO)).toList();
        // Solo cubre asistencias ligadas a una Clase (via Clase.sucursal); las de acceso libre sin clase no tienen sucursal hoy (ver decision de scope).
        List<EtiquetaValorDto> porSucursal = asistenciaRepository.porSucursal(centroId, from, to).stream()
                .map(r -> new EtiquetaValorDto((Long) r[0], (String) r[1], toLong(r[2]), BigDecimal.ZERO)).toList();
        return new AsistenciasResumenDto(total, unicos, promedio, porDia, porDisciplina, porSucursal);
    }

    @Transactional(readOnly = true)
    public List<OcupacionClaseDto> ocupacionClases(Usuario actor, LocalDate from, LocalDate to, Long sucursalId) {
        Long centroId = tenantScope.scopeId(actor);
        List<Clase> clases = claseRepository.findByCentroIdAndActivoTrue(centroId).stream()
                .filter(c -> sucursalId == null || c.getSucursal().getId().equals(sucursalId))
                .toList();
        List<OcupacionClaseDto> resultado = new ArrayList<>();
        for (Clase c : clases) {
            long sesiones = contarSesiones(c.getDiaSemana(), from, to);
            long reservados = reservaRepository.countByClaseIdAndFechaBetweenAndEstadoNot(c.getId(), from, to, EstadoReserva.CANCELADA);
            long asistieron = asistenciaRepository.countByClaseIdAndFechaBetween(c.getId(), from, to);
            long capacidadDisponible = c.getCapacidadMaxima() * sesiones;
            BigDecimal ocupacion = capacidadDisponible > 0
                    ? BigDecimal.valueOf(asistieron).divide(BigDecimal.valueOf(capacidadDisponible), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
            String horario = capitalize(c.getDiaSemana().name()) + " " + c.getHoraInicio() + "-" + c.getHoraFin();
            resultado.add(new OcupacionClaseDto(c.getId(), c.getDisciplina().getNombre(),
                    c.getInstructor() != null ? c.getInstructor().getNombre() : null, c.getSucursal().getNombre(),
                    horario, c.getCapacidadMaxima(), reservados, asistieron, ocupacion));
        }
        resultado.sort((a, b) -> b.porcentajeOcupacion().compareTo(a.porcentajeOcupacion()));
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<InstructorAnaliticaDto> instructores(Usuario actor, LocalDate from, LocalDate to) {
        Long centroId = tenantScope.scopeId(actor);
        List<InstructorAnaliticaDto> resultado = new ArrayList<>();
        for (Instructor ins : instructorRepository.findByCentroIdAndActivoTrue(centroId)) {
            List<Clase> clases = claseRepository.findByInstructorId(ins.getId()).stream().filter(Clase::isActivo).toList();
            long sesiones = clases.stream().mapToLong(c -> contarSesiones(c.getDiaSemana(), from, to)).sum();
            long capacidadDisponible = clases.stream().mapToLong(c -> (long) c.getCapacidadMaxima() * contarSesiones(c.getDiaSemana(), from, to)).sum();
            long asistencias = asistenciaRepository.countByInstructorAndRango(ins.getId(), from, to);
            BigDecimal promedioAlumnos = sesiones > 0 ? BigDecimal.valueOf(asistencias).divide(BigDecimal.valueOf(sesiones), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal ocupacionPromedio = capacidadDisponible > 0
                    ? BigDecimal.valueOf(asistencias).divide(BigDecimal.valueOf(capacidadDisponible), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
            resultado.add(new InstructorAnaliticaDto(ins.getId(), ins.getNombre(), sesiones, asistencias, promedioAlumnos, ocupacionPromedio));
        }
        resultado.sort((a, b) -> Long.compare(b.clasesImpartidas(), a.clasesImpartidas()));
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<DisciplinaAnaliticaDto> disciplinas(Usuario actor, LocalDate from, LocalDate to) {
        Long centroId = tenantScope.scopeId(actor);
        List<DisciplinaAnaliticaDto> resultado = new ArrayList<>();
        for (Disciplina d : disciplinaRepository.findByCentroIdAndActivoTrue(centroId)) {
            List<Membresia> membresias = membresiaRepository.findActivasPorDisciplina(centroId, d.getId());
            long alumnosActivos = membresias.stream().map(m -> m.getAlumno().getId()).distinct().count();
            BigDecimal ingresos = membresias.stream().map(Membresia::getPrecioFinal).reduce(BigDecimal.ZERO, BigDecimal::add);
            long asistencias = asistenciaRepository.countByCentroIdAndDisciplinaIdAndFechaBetween(centroId, d.getId(), from, to);
            long sesiones = claseRepository.findByCentroIdAndActivoTrue(centroId).stream()
                    .filter(c -> c.getDisciplina().getId().equals(d.getId()))
                    .mapToLong(c -> contarSesiones(c.getDiaSemana(), from, to)).sum();
            resultado.add(new DisciplinaAnaliticaDto(d.getId(), d.getNombre(), alumnosActivos, membresias.size(), asistencias, sesiones, ingresos));
        }
        resultado.sort((a, b) -> Long.compare(b.alumnosActivos(), a.alumnosActivos()));
        return resultado;
    }

    /** "Operaciones por usuario" (seccion 14): staff administrativo (ventas + pagos registrados), NO instructores. */
    @Transactional(readOnly = true)
    public List<OperacionUsuarioDto> operacionesPorUsuario(Usuario actor, LocalDate from, LocalDate to) {
        Long centroId = tenantScope.scopeId(actor);
        Map<Long, String> nombres = new LinkedHashMap<>();
        Map<Long, long[]> conteos = new HashMap<>(); // [ventas, pagos]
        Map<Long, BigDecimal[]> montos = new HashMap<>(); // [totalVentas, totalPagos]

        for (Object[] r : ventaRepository.operacionesPorUsuario(centroId, from.atStartOfDay(), to.plusDays(1).atStartOfDay())) {
            Long id = (Long) r[0];
            nombres.put(id, (String) r[1]);
            conteos.computeIfAbsent(id, k -> new long[2])[0] = toLong(r[2]);
            montos.computeIfAbsent(id, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO})[0] = toBigDecimal(r[3]);
        }
        for (Object[] r : pagoMembresiaRepository.operacionesPorUsuario(centroId, from, to)) {
            Long id = (Long) r[0];
            nombres.put(id, (String) r[1]);
            conteos.computeIfAbsent(id, k -> new long[2])[1] = toLong(r[2]);
            montos.computeIfAbsent(id, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO})[1] = toBigDecimal(r[3]);
        }

        List<OperacionUsuarioDto> resultado = nombres.entrySet().stream().map(e -> {
            long[] c = conteos.getOrDefault(e.getKey(), new long[2]);
            BigDecimal[] m = montos.getOrDefault(e.getKey(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            return new OperacionUsuarioDto(e.getKey(), e.getValue(), c[0], m[0], c[1], m[1]);
        }).collect(Collectors.toList());
        resultado.sort((a, b) -> b.totalVentas().add(b.totalPagos()).compareTo(a.totalVentas().add(a.totalPagos())));
        return resultado;
    }

    private long contarSesiones(DiaSemana diaSemana, LocalDate from, LocalDate to) {
        DayOfWeek objetivo = mapDia(diaSemana);
        long dias = to.toEpochDay() - from.toEpochDay() + 1;
        if (dias <= 0) return 0;
        long primerOffset = (objetivo.getValue() - from.getDayOfWeek().getValue() + 7) % 7;
        if (primerOffset >= dias) return 0;
        return (dias - primerOffset - 1) / 7 + 1;
    }

    private DayOfWeek mapDia(DiaSemana d) {
        return switch (d) {
            case LUNES -> DayOfWeek.MONDAY;
            case MARTES -> DayOfWeek.TUESDAY;
            case MIERCOLES -> DayOfWeek.WEDNESDAY;
            case JUEVES -> DayOfWeek.THURSDAY;
            case VIERNES -> DayOfWeek.FRIDAY;
            case SABADO -> DayOfWeek.SATURDAY;
            case DOMINGO -> DayOfWeek.SUNDAY;
        };
    }

    private String capitalize(String s) {
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}
