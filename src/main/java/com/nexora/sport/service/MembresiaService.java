package com.nexora.sport.service;

import com.nexora.sport.dto.MembresiaDto;
import com.nexora.sport.dto.MembresiaRenovarRequest;
import com.nexora.sport.dto.MembresiaRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.AlumnoRepository;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.CorteCajaRepository;
import com.nexora.sport.repository.MembresiaPlanRepository;
import com.nexora.sport.repository.MembresiaRepository;
import com.nexora.sport.repository.PagoMembresiaRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MembresiaService {

    private static final int UMBRAL_DIAS_PROXIMA_A_VENCER = 7;
    private static final int UMBRAL_CLASES_POR_AGOTARSE = 2;

    private final MembresiaRepository membresiaRepository;
    private final MembresiaPlanRepository planRepository;
    private final AlumnoRepository alumnoRepository;
    private final CentroRepository centroRepository;
    private final PagoMembresiaRepository pagoMembresiaRepository;
    private final PagoMembresiaService pagoMembresiaService;
    private final CorteCajaRepository corteCajaRepository;
    private final TenantScope tenantScope;
    private final NotificacionService notificacionService;

    public MembresiaService(MembresiaRepository membresiaRepository, MembresiaPlanRepository planRepository,
                             AlumnoRepository alumnoRepository, CentroRepository centroRepository,
                             PagoMembresiaRepository pagoMembresiaRepository, PagoMembresiaService pagoMembresiaService,
                             CorteCajaRepository corteCajaRepository,
                             TenantScope tenantScope, NotificacionService notificacionService) {
        this.membresiaRepository = membresiaRepository;
        this.planRepository = planRepository;
        this.alumnoRepository = alumnoRepository;
        this.centroRepository = centroRepository;
        this.pagoMembresiaRepository = pagoMembresiaRepository;
        this.pagoMembresiaService = pagoMembresiaService;
        this.corteCajaRepository = corteCajaRepository;
        this.tenantScope = tenantScope;
        this.notificacionService = notificacionService;
    }

    @Transactional(readOnly = true)
    public PageResponse<MembresiaDto> listar(Usuario actor, Pageable pageable) {
        return PageResponse.of(membresiaRepository.findByCentroId(tenantScope.scopeId(actor), pageable), this::toDto);
    }

    @Transactional(readOnly = true)
    public PageResponse<MembresiaDto> listarPorAlumno(Usuario actor, Long alumnoId, Pageable pageable) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));
        if (!alumno.getCentro().getId().equals(tenantScope.scopeId(actor))) {
            throw new ResourceNotFoundException("Alumno no encontrado");
        }
        return PageResponse.of(membresiaRepository.findByAlumnoId(alumnoId, pageable), this::toDto);
    }

    /** La(s) vigencia(s) que representan el estado actual del alumno para su expediente (seccion 25). */
    @Transactional(readOnly = true)
    public List<MembresiaDto> vigentesPorAlumno(Usuario actor, Long alumnoId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));
        if (!alumno.getCentro().getId().equals(tenantScope.scopeId(actor))) {
            throw new ResourceNotFoundException("Alumno no encontrado");
        }
        return membresiaRepository.findVigentesPorAlumnoOrdenadas(alumnoId).stream().map(this::toDto).toList();
    }

    @Transactional
    public MembresiaDto crear(Usuario actor, MembresiaRequest request) {
        assertCorteAbierto(actor);
        Long centroId = tenantScope.scopeId(actor);
        MembresiaPlan plan = buscarPlanDelCentro(request.planId(), centroId);
        assertCupoDisponible(plan);
        Alumno alumno = buscarAlumnoDelCentro(request.alumnoId(), centroId);

        LocalDate inicio = request.fechaInicio() != null ? request.fechaInicio() : LocalDate.now();
        BigDecimal descuento = validarDescuento(request.descuento(), plan.getPrecio());

        Membresia m = construirMembresia(centroId, alumno, plan, inicio, descuento, request.disciplinaIds(), null);
        m = membresiaRepository.save(m);

        registrarPagoInicialSiAplica(actor, centroId, m, request.montoPagoInicial(), request.metodoPago());
        notificacionService.notificarMembresiaNueva(m);
        notificacionService.notificarAdminMembresiaNueva(m);
        if (plan.getLimiteAlumnos() != null && membresiaRepository.countVigentesPorPlan(plan.getId()) >= plan.getLimiteAlumnos()) {
            notificacionService.notificarAdminPlanLleno(plan);
        }
        return toDto(m);
    }

    @Transactional
    public MembresiaDto renovar(Usuario actor, Long membresiaAnteriorId, MembresiaRenovarRequest request) {
        assertCorteAbierto(actor);
        Long centroId = tenantScope.scopeId(actor);
        Membresia anterior = buscarDelCentro(membresiaAnteriorId, centroId);
        if (anterior.getEstado() == EstadoMembresia.CANCELADA) {
            throw new IllegalStateException("No se puede renovar una membresia cancelada");
        }
        MembresiaPlan plan = buscarPlanDelCentro(request.planId(), centroId);

        LocalDate inicio;
        if (request.fechaInicio() != null) {
            if (!tenantScope.isAdminOSuperior(actor)) {
                throw new IllegalStateException("Solo Dueno o Administrador puede fijar manualmente la fecha de inicio de una renovacion");
            }
            inicio = request.fechaInicio();
        } else {
            LocalDate hoy = LocalDate.now();
            // Renovacion anticipada: la anterior aun no vence -> arranca al dia siguiente para no perder dias (seccion 9).
            // Renovacion tardia o sin fecha de fin (por clases puro): arranca hoy; politica configurable a futuro (seccion 10).
            inicio = (anterior.getFechaFin() != null && !anterior.getFechaFin().isBefore(hoy)) ? anterior.getFechaFin().plusDays(1) : hoy;
        }
        BigDecimal descuento = validarDescuento(request.descuento(), plan.getPrecio());

        Membresia m = construirMembresia(centroId, anterior.getAlumno(), plan, inicio, descuento, request.disciplinaIds(), anterior);
        m = membresiaRepository.save(m);

        registrarPagoInicialSiAplica(actor, centroId, m, request.montoPagoInicial(), request.metodoPago());
        notificacionService.notificarMembresiaRenovada(m);
        return toDto(m);
    }

    @Transactional
    public MembresiaDto suspender(Usuario actor, Long id, String motivo) {
        if (!com.nexora.sport.security.PermisoEvaluator.tiene(actor, com.nexora.sport.model.Permiso.MEMBRESIAS_SUSPENDER)) {
            throw new IllegalStateException("No tienes permiso para suspender membresias");
        }
        Membresia m = buscarDelCentro(id, tenantScope.scopeId(actor));
        if (m.getEstado() == EstadoMembresia.CANCELADA || m.getEstado() == EstadoMembresia.VENCIDA) {
            throw new IllegalStateException("No se puede suspender una membresia " + m.getEstado().name().toLowerCase());
        }
        m.setEstado(EstadoMembresia.SUSPENDIDA);
        m.setSuspendidaMotivo(motivo);
        m.setSuspendidaEn(LocalDateTime.now());
        m.setSuspendidaPor(actor);
        return toDto(membresiaRepository.save(m));
    }

    @Transactional
    public MembresiaDto reanudar(Usuario actor, Long id) {
        if (!tenantScope.isAdminOSuperior(actor)) {
            throw new IllegalStateException("Solo Dueno o Administrador puede reanudar una membresia");
        }
        Membresia m = buscarDelCentro(id, tenantScope.scopeId(actor));
        if (m.getEstado() != EstadoMembresia.SUSPENDIDA) {
            throw new IllegalStateException("Esta membresia no esta suspendida");
        }
        LocalDate hoy = LocalDate.now();
        if (m.getFechaFin() != null && m.getFechaFin().isBefore(hoy)) {
            m.setEstado(EstadoMembresia.VENCIDA);
        } else if (m.getFechaInicio().isAfter(hoy)) {
            m.setEstado(EstadoMembresia.PENDIENTE);
        } else {
            m.setEstado(EstadoMembresia.ACTIVA);
        }
        return toDto(membresiaRepository.save(m));
    }

    @Transactional
    public MembresiaDto cancelar(Usuario actor, Long id, String motivo) {
        if (!com.nexora.sport.security.PermisoEvaluator.tiene(actor, com.nexora.sport.model.Permiso.MEMBRESIAS_CANCELAR)) {
            throw new IllegalStateException("No tienes permiso para cancelar membresias");
        }
        Membresia m = buscarDelCentro(id, tenantScope.scopeId(actor));
        if (m.getEstado() == EstadoMembresia.CANCELADA) {
            throw new IllegalStateException("Esta membresia ya esta cancelada");
        }
        m.setEstado(EstadoMembresia.CANCELADA);
        m.setCanceladaMotivo(motivo);
        m.setCanceladaEn(LocalDateTime.now());
        m.setCanceladaPor(actor);
        m = membresiaRepository.save(m);
        notificacionService.notificarAdminMembresiaCanceladaOVencida(m, "fue cancelada");
        return toDto(m);
    }

    public Membresia buscar(Long id) {
        return membresiaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Membresia no encontrada"));
    }

    private Membresia buscarDelCentro(Long id, Long centroId) {
        Membresia m = buscar(id);
        if (!m.getCentro().getId().equals(centroId)) {
            throw new ResourceNotFoundException("Membresia no encontrada");
        }
        return m;
    }

    /** Registrar/renovar una membresia puede cobrar de inmediato (montoPagoInicial), asi
     * que exige un corte de caja propio abierto igual que VentaService#crear -- sin
     * excepcion para Dueno/SUPER_ADMIN, mismo criterio que ya aplica al vender en el POS. */
    private void assertCorteAbierto(Usuario actor) {
        corteCajaRepository.findFirstByUsuarioIdAndEstado(actor.getId(), EstadoCorteCaja.ABIERTO)
                .orElseThrow(() -> new IllegalStateException("Debes abrir un corte de caja antes de registrar una membresia"));
    }

    private MembresiaPlan buscarPlanDelCentro(Long planId, Long centroId) {
        MembresiaPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"));
        if (!plan.getCentro().getId().equals(centroId)) {
            throw new ResourceNotFoundException("Plan no encontrado");
        }
        if (!plan.isActivo()) {
            throw new IllegalStateException("Este plan esta desactivado");
        }
        return plan;
    }

    /** Solo aplica a contrataciones NUEVAS (crear): una renovacion es del mismo alumno
     * que ya ocupaba el cupo, no le suma un lugar nuevo al plan. */
    private void assertCupoDisponible(MembresiaPlan plan) {
        if (plan.getLimiteAlumnos() == null) return;
        long ocupados = membresiaRepository.countVigentesPorPlan(plan.getId());
        if (ocupados >= plan.getLimiteAlumnos()) {
            throw new IllegalStateException("Este plan ya alcanzo su cupo maximo de alumnos");
        }
    }

    private Alumno buscarAlumnoDelCentro(Long alumnoId, Long centroId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));
        if (!alumno.getCentro().getId().equals(centroId)) {
            throw new ResourceNotFoundException("Alumno no encontrado");
        }
        return alumno;
    }

    private BigDecimal validarDescuento(BigDecimal descuento, BigDecimal precio) {
        BigDecimal d = descuento != null ? descuento : BigDecimal.ZERO;
        if (d.signum() < 0 || d.compareTo(precio) > 0) {
            throw new IllegalArgumentException("El descuento no es valido");
        }
        return d;
    }

    private Membresia construirMembresia(Long centroId, Alumno alumno, MembresiaPlan plan, LocalDate inicio,
                                          BigDecimal descuento, Set<Long> disciplinaIds, Membresia anterior) {
        Membresia m = new Membresia();
        m.setCentro(centroRepository.getReferenceById(centroId));
        m.setAlumno(alumno);
        m.setPlan(plan);
        m.setPlanNombreSnapshot(plan.getNombre());
        m.setTipoPlanSnapshot(plan.getTipoPlan());
        m.setFechaInicio(inicio);
        m.setFechaFin(calcularFin(plan, inicio));
        m.setNumeroClasesContratadas(plan.getNumeroClasesIncluidas());
        m.setClasesRestantes(plan.getNumeroClasesIncluidas());
        m.setPrecioOriginal(plan.getPrecio());
        m.setDescuento(descuento);
        m.setPrecioFinal(plan.getPrecio().subtract(descuento));
        m.setEstado(inicio.isAfter(LocalDate.now()) ? EstadoMembresia.PENDIENTE : EstadoMembresia.ACTIVA);
        m.setMembresiaAnterior(anterior);
        validarYAplicarDisciplinas(m, plan, disciplinaIds);
        return m;
    }

    private void validarYAplicarDisciplinas(Membresia m, MembresiaPlan plan, Set<Long> disciplinaIds) {
        if (plan.isAccesoCompleto()) {
            m.setDisciplinas(new HashSet<>());
            return;
        }
        Set<Disciplina> permitidas = plan.getDisciplinas();
        if (permitidas.isEmpty()) {
            if (disciplinaIds != null && !disciplinaIds.isEmpty()) {
                throw new IllegalArgumentException("Este plan no tiene disciplinas configuradas");
            }
            m.setDisciplinas(new HashSet<>());
            return;
        }
        if (disciplinaIds == null || disciplinaIds.isEmpty()) {
            throw new IllegalArgumentException("Selecciona al menos una disciplina permitida por este plan");
        }
        int max = plan.getMaxDisciplinasSeleccionables() != null ? plan.getMaxDisciplinasSeleccionables() : 1;
        if (disciplinaIds.size() > max) {
            throw new IllegalArgumentException("Este plan permite elegir hasta " + max + " disciplina(s)");
        }
        Set<Long> permitidasIds = permitidas.stream().map(Disciplina::getId).collect(Collectors.toSet());
        if (!permitidasIds.containsAll(disciplinaIds)) {
            throw new IllegalArgumentException("Seleccionaste una disciplina no permitida por este plan");
        }
        m.setDisciplinas(permitidas.stream().filter(d -> disciplinaIds.contains(d.getId())).collect(Collectors.toSet()));
    }

    private void registrarPagoInicialSiAplica(Usuario actor, Long centroId, Membresia m, BigDecimal montoPagoInicial, String metodoPago) {
        if (montoPagoInicial != null && montoPagoInicial.compareTo(BigDecimal.ZERO) > 0) {
            pagoMembresiaService.registrarPagoInterno(actor, centroId, m, montoPagoInicial, metodoPago);
        }
    }

    private LocalDate calcularFin(MembresiaPlan plan, LocalDate inicio) {
        Integer cantidad = plan.getDuracionCantidad();
        UnidadDuracion unidad = plan.getDuracionUnidad();
        if (cantidad == null || unidad == null) {
            return null; // POR_CLASES/PASE sin vigencia configurada: solo se agota por clases.
        }
        return switch (unidad) {
            case DIA -> inicio.plusDays(cantidad);
            case SEMANA -> inicio.plusWeeks(cantidad);
            case MES -> inicio.plusMonths(cantidad);
            case ANIO -> inicio.plusYears(cantidad);
        };
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

    /** Activa cualquier membresia PENDIENTE cuya fecha_inicio ya llego (renovacion anticipada que ya arranco). */
    @Transactional
    public void activarPendientes(Long centroId) {
        var pendientes = centroId != null
                ? membresiaRepository.findPendientesParaActivar(centroId, LocalDate.now())
                : membresiaRepository.findAllPendientesParaActivar(LocalDate.now());
        pendientes.forEach(m -> m.setEstado(EstadoMembresia.ACTIVA));
        membresiaRepository.saveAll(pendientes);
    }

    public MembresiaDto toDto(Membresia m) {
        BigDecimal totalPagado = pagoMembresiaRepository.sumValidoByMembresiaId(m.getId());
        BigDecimal saldo = m.getPrecioFinal().subtract(totalPagado);
        Long diasParaVencer = m.getFechaFin() != null ? ChronoUnit.DAYS.between(LocalDate.now(), m.getFechaFin()) : null;
        boolean renovada = m.getId() != null && membresiaRepository.existsByMembresiaAnteriorId(m.getId());

        List<String> indicadores = new ArrayList<>();
        boolean vigenteOPendiente = m.getEstado() == EstadoMembresia.ACTIVA || m.getEstado() == EstadoMembresia.PENDIENTE;
        if (vigenteOPendiente && diasParaVencer != null && diasParaVencer >= 0 && diasParaVencer <= UMBRAL_DIAS_PROXIMA_A_VENCER) {
            indicadores.add("PROXIMA_A_VENCER");
        }
        if (saldo.signum() > 0) {
            indicadores.add("SALDO_PENDIENTE");
            if (m.getEstado() == EstadoMembresia.VENCIDA) indicadores.add("PAGO_VENCIDO");
        }
        if (m.getClasesRestantes() != null) {
            if (m.getClasesRestantes() <= 0) indicadores.add("AGOTADA");
            else if (m.getClasesRestantes() <= UMBRAL_CLASES_POR_AGOTARSE) indicadores.add("CLASES_POR_AGOTARSE");
        }

        return new MembresiaDto(
                m.getId(), m.getAlumno().getId(), m.getAlumno().getNombre(), m.getPlan().getId(),
                m.getPlanNombreSnapshot(), m.getTipoPlanSnapshot().name(),
                m.getDisciplinas().stream().map(Disciplina::getId).collect(Collectors.toSet()),
                m.getDisciplinas().stream().map(Disciplina::getNombre).collect(Collectors.toSet()),
                m.getFechaInicio(), m.getFechaFin(), m.getNumeroClasesContratadas(), m.getClasesRestantes(),
                m.getPrecioOriginal(), m.getDescuento(), m.getPrecioFinal(), totalPagado, saldo,
                m.getEstado().name(), diasParaVencer, indicadores,
                m.getMembresiaAnterior() != null ? m.getMembresiaAnterior().getId() : null, renovada,
                m.getSuspendidaMotivo(), m.getCanceladaMotivo()
        );
    }
}
