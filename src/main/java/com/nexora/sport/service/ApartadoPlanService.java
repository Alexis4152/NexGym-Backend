package com.nexora.sport.service;

import com.nexora.sport.dto.AlumnoDto;
import com.nexora.sport.dto.AlumnoRequest;
import com.nexora.sport.dto.ApartadoPlanCancelRequest;
import com.nexora.sport.dto.ApartadoPlanDto;
import com.nexora.sport.dto.ApartadoPlanRequest;
import com.nexora.sport.dto.ConvertirApartadoPlanRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.dto.publico.PublicApartadoPlanResponseDto;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.*;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Apartado de un plan de membresia desde la tienda publica: el cliente paga un anticipo
 * del 20% con tarjeta (pago simulado, sin pasarela conectada todavia -- seccion 41 del
 * encargo) para reservar su lugar. Al presentarse, el staff lo CONVIERTE: liga el apartado
 * a un alumno (existente o nuevo) y da de alta la Membresia real, con el anticipo ya
 * registrado como su primer pago (sin volver a cobrarlo ni duplicarlo en Caja).
 */
@Service
public class ApartadoPlanService {

    private static final BigDecimal PORCENTAJE_ANTICIPO = new BigDecimal("0.20");
    private static final int MAX_COINCIDENCIAS_ALUMNO = 8;

    private final ApartadoPlanRepository apartadoPlanRepository;
    private final MembresiaPlanRepository membresiaPlanRepository;
    private final MembresiaRepository membresiaRepository;
    private final CentroRepository centroRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final CajaService cajaService;
    private final MailService mailService;
    private final AlumnoService alumnoService;
    private final MembresiaService membresiaService;
    private final NotificacionService notificacionService;
    private final TenantScope tenantScope;

    public ApartadoPlanService(ApartadoPlanRepository apartadoPlanRepository, MembresiaPlanRepository membresiaPlanRepository,
                                MembresiaRepository membresiaRepository, CentroRepository centroRepository,
                                UsuarioRepository usuarioRepository, AlumnoRepository alumnoRepository,
                                CajaService cajaService, MailService mailService, AlumnoService alumnoService,
                                MembresiaService membresiaService, NotificacionService notificacionService,
                                TenantScope tenantScope) {
        this.apartadoPlanRepository = apartadoPlanRepository;
        this.membresiaPlanRepository = membresiaPlanRepository;
        this.membresiaRepository = membresiaRepository;
        this.centroRepository = centroRepository;
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.cajaService = cajaService;
        this.mailService = mailService;
        this.alumnoService = alumnoService;
        this.membresiaService = membresiaService;
        this.notificacionService = notificacionService;
        this.tenantScope = tenantScope;
    }

    // ---------------------------------------------------------------- publico
    @Transactional
    public PublicApartadoPlanResponseDto crearPublico(String slug, ApartadoPlanRequest request) {
        Centro centro = centroRepository.findBySlugPublicoAndActivoTrue(slug)
                .filter(Centro::isApartadosActivo)
                .orElseThrow(() -> new ResourceNotFoundException("Apartados no disponibles"));
        MembresiaPlan plan = membresiaPlanRepository.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"));
        if (!plan.getCentro().getId().equals(centro.getId()) || !plan.isActivo()) {
            throw new ResourceNotFoundException("Plan no encontrado");
        }
        if (plan.getLimiteAlumnos() != null) {
            long inscritos = membresiaRepository.countVigentesPorPlan(plan.getId());
            if (inscritos >= plan.getLimiteAlumnos()) {
                throw new IllegalStateException("Este plan ya no tiene cupo disponible");
            }
        }

        ApartadoPlan apartado = new ApartadoPlan();
        apartado.setCentro(centro);
        apartado.setPlan(plan);
        apartado.setPlanNombreSnapshot(plan.getNombre());
        apartado.setPrecioPlanSnapshot(plan.getPrecio());
        apartado.setMontoAnticipo(plan.getPrecio().multiply(PORCENTAJE_ANTICIPO).setScale(2, RoundingMode.HALF_UP));
        apartado.setClienteNombre(request.clienteNombre());
        apartado.setClienteTelefono(request.clienteTelefono());
        apartado.setClienteEmail(request.clienteEmail());
        apartado.setNotas(request.notas());
        apartado.setEstado(EstadoApartadoPlan.PENDIENTE);
        apartado = apartadoPlanRepository.save(apartado);

        MovimientoFinanciero movimiento = cajaService.registrarIngresoDeApartadoPlan(centro.getId(), apartado, apartado.getMontoAnticipo());
        apartado.setMovimientoFinancieroId(movimiento.getId());
        apartado = apartadoPlanRepository.save(apartado);

        for (Usuario staff : staffParaNotificar(centro.getId())) {
            mailService.send(staff.getEmail(), "Nueva solicitud de apartado de plan #" + apartado.getId(),
                    "Cliente: " + apartado.getClienteNombre() + " (" + apartado.getClienteTelefono() + ", " + apartado.getClienteEmail() + ")\n"
                            + "Plan: " + apartado.getPlanNombreSnapshot() + " ($" + apartado.getPrecioPlanSnapshot() + ")\n"
                            + "Anticipo pagado con tarjeta: $" + apartado.getMontoAnticipo() + "\n\n"
                            + "Revisa la solicitud desde Apartados o Membresias y da de alta su membresia cuando se presente.");
        }
        notificacionService.notificarApartadoPlanNuevo(apartado);

        return new PublicApartadoPlanResponseDto(apartado.getId(), apartado.getEstado().name(), apartado.getMontoAnticipo(),
                apartado.getPrecioPlanSnapshot(), apartado.getSolicitadoEn());
    }

