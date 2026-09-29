package com.nexora.sport.service;

import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.dto.ReservaDto;
import com.nexora.sport.dto.ReservaRequest;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.AlumnoRepository;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.ClaseRepository;
import com.nexora.sport.repository.ReservaRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final ClaseRepository claseRepository;
    private final AlumnoRepository alumnoRepository;
    private final CentroRepository centroRepository;
    private final TenantScope tenantScope;
    private final NotificacionService notificacionService;

    public ReservaService(ReservaRepository reservaRepository, ClaseRepository claseRepository,
                           AlumnoRepository alumnoRepository, CentroRepository centroRepository,
                           TenantScope tenantScope, NotificacionService notificacionService) {
        this.reservaRepository = reservaRepository;
        this.claseRepository = claseRepository;
        this.alumnoRepository = alumnoRepository;
        this.centroRepository = centroRepository;
        this.tenantScope = tenantScope;
        this.notificacionService = notificacionService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservaDto> listar(Usuario actor, Pageable pageable) {
        return PageResponse.of(reservaRepository.findByCentroId(tenantScope.scopeId(actor), pageable), this::toDto);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservaDto> listarPorAlumno(Long alumnoId, Pageable pageable) {
        return PageResponse.of(reservaRepository.findByAlumnoId(alumnoId, pageable), this::toDto);
    }

    @Transactional
    public ReservaDto crear(Usuario actor, ReservaRequest request) {
        Clase clase = claseRepository.findById(request.claseId())
                .orElseThrow(() -> new ResourceNotFoundException("Clase no encontrada"));
        Alumno alumno = alumnoRepository.findById(request.alumnoId())
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));

        if (reservaRepository.existsByClaseIdAndAlumnoIdAndFecha(clase.getId(), alumno.getId(), request.fecha())) {
            throw new IllegalStateException("El alumno ya tiene una reserva para esa clase y fecha");
        }
        if (reservaRepository.existeConflictoHorarioAlumno(alumno.getId(), request.fecha(),
                clase.getHoraInicio(), clase.getHoraFin(), EstadoReserva.CANCELADA)) {
            throw new IllegalStateException("El alumno ya tiene otra clase inscrita en ese mismo horario");
        }
        long ocupadas = reservaRepository.countByClaseIdAndFechaAndEstadoNot(clase.getId(), request.fecha(), EstadoReserva.CANCELADA);
        if (ocupadas >= clase.getCapacidadMaxima()) {
            throw new IllegalStateException("La clase ya alcanzo su capacidad maxima para esa fecha");
        }

        Reserva reserva = new Reserva();
        reserva.setCentro(centroRepository.getReferenceById(tenantScope.scopeId(actor)));
        reserva.setClase(clase);
        reserva.setAlumno(alumno);
        reserva.setFecha(request.fecha());
        reserva = reservaRepository.save(reserva);

        notificacionService.notificarReservaConfirmada(reserva);
        if (ocupadas + 1 == clase.getCapacidadMaxima()) {
            notificacionService.notificarAdminCupoLleno(clase, request.fecha());
        }
        return toDto(reserva);
    }

    @Transactional
    public ReservaDto cambiarEstado(Long id, EstadoReserva estado) {
        Reserva reserva = buscar(id);
        EstadoReserva anterior = reserva.getEstado();
        reserva.setEstado(estado);
        reserva = reservaRepository.save(reserva);
        if (estado == EstadoReserva.CANCELADA && anterior != EstadoReserva.CANCELADA) {
            notificacionService.notificarReservaCancelada(reserva);
        }
        return toDto(reserva);
    }

    public Reserva buscar(Long id) {
        return reservaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
    }

    public ReservaDto toDto(Reserva r) {
        return new ReservaDto(
                r.getId(), r.getClase().getId(), r.getClase().getDisciplina().getNombre(),
                r.getClase().getDiaSemana().name() + " " + r.getClase().getHoraInicio(),
                r.getAlumno().getId(), r.getAlumno().getNombre(), r.getFecha(), r.getEstado().name()
        );
    }
}
