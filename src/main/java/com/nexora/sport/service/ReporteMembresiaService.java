package com.nexora.sport.service;

import com.nexora.sport.dto.reportes.*;
import com.nexora.sport.model.Disciplina;
import com.nexora.sport.model.EstadoMembresia;
import com.nexora.sport.model.Membresia;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.AsistenciaRepository;
import com.nexora.sport.repository.MembresiaRepository;
import com.nexora.sport.repository.PagoMembresiaRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.nexora.sport.service.ReporteUtils.toBigDecimal;

/** Reportes de Membresias: ventas (contrataciones nuevas), vencimientos y renovaciones. */
@Service
public class ReporteMembresiaService {

    private final MembresiaRepository membresiaRepository;
    private final PagoMembresiaRepository pagoMembresiaRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final TenantScope tenantScope;

    public ReporteMembresiaService(MembresiaRepository membresiaRepository, PagoMembresiaRepository pagoMembresiaRepository,
                                    AsistenciaRepository asistenciaRepository, TenantScope tenantScope) {
        this.membresiaRepository = membresiaRepository;
        this.pagoMembresiaRepository = pagoMembresiaRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.tenantScope = tenantScope;
    }

    /** "Ventas": contrataciones NUEVAS (accrual, precioFinal) vs. totalCobrado (cash, TODOS los pagos validos del periodo). */
    @Transactional(readOnly = true)
    public MembresiaVentasResumenDto ventas(Usuario actor, LocalDate from, LocalDate to) {
        Long centroId = tenantScope.scopeId(actor);
        List<Membresia> nuevas = membresiaRepository.findNuevasEnPeriodo(centroId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        BigDecimal totalContratado = nuevas.stream().map(Membresia::getPrecioFinal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCobrado = pagoMembresiaRepository.sumValidoByCentroAndRango(centroId, from, to);

        Map<String, EtiquetaValorDto> porPlan = new LinkedHashMap<>();
        for (Membresia m : nuevas) {
            String plan = m.getPlanNombreSnapshot();
            EtiquetaValorDto actualEv = porPlan.get(plan);
            porPlan.put(plan, new EtiquetaValorDto(m.getPlan().getId(), plan,
                    (actualEv == null ? 0 : actualEv.cantidad()) + 1,
                    (actualEv == null ? BigDecimal.ZERO : actualEv.total()).add(m.getPrecioFinal())));
        }
        return new MembresiaVentasResumenDto(nuevas.size(), totalContratado, totalCobrado, new ArrayList<>(porPlan.values()));
    }

    @Transactional(readOnly = true)
    public VencimientosResumenDto vencimientos(Usuario actor, int horizonteDias) {
        Long centroId = tenantScope.scopeId(actor);
        LocalDate hoy = LocalDate.now();
        List<Membresia> activas = membresiaRepository.findProximasAVencer(centroId, hoy, hoy.plusDays(horizonteDias));
        List<Membresia> venceHoy = activas.stream().filter(m -> m.getFechaFin().isEqual(hoy)).toList();
        List<Membresia> proximos = activas.stream().filter(m -> m.getFechaFin().isAfter(hoy)).toList();
        List<Membresia> vencidas = membresiaRepository.findVencidasNoActualizadas(centroId, hoy);
        // findVencidasNoActualizadas solo ve ACTIVA con fecha ya pasada (el job diario las pasa a VENCIDA);
        // aqui tambien queremos las que YA quedaron marcadas VENCIDA por el job.
        List<Membresia> vencidasFormal = membresiaRepository.findByCentroIdAndEstado(centroId, EstadoMembresia.VENCIDA);
        List<Membresia> todasVencidas = new ArrayList<>(vencidas);
        for (Membresia m : vencidasFormal) if (todasVencidas.stream().noneMatch(x -> x.getId().equals(m.getId()))) todasVencidas.add(m);
        List<Membresia> vencidasSinRenovar = todasVencidas.stream()
                .filter(m -> !membresiaRepository.existsByMembresiaAnteriorId(m.getId())).toList();

        return new VencimientosResumenDto(
                mapear(venceHoy), mapear(proximos), mapear(todasVencidas), mapear(vencidasSinRenovar));
    }

    private List<VencimientoItemDto> mapear(List<Membresia> lista) {
        if (lista.isEmpty()) return List.of();
        List<Long> ids = lista.stream().map(Membresia::getId).toList();
        Map<Long, BigDecimal> pagado = new HashMap<>();
        for (Object[] row : pagoMembresiaRepository.sumValidoAgrupadoPorMembresia(ids)) {
            pagado.put((Long) row[0], toBigDecimal(row[1]));
        }
        return lista.stream().map(m -> new VencimientoItemDto(
                m.getId(), m.getAlumno().getId(), m.getAlumno().getNombre(), m.getPlanNombreSnapshot(),
                m.getDisciplinas().stream().map(Disciplina::getNombre).collect(Collectors.joining(", ")),
                m.getFechaFin(), m.getPrecioFinal().subtract(pagado.getOrDefault(m.getId(), BigDecimal.ZERO)),
                m.getEstado().name(),
                asistenciaRepository.findFirstByAlumnoIdOrderByFechaDescHoraDesc(m.getAlumno().getId()).map(a -> a.getFecha()).orElse(null)
        )).toList();
    }

    @Transactional(readOnly = true)
    public RenovacionesResumenDto renovaciones(Usuario actor, LocalDate from, LocalDate to) {
        Long centroId = tenantScope.scopeId(actor);
        List<Membresia> renovaciones = membresiaRepository.findRenovacionesEnPeriodo(centroId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        List<RenovacionItemDto> detalle = new ArrayList<>();
        long mismoPlan = 0, cambioPlan = 0, cambioDisciplina = 0, anticipadas = 0, tardias = 0;
        for (Membresia m : renovaciones) {
            Membresia anterior = m.getMembresiaAnterior();
            boolean esMismoPlan = anterior.getPlan().getId().equals(m.getPlan().getId());
            Set<Long> discAnterior = anterior.getDisciplinas().stream().map(Disciplina::getId).collect(Collectors.toSet());
            Set<Long> discNueva = m.getDisciplinas().stream().map(Disciplina::getId).collect(Collectors.toSet());
            boolean esCambioDisciplina = !discAnterior.equals(discNueva);
            boolean esAnticipada = anterior.getFechaFin() == null || !m.getFechaInicio().isAfter(anterior.getFechaFin().plusDays(1));

            if (esMismoPlan) mismoPlan++; else cambioPlan++;
            if (esCambioDisciplina) cambioDisciplina++;
            if (esAnticipada) anticipadas++; else tardias++;

            detalle.add(new RenovacionItemDto(m.getId(), m.getAlumno().getId(), m.getAlumno().getNombre(),
                    anterior.getPlanNombreSnapshot(), m.getPlanNombreSnapshot(), m.getFechaInicio(),
                    esMismoPlan, esCambioDisciplina, esAnticipada, !esAnticipada));
        }
        return new RenovacionesResumenDto(renovaciones.size(), mismoPlan, cambioPlan, cambioDisciplina, anticipadas, tardias, detalle);
    }
}
