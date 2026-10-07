package com.nexora.sport.service;

import com.nexora.sport.dto.ApartadoPlanCancelRequest;
import com.nexora.sport.dto.ApartadoPlanDto;
import com.nexora.sport.dto.ApartadoPlanRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.dto.publico.PublicApartadoPlanResponseDto;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.*;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Apartado de un plan de membresia desde la tienda publica: el cliente paga un anticipo
 * del 20% con tarjeta (pago simulado, sin pasarela conectada todavia -- seccion 41 del
 * encargo) para reservar su lugar; el staff de Membresias completa despues el alta real
 * (Membresia) cuando el cliente se presenta. Mismo patron general que ApartadoService
 * (productos), pero sin descuento de stock/cupo: solo folio + anticipo ya cobrado.
 */
@Service
public class ApartadoPlanService {

    private static final BigDecimal PORCENTAJE_ANTICIPO = new BigDecimal("0.20");

    private final ApartadoPlanRepository apartadoPlanRepository;
    private final MembresiaPlanRepository membresiaPlanRepository;
    private final MembresiaRepository membresiaRepository;
    private final CentroRepository centroRepository;
    private final UsuarioRepository usuarioRepository;
    private final CajaService cajaService;
    private final MailService mailService;
    private final TenantScope tenantScope;

    public ApartadoPlanService(ApartadoPlanRepository apartadoPlanRepository, MembresiaPlanRepository membresiaPlanRepository,
                                MembresiaRepository membresiaRepository, CentroRepository centroRepository,
                                UsuarioRepository usuarioRepository, CajaService cajaService, MailService mailService,
                                TenantScope tenantScope) {
        this.apartadoPlanRepository = apartadoPlanRepository;
        this.membresiaPlanRepository = membresiaPlanRepository;
        this.membresiaRepository = membresiaRepository;
        this.centroRepository = centroRepository;
        this.usuarioRepository = usuarioRepository;
        this.cajaService = cajaService;
        this.mailService = mailService;
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
                            + "Revisa la solicitud desde el panel de Apartados y da de alta su membresia cuando se presente.");
        }

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

    public ApartadoPlan buscar(Long id) {
        return apartadoPlanRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Apartado de plan no encontrado"));
    }

    @Transactional(readOnly = true)
    public ApartadoPlanDto obtener(Long id) {
        return toDto(buscar(id));
    }

    @Transactional
    public ApartadoPlanDto cancelar(Long id, Usuario actor, ApartadoPlanCancelRequest request) {
        ApartadoPlan apartado = buscar(id);
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

    @Transactional
    public ApartadoPlanDto convertir(Long id, Usuario actor) {
        ApartadoPlan apartado = buscar(id);
        if (apartado.getEstado() != EstadoApartadoPlan.PENDIENTE) {
            throw new IllegalStateException("Este apartado ya fue procesado");
        }
        apartado.setEstado(EstadoApartadoPlan.CONVERTIDO);
        apartado.setConvertidoPor(actor);
        apartado.setConvertidoEn(LocalDateTime.now());
        return toDto(apartadoPlanRepository.save(apartado));
    }

    // ---------------------------------------------------------------- helpers
    private List<Usuario> staffParaNotificar(Long centroId) {
        return usuarioRepository.findActivosConSeccion(centroId, Seccion.MEMBRESIAS).stream()
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .toList();
    }

    public ApartadoPlanDto toDto(ApartadoPlan a) {
        return new ApartadoPlanDto(
                a.getId(), a.getClienteNombre(), a.getClienteTelefono(), a.getClienteEmail(), a.getNotas(),
                a.getEstado().name(), a.getPlanNombreSnapshot(), a.getPrecioPlanSnapshot(), a.getMontoAnticipo(),
                a.getSolicitadoEn(),
                a.getConvertidoPor() != null ? a.getConvertidoPor().getNombre() : null, a.getConvertidoEn(),
                a.getCanceladoPor() != null ? a.getCanceladoPor().getNombre() : null, a.getCanceladoEn(), a.getMotivoCancelacion()
        );
    }
}
