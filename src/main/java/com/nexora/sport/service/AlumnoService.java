package com.nexora.sport.service;

import com.nexora.sport.dto.AlumnoDto;
import com.nexora.sport.dto.AlumnoRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.Alumno;
import com.nexora.sport.model.Disciplina;
import com.nexora.sport.model.EstadoAlumno;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.AlumnoRepository;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.DisciplinaRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AlumnoService {

    private final AlumnoRepository alumnoRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final CentroRepository centroRepository;
    private final TenantScope tenantScope;

    public AlumnoService(AlumnoRepository alumnoRepository, DisciplinaRepository disciplinaRepository,
                          CentroRepository centroRepository, TenantScope tenantScope) {
        this.alumnoRepository = alumnoRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.centroRepository = centroRepository;
        this.tenantScope = tenantScope;
    }

    @Transactional(readOnly = true)
    public PageResponse<AlumnoDto> listar(Usuario actor, String q, Pageable pageable) {
        Long centroId = tenantScope.scopeId(actor);
        var page = (q == null || q.isBlank())
                ? alumnoRepository.findByCentroIdAndDeletedAtIsNull(centroId, pageable)
                : alumnoRepository.findByCentroIdAndDeletedAtIsNullAndNombreContainingIgnoreCase(centroId, q, pageable);
        return PageResponse.of(page, this::toDto);
    }

    public Alumno buscar(Long id) {
        return alumnoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));
    }

    @Transactional(readOnly = true)
    public AlumnoDto obtener(Long id) {
        return toDto(buscar(id));
    }

    @Transactional
    public AlumnoDto crear(Usuario actor, AlumnoRequest request) {
        Alumno alumno = new Alumno();
        alumno.setCentro(centroRepository.getReferenceById(tenantScope.scopeId(actor)));
        aplicar(alumno, request);
        return toDto(alumnoRepository.save(alumno));
    }

    @Transactional
    public AlumnoDto actualizar(Long id, AlumnoRequest request) {
        Alumno alumno = buscar(id);
        aplicar(alumno, request);
        return toDto(alumnoRepository.save(alumno));
    }

    @Transactional
    public void desactivar(Long id) {
        Alumno alumno = buscar(id);
        alumno.setEstado(EstadoAlumno.INACTIVO);
        alumno.setFechaBaja(java.time.LocalDateTime.now());
        alumnoRepository.save(alumno);
    }

    private void aplicar(Alumno alumno, AlumnoRequest request) {
        alumno.setNombre(request.nombre());
        alumno.setFechaNacimiento(request.fechaNacimiento());
        alumno.setTelefono(request.telefono());
        alumno.setEmail(request.email());
        alumno.setContactoEmergenciaNombre(request.contactoEmergenciaNombre());
        alumno.setContactoEmergenciaTelefono(request.contactoEmergenciaTelefono());
        alumno.setObservaciones(request.observaciones());
        if (request.estado() != null) {
            EstadoAlumno nuevoEstado = EstadoAlumno.valueOf(request.estado());
            // Registra el momento de la baja mas reciente; no se limpia al reactivarse (ver Alumno#fechaBaja).
            if (nuevoEstado != EstadoAlumno.ACTIVO && alumno.getEstado() == EstadoAlumno.ACTIVO) {
                alumno.setFechaBaja(java.time.LocalDateTime.now());
            }
            alumno.setEstado(nuevoEstado);
        }
        Set<Disciplina> disciplinas = new HashSet<>();
        if (request.disciplinaIds() != null) {
            request.disciplinaIds().forEach(id -> disciplinas.add(disciplinaRepository.getReferenceById(id)));
        }
        alumno.setDisciplinas(disciplinas);
    }

    public AlumnoDto toDto(Alumno a) {
        return new AlumnoDto(
                a.getId(), a.getNombre(), a.getFechaNacimiento(), a.getTelefono(), a.getEmail(),
                a.getContactoEmergenciaNombre(), a.getContactoEmergenciaTelefono(), a.getFotoUrl(),
                a.getObservaciones(), a.getEstado().name(), a.getFechaIngreso(),
                a.getDisciplinas().stream().map(Disciplina::getId).collect(Collectors.toSet()),
                a.getDisciplinas().stream().map(Disciplina::getNombre).collect(Collectors.toSet())
        );
    }
}
