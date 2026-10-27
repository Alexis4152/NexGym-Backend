package com.nexora.sport.service;

import com.nexora.sport.dto.AsistenciaDto;
import com.nexora.sport.dto.AsistenciaRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.*;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private final AlumnoRepository alumnoRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final ClaseRepository claseRepository;
    private final CentroRepository centroRepository;
    private final TenantScope tenantScope;
    private final MembresiaService membresiaService;
    private final PagoMembresiaService pagoMembresiaService;
    private final AlcanceInstructorService alcanceInstructor;

    public AsistenciaService(AsistenciaRepository asistenciaRepository, AlumnoRepository alumnoRepository,
                              DisciplinaRepository disciplinaRepository, ClaseRepository claseRepository,
                              CentroRepository centroRepository, TenantScope tenantScope,
                              MembresiaService membresiaService, PagoMembresiaService pagoMembresiaService,
                              AlcanceInstructorService alcanceInstructor) {
        this.alcanceInstructor = alcanceInstructor;
        this.asistenciaRepository = asistenciaRepository;
        this.alumnoRepository = alumnoRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.claseRepository = claseRepository;
        this.centroRepository = centroRepository;
        this.tenantScope = tenantScope;
        this.membresiaService = membresiaService;
        this.pagoMembresiaService = pagoMembresiaService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AsistenciaDto> listar(Usuario actor, Pageable pageable) {
        Long centroId = tenantScope.scopeId(actor);
        var instructor = alcanceInstructor.instructorAcotado(actor);
        if (instructor.isPresent()) {
            var clases = alcanceInstructor.clasesActivas(instructor.get());
            if (clases.isEmpty()) return PageResponse.of(org.springframework.data.domain.Page.empty(pageable), this::toDto);
            var claseIds = clases.stream().map(Clase::getId).toList();
            var disciplinaIds = clases.stream().map(c -> c.getDisciplina().getId()).distinct().toList();
            return PageResponse.of(asistenciaRepository.buscarDeInstructor(centroId, claseIds, disciplinaIds, pageable), this::toDto);
        }
        return PageResponse.of(asistenciaRepository.findByCentroId(centroId, pageable), this::toDto);
    }

    public long contarHoy(Long centroId) {
        return asistenciaRepository.countByCentroIdAndFecha(centroId, LocalDate.now());
    }

    /**
     * Registra una asistencia SOLO si el alumno tiene una membresia vigente que cubra la
     * disciplina (ver MembresiaService#resolverParaAsistencia). Si la compro hoy y todavia
     * habia clase disponible, esta asistencia es la que arranca su vigencia. En planes por
     * numero de clases descuenta una. Respeta Centro#permitirAccesoConAdeudo.
     */
    @Transactional
    public AsistenciaDto registrar(Usuario actor, AsistenciaRequest request) {
        Long centroId = tenantScope.scopeId(actor);
        Centro centro = centroRepository.findById(centroId).orElseThrow();
        Alumno alumno = alumnoRepository.findById(request.alumnoId())
                .filter(al -> al.getCentro().getId().equals(centroId) && al.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));
        var instructor = alcanceInstructor.instructorAcotado(actor);
        if (instructor.isPresent() && !alcanceInstructor.esAlumnoDeSusClases(instructor.get(), centroId, alumno.getId())) {
            throw new IllegalStateException(alumno.getNombre() + " no esta registrado a tus clases");
        }
        Clase clase = null;
        if (request.claseId() != null) {
            clase = claseRepository.findById(request.claseId())
                    .filter(c -> c.getCentro().getId().equals(centroId))
                    .orElseThrow(() -> new ResourceNotFoundException("Clase no encontrada"));
        }
        Disciplina disciplina = request.disciplinaId() != null
                ? disciplinaRepository.findById(request.disciplinaId())
                        .orElseThrow(() -> new ResourceNotFoundException("Disciplina no encontrada"))
                : (clase != null ? clase.getDisciplina() : null);

        Membresia membresia = membresiaService.resolverParaAsistencia(alumno, disciplina);
        if (!centro.isPermitirAccesoConAdeudo() && pagoMembresiaService.calcularSaldo(membresia).signum() > 0) {
            throw new IllegalStateException(alumno.getNombre() + " tiene saldo pendiente de $"
                    + pagoMembresiaService.calcularSaldo(membresia) + " y este centro no permite el acceso con adeudo");
        }
        membresiaService.descontarClase(membresia);

        Asistencia a = new Asistencia();
        a.setCentro(centro);
        a.setAlumno(alumno);
        a.setDisciplina(disciplina);
        a.setClase(clase);
        a.setRegistradoPor(actor);
        return toDto(asistenciaRepository.save(a));
    }

    public AsistenciaDto toDto(Asistencia a) {
        return new AsistenciaDto(
                a.getId(), a.getAlumno().getId(), a.getAlumno().getNombre(),
                a.getDisciplina() != null ? a.getDisciplina().getId() : null,
                a.getDisciplina() != null ? a.getDisciplina().getNombre() : null,
                a.getClase() != null ? a.getClase().getId() : null,
                a.getFecha(), a.getHora()
        );
    }
}
