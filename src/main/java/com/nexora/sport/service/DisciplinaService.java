package com.nexora.sport.service;

import com.nexora.sport.dto.DisciplinaDto;
import com.nexora.sport.dto.DisciplinaRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.exception.FieldConflictException;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.Disciplina;
import com.nexora.sport.model.ModalidadDisciplina;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.DisciplinaRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DisciplinaService {

    private final DisciplinaRepository disciplinaRepository;
    private final CentroRepository centroRepository;
    private final TenantScope tenantScope;
    private final NotificacionService notificacionService;

    public DisciplinaService(DisciplinaRepository disciplinaRepository, CentroRepository centroRepository,
                              TenantScope tenantScope, NotificacionService notificacionService) {
        this.disciplinaRepository = disciplinaRepository;
        this.centroRepository = centroRepository;
        this.tenantScope = tenantScope;
        this.notificacionService = notificacionService;
    }

    @Transactional(readOnly = true)
    public PageResponse<DisciplinaDto> listar(Usuario actor, Pageable pageable) {
        return PageResponse.of(disciplinaRepository.findByCentroId(tenantScope.scopeId(actor), pageable), this::toDto);
    }

    @Transactional(readOnly = true)
    public List<DisciplinaDto> listarActivas(Usuario actor) {
        return disciplinaRepository.findByCentroIdAndActivoTrue(tenantScope.scopeId(actor)).stream()
                .map(this::toDto).toList();
    }

    public Disciplina buscar(Long id) {
        return disciplinaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Disciplina no encontrada"));
    }

    @Transactional
    public DisciplinaDto crear(Usuario actor, DisciplinaRequest request) {
        Long centroId = tenantScope.scopeId(actor);
        if (disciplinaRepository.existsByCentroIdAndNombreIgnoreCase(centroId, request.nombre())) {
            throw new FieldConflictException("nombre", "Ya existe una disciplina con ese nombre");
        }
        Disciplina d = new Disciplina();
        d.setCentro(centroRepository.getReferenceById(centroId));
        aplicar(d, request);
        d = disciplinaRepository.save(d);
        notificacionService.notificarAdminDisciplinaNueva(d);
        return toDto(d);
    }

    @Transactional
    public DisciplinaDto actualizar(Long id, DisciplinaRequest request) {
        Disciplina d = buscar(id);
        aplicar(d, request);
        return toDto(disciplinaRepository.save(d));
    }

    @Transactional
    public void desactivar(Long id) {
        Disciplina d = buscar(id);
        d.setActivo(false);
        disciplinaRepository.save(d);
    }

    private void aplicar(Disciplina d, DisciplinaRequest request) {
        d.setNombre(request.nombre());
        d.setDescripcion(request.descripcion());
        d.setIcono(request.icono());
        d.setColor(request.color());
        d.setModalidad(ModalidadDisciplina.valueOf(request.modalidad()));
        d.setLimiteAlumnos(request.limiteAlumnos());
        d.setRequiereInstalacion(request.requiereInstalacion());
    }

    public DisciplinaDto toDto(Disciplina d) {
        return new DisciplinaDto(d.getId(), d.getNombre(), d.getDescripcion(), d.getIcono(), d.getColor(),
                d.getModalidad().name(), d.getLimiteAlumnos(), d.isRequiereInstalacion(), d.isActivo());
    }
}