    // ---------------------------------------------------------------- admin
    @Transactional(readOnly = true)
    public PageResponse<ApartadoPlanDto> listar(Usuario actor, EstadoApartadoPlan estado, LocalDate desde, LocalDate hasta,
                                                 String q, Pageable pageable) {
        Long centroId = tenantScope.scopeId(actor);
        LocalDateTime desdeFecha = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaFecha = hasta != null ? hasta.plusDays(1).atStartOfDay() : null;
        String texto = (q == null || q.isBlank()) ? null : q;
        return PageResponse.of(apartadoPlanRepository.buscar(centroId, estado, desdeFecha, hastaFecha, texto, pageable), this::toDto);
    }

    /** Nunca devuelve un apartado de otro centro (evita adivinar ids ajenos). */
    private ApartadoPlan buscarDelCentro(Long id, Usuario actor) {
        ApartadoPlan apartado = apartadoPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Apartado de plan no encontrado"));
        if (!apartado.getCentro().getId().equals(tenantScope.scopeId(actor))) {
            throw new ResourceNotFoundException("Apartado de plan no encontrado");
        }
        return apartado;
    }

    @Transactional(readOnly = true)
    public ApartadoPlanDto obtener(Long id, Usuario actor) {
        return toDto(buscarDelCentro(id, actor));
    }

    @Transactional(readOnly = true)
    public long contarPendientes(Usuario actor) {
        return apartadoPlanRepository.countByCentroIdAndEstado(tenantScope.scopeId(actor), EstadoApartadoPlan.PENDIENTE);
    }

    @Transactional
    public ApartadoPlanDto cancelar(Long id, Usuario actor, ApartadoPlanCancelRequest request) {
        ApartadoPlan apartado = buscarDelCentro(id, actor);
        if (apartado.getEstado() != EstadoApartadoPlan.PENDIENTE) {
            throw new IllegalStateException("Este apartado ya fue procesado");
        }
        if (apartado.getMovimientoFinancieroId() != null) {
            cajaService.anularMovimiento(apartado.getMovimientoFinancieroId(), actor);
        }
        apartado.setEstado(EstadoApartadoPlan.CANCELADO);
        apartado.setCanceladoPor(actor);
        apartado.setCanceladoEn(LocalDateTime.now());
        apartado.setMotivoCancelacion(request.motivo());
        apartado = apartadoPlanRepository.save(apartado);

        mailService.send(apartado.getClienteEmail(), "Tu apartado de plan #" + apartado.getId() + " fue cancelado",
                "Tu reserva del plan \"" + apartado.getPlanNombreSnapshot() + "\" fue cancelada."
                        + (request.motivo() != null ? ("\nMotivo: " + request.motivo()) : "")
                        + "\nTu anticipo de $" + apartado.getMontoAnticipo() + " te sera reembolsado por el centro.");
        return toDto(apartado);
    }

