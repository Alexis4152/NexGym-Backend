package com.nexora.sport.service;

import com.nexora.sport.dto.ClaseDto;
import com.nexora.sport.dto.ClaseRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.Clase;
import com.nexora.sport.model.DiaSemana;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.ClaseRepository;
import com.nexora.sport.repository.DisciplinaRepository;
import com.nexora.sport.repository.InstructorRepository;
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
    private final CentroRepository centroRepository;
    private final TenantScope tenantScope;

    public ClaseService(ClaseRepository claseRepository, DisciplinaRepository disciplinaRepository,
                         InstructorRepository instructorRepository, CentroRepository centroRepository,
                         TenantScope tenantScope) {
        this.claseRepository = claseRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.instructorRepository = instructorRepository;
        this.centroRepository = centroRepository;
        this.tenantScope = tenantScope;
    }

    public PageResponse<ClaseDto> listar(Usuario actor, Pageable pageable) {
        return PageResponse.of(claseRepository.findByCentroId(tenantScope.scopeId(actor), pageable), this::toDto);
    }

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
        aplicar(clase, request);
        return toDto(claseRepository.save(clase));
    }

    @Transactional
    public ClaseDto actualizar(Long id, ClaseRequest request) {
        Clase clase = buscar(id);
        aplicar(clase, request);
        return toDto(claseRepository.save(clase));
    }

    @Transactional
    public void desactivar(Long id) {
        Clase clase = buscar(id);
        clase.setActivo(false);
        claseRepository.save(clase);
    }

    private void aplicar(Clase clase, ClaseRequest request) {
        clase.setDisciplina(disciplinaRepository.getReferenceById(request.disciplinaId()));
        clase.setInstructor(request.instructorId() != null ? instructorRepository.getReferenceById(request.instructorId()) : null);
        clase.setDiaSemana(DiaSemana.valueOf(request.diaSemana()));
        clase.setHoraInicio(request.horaInicio());
        clase.setHoraFin(request.horaFin());
        clase.setLugar(request.lugar());
        clase.setCapacidadMaxima(request.capacidadMaxima());
    }

    public ClaseDto toDto(Clase c) {
        return new ClaseDto(
                c.getId(), c.getDisciplina().getId(), c.getDisciplina().getNombre(),
                c.getInstructor() != null ? c.getInstructor().getId() : null,
                c.getInstructor() != null ? c.getInstructor().getNombre() : null,
                c.getDiaSemana().name(), c.getHoraInicio(), c.getHoraFin(), c.getLugar(),
                c.getCapacidadMaxima(), c.isActivo()
        );
    }
}
