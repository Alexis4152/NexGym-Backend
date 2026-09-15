package com.nexora.sport.service;

import com.nexora.sport.dto.MembresiaPlanDto;
import com.nexora.sport.dto.MembresiaPlanRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.Disciplina;
import com.nexora.sport.model.MembresiaPlan;
import com.nexora.sport.model.TipoPeriodoMembresia;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.DisciplinaRepository;
import com.nexora.sport.repository.MembresiaPlanRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MembresiaPlanService {

    private final MembresiaPlanRepository membresiaPlanRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final CentroRepository centroRepository;
    private final TenantScope tenantScope;

    public MembresiaPlanService(MembresiaPlanRepository membresiaPlanRepository, DisciplinaRepository disciplinaRepository,
                                 CentroRepository centroRepository, TenantScope tenantScope) {
        this.membresiaPlanRepository = membresiaPlanRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.centroRepository = centroRepository;
        this.tenantScope = tenantScope;
    }

    public PageResponse<MembresiaPlanDto> listar(Usuario actor, Pageable pageable) {
        return PageResponse.of(membresiaPlanRepository.findByCentroId(tenantScope.scopeId(actor), pageable), this::toDto);
    }

    public List<MembresiaPlanDto> listarActivos(Usuario actor) {
        return membresiaPlanRepository.findByCentroIdAndActivoTrue(tenantScope.scopeId(actor)).stream()
                .map(this::toDto).toList();
    }

    public MembresiaPlan buscar(Long id) {
        return membresiaPlanRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"));
    }

    @Transactional
    public MembresiaPlanDto crear(Usuario actor, MembresiaPlanRequest request) {
        MembresiaPlan plan = new MembresiaPlan();
        plan.setCentro(centroRepository.getReferenceById(tenantScope.scopeId(actor)));
        aplicar(plan, request);
        return toDto(membresiaPlanRepository.save(plan));
    }

    @Transactional
    public MembresiaPlanDto actualizar(Long id, MembresiaPlanRequest request) {
        MembresiaPlan plan = buscar(id);
        aplicar(plan, request);
        return toDto(membresiaPlanRepository.save(plan));
    }

    @Transactional
    public void desactivar(Long id) {
        MembresiaPlan plan = buscar(id);
        plan.setActivo(false);
        membresiaPlanRepository.save(plan);
    }

    private void aplicar(MembresiaPlan plan, MembresiaPlanRequest request) {
        plan.setNombre(request.nombre());
        plan.setTipoPeriodo(TipoPeriodoMembresia.valueOf(request.tipoPeriodo()));
        plan.setDuracionDias(request.duracionDias());
        plan.setNumeroClasesIncluidas(request.numeroClasesIncluidas());
        plan.setPrecio(request.precio());
        plan.setMultidisciplina(request.multidisciplina());
        plan.setAccesoCompleto(request.accesoCompleto());
        plan.setLimiteAlumnos(request.limiteAlumnos());
        Set<Disciplina> disciplinas = new HashSet<>();
        if (request.disciplinaIds() != null) {
            request.disciplinaIds().forEach(id -> disciplinas.add(disciplinaRepository.getReferenceById(id)));
        }
        plan.setDisciplinas(disciplinas);
    }

    public MembresiaPlanDto toDto(MembresiaPlan p) {
        return new MembresiaPlanDto(
                p.getId(), p.getNombre(), p.getTipoPeriodo().name(), p.getDuracionDias(),
                p.getNumeroClasesIncluidas(), p.getPrecio(), p.isMultidisciplina(), p.isAccesoCompleto(),
                p.getLimiteAlumnos(), p.isActivo(),
                p.getDisciplinas().stream().map(Disciplina::getId).collect(Collectors.toSet()),
                p.getDisciplinas().stream().map(Disciplina::getNombre).collect(Collectors.toSet())
        );
    }
}
