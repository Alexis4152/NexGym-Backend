package com.nexora.sport.service;

import com.nexora.sport.dto.AsistenciaDto;
import com.nexora.sport.dto.AsistenciaRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.model.Asistencia;
import com.nexora.sport.model.Usuario;
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

    public AsistenciaService(AsistenciaRepository asistenciaRepository, AlumnoRepository alumnoRepository,
                              DisciplinaRepository disciplinaRepository, ClaseRepository claseRepository,
                              CentroRepository centroRepository, TenantScope tenantScope) {
        this.asistenciaRepository = asistenciaRepository;
        this.alumnoRepository = alumnoRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.claseRepository = claseRepository;
        this.centroRepository = centroRepository;
        this.tenantScope = tenantScope;
    }

    @Transactional(readOnly = true)
    public PageResponse<AsistenciaDto> listar(Usuario actor, Pageable pageable) {
        return PageResponse.of(asistenciaRepository.findByCentroId(tenantScope.scopeId(actor), pageable), this::toDto);
    }

    public long contarHoy(Long centroId) {
        return asistenciaRepository.countByCentroIdAndFecha(centroId, LocalDate.now());
    }

    @Transactional
    public AsistenciaDto registrar(Usuario actor, AsistenciaRequest request) {
        Asistencia a = new Asistencia();
        a.setCentro(centroRepository.getReferenceById(tenantScope.scopeId(actor)));
        a.setAlumno(alumnoRepository.getReferenceById(request.alumnoId()));
        if (request.disciplinaId() != null) a.setDisciplina(disciplinaRepository.getReferenceById(request.disciplinaId()));
        if (request.claseId() != null) a.setClase(claseRepository.getReferenceById(request.claseId()));
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
