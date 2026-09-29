package com.nexora.sport.service;

import com.nexora.sport.dto.ClaseDisponibleDto;
import com.nexora.sport.dto.ClaseDto;
import com.nexora.sport.dto.ClaseRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.Clase;
import com.nexora.sport.model.DiaSemana;
import com.nexora.sport.model.Disciplina;
import com.nexora.sport.model.EstadoReserva;
import com.nexora.sport.model.Instructor;
import com.nexora.sport.model.Lugar;
import com.nexora.sport.model.Reserva;
import com.nexora.sport.model.Sucursal;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.ClaseRepository;
import com.nexora.sport.repository.DisciplinaRepository;
import com.nexora.sport.repository.InstructorRepository;
import com.nexora.sport.repository.LugarRepository;
import com.nexora.sport.repository.ReservaRepository;
import com.nexora.sport.repository.SucursalRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class ClaseService {

    private final ClaseRepository claseRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final InstructorRepository instructorRepository;
    private final LugarRepository lugarRepository;
    private final SucursalRepository sucursalRepository;
    private final CentroRepository centroRepository;
    private final ReservaRepository reservaRepository;
    private final TenantScope tenantScope;
    private final NotificacionService notificacionService;

    public ClaseService(ClaseRepository claseRepository, DisciplinaRepository disciplinaRepository,
                         InstructorRepository instructorRepository, LugarRepository lugarRepository,
                         SucursalRepository sucursalRepository, CentroRepository centroRepository,
                         ReservaRepository reservaRepository, TenantScope tenantScope,
                         NotificacionService notificacionService) {
        this.claseRepository = claseRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.instructorRepository = instructorRepository;
        this.lugarRepository = lugarRepository;
        this.sucursalRepository = sucursalRepository;
        this.centroRepository = centroRepository;
        this.reservaRepository = reservaRepository;
        this.tenantScope = tenantScope;
        this.notificacionService = notificacionService;
    }

    /** Filtra por la SUCURSAL ACTIVA (una sola, ver TenantScope#sucursalActivaId) --
     * no por todas las autorizadas: si Carlos administra Centro+Norte pero esta
     * trabajando en Norte, aqui solo debe ver las clases de Norte. Para cambiar que
     * vera, el frontend cambia de sucursal activa, no hace falta otro endpoint. */
    @Transactional(readOnly = true)
    public PageResponse<ClaseDto> listar(Usuario actor, Pageable pageable) {
        Long centroId = tenantScope.scopeId(actor);
        Long sucursalActiva = tenantScope.sucursalActivaId(actor);
        Set<Long> sucursalIds = sucursalActiva == null ? null : Set.of(sucursalActiva);
        Long instructorId = instructorPropioSiAplica(actor);
        return PageResponse.of(claseRepository.buscarEnAlcance(centroId, sucursalIds, instructorId, pageable), this::toDto);
    }

    /** Si el actor es OPERATIVO y su cuenta esta ligada a una ficha de Instructor, acota
     * las clases a las suyas (Caso 5/13 del encargo: "Instructor A no ve clases de B").
     * Dueno/Supervisor/Admin nunca se acotan asi, aunque tengan una ficha de instructor. */
    private Long instructorPropioSiAplica(Usuario actor) {
        if (tenantScope.isAdminOSuperior(actor)) return null;
        return instructorRepository.findByUsuarioId(actor.getId()).map(Instructor::getId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ClaseDto> listarActivas(Usuario actor) {
        return claseRepository.findByCentroIdAndActivoTrue(tenantScope.scopeId(actor)).stream().map(this::toDto).toList();
    }

    /** Disponibilidad de mostrador (seccion 26 del encargo de jerarquias): a diferencia
     * de {@link #listar}, esto ignora a proposito el alcance por sucursal del actor -- un
     * Admin/Operativo acotado a Sucursal X necesita ver el cupo de TODAS las sucursales
     * del centro para poder decirle a un cliente "en tal otra sucursal si hay lugar",
     * sin exponer instructor ni roster (ver ClaseDisponibleDto). */
    @Transactional(readOnly = true)
    public List<ClaseDisponibleDto> disponibilidad(Usuario actor) {
        Long centroId = tenantScope.scopeId(actor);
        return claseRepository.findByCentroIdAndActivoTrue(centroId).stream()
                .map(c -> {
                    LocalDate proximaFecha = proximaOcurrencia(c.getDiaSemana());
                    long reservados = reservaRepository.countByClaseIdAndFechaAndEstadoNot(
                            c.getId(), proximaFecha, EstadoReserva.CANCELADA);
                    long disponible = Math.max(0, c.getCapacidadMaxima() - reservados);
                    return new ClaseDisponibleDto(
                            c.getId(), c.getSucursal().getNombre(), c.getDisciplina().getNombre(),
                            c.getDiaSemana().name(), String.valueOf(c.getHoraInicio()), String.valueOf(c.getHoraFin()),
                            c.getCapacidadMaxima(), reservados, disponible);
                })
                .sorted((a, b) -> a.sucursalNombre().compareTo(b.sucursalNombre()))
                .toList();
    }

    /** Proxima fecha (hoy inclusive) en la que cae ese dia de la semana. */
    private LocalDate proximaOcurrencia(DiaSemana dia) {
        LocalDate hoy = LocalDate.now();
        java.time.DayOfWeek objetivo = java.time.DayOfWeek.valueOf(mapDia(dia));
        int diff = (objetivo.getValue() - hoy.getDayOfWeek().getValue() + 7) % 7;
        return hoy.plusDays(diff);
    }

    private String mapDia(DiaSemana dia) {
        return switch (dia) {
            case LUNES -> "MONDAY";
            case MARTES -> "TUESDAY";
            case MIERCOLES -> "WEDNESDAY";
            case JUEVES -> "THURSDAY";
            case VIERNES -> "FRIDAY";
            case SABADO -> "SATURDAY";
            case DOMINGO -> "SUNDAY";
        };
    }

    public Clase buscar(Long id) {
        return claseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Clase no encontrada"));
    }

    @Transactional
    public ClaseDto crear(Usuario actor, ClaseRequest request) {
        Clase clase = new Clase();
        clase.setCentro(centroRepository.getReferenceById(tenantScope.scopeId(actor)));
        aplicar(actor, clase, request, null);
        return toDto(claseRepository.save(clase));
    }

    @Transactional
    public ClaseDto actualizar(Usuario actor, Long id, ClaseRequest request) {
        Clase clase = buscar(id);
        var horaInicioAnterior = clase.getHoraInicio();
        var horaFinAnterior = clase.getHoraFin();
        Long instructorAnteriorId = clase.getInstructor() != null ? clase.getInstructor().getId() : null;

        aplicar(actor, clase, request, id);
        clase = claseRepository.save(clase);

        notificarCambiosAFuturasReservas(clase, horaInicioAnterior, horaFinAnterior, instructorAnteriorId);
        return toDto(clase);
    }

    private void notificarCambiosAFuturasReservas(Clase clase, java.time.LocalTime horaInicioAnterior,
                                                    java.time.LocalTime horaFinAnterior, Long instructorAnteriorId) {
        boolean cambioHorario = !clase.getHoraInicio().equals(horaInicioAnterior) || !clase.getHoraFin().equals(horaFinAnterior);
        Long instructorNuevoId = clase.getInstructor() != null ? clase.getInstructor().getId() : null;
        boolean cambioInstructor = !Objects.equals(instructorAnteriorId, instructorNuevoId);
        if (!cambioHorario && !cambioInstructor) return;

        List<Reserva> afectadas = reservaRepository.findByClaseIdAndEstadoAndFechaGreaterThanEqual(
                clase.getId(), EstadoReserva.RESERVADA, LocalDate.now());
        for (Reserva r : afectadas) {
            if (cambioHorario) {
                notificacionService.notificarClaseHorarioCambiado(r, String.valueOf(horaInicioAnterior), String.valueOf(clase.getHoraInicio()));
            }
            if (cambioInstructor) {
                Instructor nuevo = clase.getInstructor();
                notificacionService.notificarClaseInstructorCambiado(r, nuevo != null ? nuevo.getNombre() : null);
            }
        }
    }

    @Transactional
    public void desactivar(Long id) {
        Clase clase = buscar(id);
        clase.setActivo(false);
        claseRepository.save(clase);

        // Cancela en cascada las reservas futuras y avisa a cada alumno (seccion 11.C del encargo).
        List<Reserva> afectadas = reservaRepository.findByClaseIdAndEstadoAndFechaGreaterThanEqual(
                clase.getId(), EstadoReserva.RESERVADA, LocalDate.now());
        for (Reserva r : afectadas) {
            r.setEstado(EstadoReserva.CANCELADA);
        }
        reservaRepository.saveAll(afectadas);
        for (Reserva r : afectadas) {
            notificacionService.notificarClaseCanceladaPorReserva(r);
        }
    }

    private void aplicar(Usuario actor, Clase clase, ClaseRequest request, Long excludeId) {
        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
        }
        if (request.capacidadMaxima() <= 0) {
            throw new IllegalArgumentException("El cupo maximo debe ser mayor a cero");
        }

        Sucursal sucursal = sucursalRepository.findById(request.sucursalId())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
        if (!tenantScope.sucursalPermite(actor, sucursal.getId())) {
            throw new ResourceNotFoundException("Sucursal no encontrada");
        }
        Disciplina disciplina = disciplinaRepository.findById(request.disciplinaId())
                .orElseThrow(() -> new ResourceNotFoundException("Disciplina no encontrada"));

        Lugar lugar = null;
        if (request.lugarId() != null) {
            lugar = lugarRepository.findById(request.lugarId())
                    .orElseThrow(() -> new ResourceNotFoundException("Instalacion no encontrada"));
            if (!lugar.isActivo()) {
                throw new IllegalArgumentException("La instalacion seleccionada no esta activa");
            }
            if (!lugar.getSucursal().getId().equals(sucursal.getId())) {
                throw new IllegalArgumentException("La instalacion no pertenece a la sucursal seleccionada");
            }
            boolean compatible = lugar.getDisciplinas().isEmpty()
                    || lugar.getDisciplinas().stream().anyMatch(d -> d.getId().equals(disciplina.getId()));
            if (!compatible) {
                throw new IllegalArgumentException("La instalacion no esta habilitada para esta disciplina");
            }
            if (lugar.getCapacidadMaxima() != null && request.capacidadMaxima() > lugar.getCapacidadMaxima()
                    && !tenantScope.isAdminOSuperior(actor)) {
                throw new IllegalArgumentException(
                        "El cupo excede la capacidad de la instalacion (" + lugar.getCapacidadMaxima()
                                + "); solo un administrador puede autorizarlo");
            }
        } else if (disciplina.isRequiereInstalacion()) {
            throw new IllegalArgumentException("Esta disciplina requiere seleccionar una instalacion");
        }

        DiaSemana dia = DiaSemana.valueOf(request.diaSemana());
        if (lugar != null && claseRepository.existeConflictoLugar(
                lugar.getId(), dia, request.horaInicio(), request.horaFin(), excludeId)) {
            throw new IllegalArgumentException("Ya existe otra clase en esa instalacion en ese horario");
        }
        Instructor instructor = null;
        if (request.instructorId() != null) {
            if (claseRepository.existeConflictoInstructor(
                    request.instructorId(), dia, request.horaInicio(), request.horaFin(), excludeId)) {
                throw new IllegalArgumentException("El instructor ya tiene otra clase asignada en ese horario");
            }
            instructor = instructorRepository.findById(request.instructorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Instructor no encontrado"));
            boolean disponibleEnSucursal = instructor.getSucursales().isEmpty()
                    || instructor.getSucursales().stream().anyMatch(s -> s.getId().equals(sucursal.getId()));
            if (!disponibleEnSucursal) {
                throw new IllegalArgumentException("Este instructor no esta disponible en la sucursal seleccionada");
            }
        }

        clase.setSucursal(sucursal);
        clase.setDisciplina(disciplina);
        clase.setInstructor(instructor);
        clase.setDiaSemana(dia);
        clase.setHoraInicio(request.horaInicio());
        clase.setHoraFin(request.horaFin());
        clase.setLugar(lugar);
        clase.setCapacidadMaxima(request.capacidadMaxima());
    }

    public ClaseDto toDto(Clase c) {
        return new ClaseDto(
                c.getId(), c.getSucursal().getId(), c.getSucursal().getNombre(),
                c.getDisciplina().getId(), c.getDisciplina().getNombre(),
                c.getInstructor() != null ? c.getInstructor().getId() : null,
                c.getInstructor() != null ? c.getInstructor().getNombre() : null,
                c.getDiaSemana().name(), c.getHoraInicio(), c.getHoraFin(),
                c.getLugar() != null ? c.getLugar().getId() : null,
                c.getLugar() != null ? c.getLugar().getNombre() : null,
                c.getLugar() != null ? c.getLugar().getDireccion() : null,
                c.getCapacidadMaxima(), c.isActivo()
        );
    }
}