    /**
     * Alumnos que podrian ser el cliente de este apartado (mismo correo, telefono o nombre
     * parecido), para que el staff lo ligue en vez de crear un duplicado.
     */
    @Transactional(readOnly = true)
    public List<AlumnoDto> alumnosCoincidentes(Long id, Usuario actor) {
        ApartadoPlan a = buscarDelCentro(id, actor);
        String digitos = a.getClienteTelefono() == null ? "" : a.getClienteTelefono().replaceAll("\\D", "");
        String telefono = digitos.length() >= 7 ? digitos.substring(Math.max(0, digitos.length() - 10)) : "";
        String email = a.getClienteEmail() == null ? "" : a.getClienteEmail().trim();
        // Por nombre solo con el primer nombre + primer apellido para no empatar de mas con
        // nombres cortos tipo "Ana".
        String[] partes = a.getClienteNombre().trim().split("\\s+");
        String nombre = partes.length >= 2 ? partes[0] + " " + partes[1] : "";
        return alumnoRepository.buscarCoincidencias(a.getCentro().getId(), email, telefono, nombre,
                        PageRequest.of(0, MAX_COINCIDENCIAS_ALUMNO)).stream()
                .map(alumnoService::toDto)
                .toList();
    }

    /**
     * Convierte el apartado: resuelve el alumno (existente o nuevo), da de alta la membresia
     * con el anticipo como primer pago y deja el apartado CONVERTIDO y ligado a ambos.
     */
    @Transactional
    public ApartadoPlanDto convertir(Long id, Usuario actor, ConvertirApartadoPlanRequest request) {
        ApartadoPlan apartado = buscarDelCentro(id, actor);
        if (apartado.getEstado() != EstadoApartadoPlan.PENDIENTE) {
            throw new IllegalStateException("Este apartado ya fue procesado");
        }
        boolean existente = request.alumnoId() != null;
        if (existente == (request.nuevoAlumno() != null)) {
            throw new IllegalArgumentException("Selecciona un alumno existente o captura los datos del alumno nuevo");
        }

        Alumno alumno;
        if (existente) {
            alumno = alumnoRepository.findById(request.alumnoId())
                    .filter(al -> al.getDeletedAt() == null && al.getCentro().getId().equals(apartado.getCentro().getId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));
        } else {
            ConvertirApartadoPlanRequest.NuevoAlumno n = request.nuevoAlumno();
            Set<Long> disciplinasInteres = request.disciplinaIds() != null ? request.disciplinaIds() : Set.of();
            AlumnoDto creado = alumnoService.crear(actor, new AlumnoRequest(
                    n.nombre().trim(), n.fechaNacimiento(), blankToNull(n.telefono()), blankToNull(n.email()),
                    null, null, "Alta desde apartado en linea #" + apartado.getId(), null,
                    disciplinasInteres, n.sucursalId()));
            alumno = alumnoRepository.findById(creado.id()).orElseThrow();
        }

        Membresia membresia = membresiaService.crearDesdeApartadoPlan(actor, alumno, apartado.getPlan(),
                request.fechaInicio(), request.descuento(), request.disciplinaIds(),
                apartado.getMontoAnticipo(), apartado.getMovimientoFinancieroId(),
                request.montoPagoAdicional(), request.metodoPago(), request.sucursalPreferidaId());

        apartado.setEstado(EstadoApartadoPlan.CONVERTIDO);
        apartado.setConvertidoPor(actor);
        apartado.setConvertidoEn(LocalDateTime.now());
        apartado.setAlumno(alumno);
        apartado.setMembresiaId(membresia.getId());
        return toDto(apartadoPlanRepository.save(apartado));
    }

    // ---------------------------------------------------------------- helpers
    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private List<Usuario> staffParaNotificar(Long centroId) {
        return usuarioRepository.findActivosConSeccion(centroId, Seccion.MEMBRESIAS).stream()
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .toList();
    }

    public ApartadoPlanDto toDto(ApartadoPlan a) {
        return new ApartadoPlanDto(
                a.getId(), a.getClienteNombre(), a.getClienteTelefono(), a.getClienteEmail(), a.getNotas(),
                a.getEstado().name(), a.getPlan() != null ? a.getPlan().getId() : null,
                a.getPlanNombreSnapshot(), a.getPrecioPlanSnapshot(), a.getMontoAnticipo(),
                a.getSolicitadoEn(),
                a.getConvertidoPor() != null ? a.getConvertidoPor().getNombre() : null, a.getConvertidoEn(),
                a.getCanceladoPor() != null ? a.getCanceladoPor().getNombre() : null, a.getCanceladoEn(), a.getMotivoCancelacion(),
                a.getAlumno() != null ? a.getAlumno().getId() : null,
                a.getAlumno() != null ? a.getAlumno().getNombre() : null,
                a.getMembresiaId()
        );
    }
}
