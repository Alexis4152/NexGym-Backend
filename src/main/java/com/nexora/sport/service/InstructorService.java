package com.nexora.sport.service;

import com.nexora.sport.dto.InstructorDto;
import com.nexora.sport.dto.InstructorRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.Disciplina;
import com.nexora.sport.model.Instructor;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.DisciplinaRepository;
import com.nexora.sport.repository.InstructorRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InstructorService {

    private final InstructorRepository instructorRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final CentroRepository centroRepository;
    private final TenantScope tenantScope;

    public InstructorService(InstructorRepository instructorRepository, DisciplinaRepository disciplinaRepository,
                              CentroRepository centroRepository, TenantScope tenantScope) {
        this.instructorRepository = instructorRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.centroRepository = centroRepository;
        this.tenantScope = tenantScope;
    }

    @Transactional(readOnly = true)
    public PageResponse<InstructorDto> listar(Usuario actor, String q, Pageable pageable) {
        Long centroId = tenantScope.scopeId(actor);
        var page = (q == null || q.isBlank())
                ? instructorRepository.findByCentroId(centroId, pageable)
                : instructorRepository.findByCentroIdAndNombreContainingIgnoreCase(centroId, q, pageable);
        return PageResponse.of(page, this::toDto);
    }

    public Instructor buscar(Long id) {
        return instructorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Instructor no encontrado"));
    }

    @Transactional
    public InstructorDto crear(Usuario actor, InstructorRequest request) {
        Instructor instructor = new Instructor();
        instructor.setCentro(centroRepository.getReferenceById(tenantScope.scopeId(actor)));
        aplicar(instructor, request);
        return toDto(instructorRepository.save(instructor));
    }

    @Transactional
    public InstructorDto actualizar(Long id, InstructorRequest request) {
        Instructor instructor = buscar(id);
        aplicar(instructor, request);
        return toDto(instructorRepository.save(instructor));
    }

    @Transactional
    public void desactivar(Long id) {
        Instructor instructor = buscar(id);
        instructor.setActivo(false);
        instructorRepository.save(instructor);
    }

    private void aplicar(Instructor instructor, InstructorRequest request) {
        instructor.setNombre(request.nombre());
        instructor.setTelefono(request.telefono());
        instructor.setEmail(request.email());
        instructor.setEspecialidad(request.especialidad());
        Set<Disciplina> disciplinas = new HashSet<>();
        if (request.disciplinaIds() != null) {
            request.disciplinaIds().forEach(id -> disciplinas.add(disciplinaRepository.getReferenceById(id)));
        }
        instructor.setDisciplinas(disciplinas);
    }

    public InstructorDto toDto(Instructor i) {
        return new InstructorDto(
                i.getId(), i.getNombre(), i.getTelefono(), i.getEmail(), i.getEspecialidad(), i.getFotoUrl(),
                i.isActivo(),
                i.getDisciplinas().stream().map(Disciplina::getId).collect(Collectors.toSet()),
                i.getDisciplinas().stream().map(Disciplina::getNombre).collect(Collectors.toSet())
        );
    }
}
