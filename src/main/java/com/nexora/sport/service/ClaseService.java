package com.nexora.sport.service;

import com.nexora.sport.dto.ClaseDto;
import com.nexora.sport.dto.ClaseRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.Clase;
import com.nexora.sport.model.DiaSemana;
import com.nexora.sport.model.Disciplina;
import com.nexora.sport.model.Lugar;
import com.nexora.sport.model.Sucursal;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.ClaseRepository;
import com.nexora.sport.repository.DisciplinaRepository;
import com.nexora.sport.repository.InstructorRepository;
import com.nexora.sport.repository.LugarRepository;
import com.nexora.sport.repository.SucursalRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClaseService {

    private final ClaseRepository claseRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final InstructorRepository instructorRepository;
    private final LugarRepository lugarRepository;
    private final SucursalRepository sucursalRepository;
    private final CentroRepository centroRepository;
    private final TenantScope tenantScope;

    public ClaseService(ClaseRepository claseRepository, DisciplinaRepository disciplinaRepository,
                         InstructorRepository instructorRepository, LugarRepository lugarRepository,
                         SucursalRepository sucursalRepository, CentroRepository centroRepository,
                         TenantScope tenantScope) {
        this.claseRepository = claseRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.instructorRepository = instructorRepository;
        this.lugarRepository = lugarRepository;
        this.sucursalRepository = sucursalRepository;
        this.centroRepository = centroRepository;
        this.tenantScope = tenantScope;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClaseDto> listar(Usuario actor, Pageable pageable) {
        return PageResponse.of(claseRepository.findByCentroId(tenantScope.scopeId(actor), pageable), this::toDto);
    }

    @Transactional(readOnly = true)
    public List<ClaseDto> listarActivas(Usuario actor) {
        return claseRepository.findByCentroIdAndActivoTrue(tenantScope.scopeId(actor)).stream().map(this::toDto).toList();
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
        aplicar(actor, clase, request, id);
        return toDto(claseRepository.save(clase));
    }

    @Transactional
    public void desactivar(Long id) {
        Clase clase = buscar(id);
        clase.setActivo(false);
        claseRepository.save(clase);
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
        if (request.instructorId() != null && claseRepository.existeConflictoInstructor(
                request.instructorId(), dia, request.horaInicio(), request.horaFin(), excludeId)) {
            throw new IllegalArgumentException("El instructor ya tiene otra clase asignada en ese horario");
        }

        clase.setSucursal(sucursal);
        clase.setDisciplina(disciplina);
        clase.setInstructor(request.instructorId() != null ? instructorRepository.getReferenceById(request.instructorId()) : null);
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
