package com.nexora.sport.service;

import com.nexora.sport.dto.MembresiaDto;
import com.nexora.sport.dto.MembresiaRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.AlumnoRepository;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.MembresiaPlanRepository;
import com.nexora.sport.repository.MembresiaRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class MembresiaService {

    private final MembresiaRepository membresiaRepository;
    private final MembresiaPlanRepository planRepository;
    private final AlumnoRepository alumnoRepository;
    private final CentroRepository centroRepository;
    private final CajaService cajaService;
    private final TenantScope tenantScope;

    public MembresiaService(MembresiaRepository membresiaRepository, MembresiaPlanRepository planRepository,
                             AlumnoRepository alumnoRepository, CentroRepository centroRepository,
                             CajaService cajaService, TenantScope tenantScope) {
        this.membresiaRepository = membresiaRepository;
        this.planRepository = planRepository;
        this.alumnoRepository = alumnoRepository;
        this.centroRepository = centroRepository;
        this.cajaService = cajaService;
        this.tenantScope = tenantScope;
    }

    @Transactional(readOnly = true)
    public PageResponse<MembresiaDto> listar(Usuario actor, Pageable pageable) {
        return PageResponse.of(membresiaRepository.findByCentroId(tenantScope.scopeId(actor), pageable), this::toDto);
    }

    @Transactional(readOnly = true)
    public PageResponse<MembresiaDto> listarPorAlumno(Long alumnoId, Pageable pageable) {
        return PageResponse.of(membresiaRepository.findByAlumnoId(alumnoId, pageable), this::toDto);
    }

    @Transactional
    public MembresiaDto crear(Usuario actor, MembresiaRequest request) {
        Long centroId = tenantScope.scopeId(actor);
        MembresiaPlan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"));
        Alumno alumno = alumnoRepository.findById(request.alumnoId())
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));

        LocalDate inicio = request.fechaInicio() != null ? request.fechaInicio() : LocalDate.now();
        LocalDate fin = calcularFin(plan, inicio);

        Membresia membresia = new Membresia();
        membresia.setCentro(centroRepository.getReferenceById(centroId));
        membresia.setAlumno(alumno);
        membresia.setPlan(plan);
        membresia.setFechaInicio(inicio);
        membresia.setFechaFin(fin);
        membresia.setClasesRestantes(plan.getNumeroClasesIncluidas());
        membresia.setPrecioFinal(request.precioFinal() != null ? request.precioFinal() : plan.getPrecio());
        membresia.setEstado(EstadoMembresia.ACTIVA);
        membresia = membresiaRepository.save(membresia);

        if (request.registrarCobro()) {
            MetodoPago metodo = request.metodoPago() != null ? MetodoPago.valueOf(request.metodoPago()) : MetodoPago.EFECTIVO;
            cajaService.registrarIngresoDeMembresia(centroId, membresia, membresia.getPrecioFinal(), metodo, actor);
        }
        return toDto(membresia);
    }

    @Transactional
    public MembresiaDto cancelar(Long id) {
        Membresia m = buscar(id);
        m.setEstado(EstadoMembresia.CANCELADA);
        return toDto(membresiaRepository.save(m));
    }

    public Membresia buscar(Long id) {
        return membresiaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Membresia no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<MembresiaDto> proximasAVencer(Usuario actor, int diasHorizonte) {
        Long centroId = tenantScope.scopeId(actor);
        return membresiaRepository.findProximasAVencer(centroId, LocalDate.now(), LocalDate.now().plusDays(diasHorizonte))
                .stream().map(this::toDto).toList();
    }

    /** Marca como VENCIDA cualquier membresia activa cuya fecha_fin ya paso. Se corre al vuelo y via job diario. */
    @Transactional
    public void actualizarVencidas(Long centroId) {
        var vencidas = centroId != null
                ? membresiaRepository.findVencidasNoActualizadas(centroId, LocalDate.now())
                : membresiaRepository.findAllVencidasNoActualizadas(LocalDate.now());
        vencidas.forEach(m -> m.setEstado(EstadoMembresia.VENCIDA));
        membresiaRepository.saveAll(vencidas);
    }

    private LocalDate calcularFin(MembresiaPlan plan, LocalDate inicio) {
        if (plan.getDuracionDias() != null) return inicio.plusDays(plan.getDuracionDias());
        return switch (plan.getTipoPeriodo()) {
            case SEMANAL -> inicio.plusWeeks(1);
            case QUINCENAL -> inicio.plusDays(15);
            case MENSUAL -> inicio.plusMonths(1);
            default -> inicio.plusMonths(1);
        };
    }

    public MembresiaDto toDto(Membresia m) {
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), m.getFechaFin());
        return new MembresiaDto(
                m.getId(), m.getAlumno().getId(), m.getAlumno().getNombre(), m.getPlan().getId(),
                m.getPlan().getNombre(), m.getFechaInicio(), m.getFechaFin(), m.getClasesRestantes(),
                m.getPrecioFinal(), m.getEstado().name(), dias
        );
    }
}
