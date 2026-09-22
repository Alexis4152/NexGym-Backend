package com.nexora.sport.service;

import com.nexora.sport.dto.MembresiaPlanDto;
import com.nexora.sport.dto.MembresiaPlanRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.Disciplina;
import com.nexora.sport.model.MembresiaPlan;
import com.nexora.sport.model.TipoPlanMembresia;
import com.nexora.sport.model.UnidadDuracion;
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

    @Transactional(readOnly = true)
    public PageResponse<MembresiaPlanDto> listar(Usuario actor, Pageable pageable) {
        return PageResponse.of(membresiaPlanRepository.findByCentroId(tenantScope.scopeId(actor), pageable), this::toDto);
    }

    @Transactional(readOnly = true)
    public List<MembresiaPlanDto> listarActivos(Usuario actor) {
        return membresiaPlanRepository.findByCentroIdAndActivoTrue(tenantScope.scopeId(actor)).stream()
                .map(this::toDto).toList();
    }

    public MembresiaPlan buscar(Long id) {
        return membresiaPlanRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"));
    }

    /** Igual que buscar(), pero valida que el plan pertenezca al centro del actor (aislamiento multi-tenant). */
    public MembresiaPlan buscarDelCentro(Long id, Long centroId) {
        MembresiaPlan plan = buscar(id);
        if (!plan.getCentro().getId().equals(centroId)) {
            throw new ResourceNotFoundException("Plan no encontrado");
        }
        return plan;
    }

    @Transactional
    public MembresiaPlanDto crear(Usuario actor, MembresiaPlanRequest request) {
        MembresiaPlan plan = new MembresiaPlan();
        plan.setCentro(centroRepository.getReferenceById(tenantScope.scopeId(actor)));
        aplicar(plan, request);
        return toDto(membresiaPlanRepository.save(plan));
    }

    @Transactional
    public MembresiaPlanDto actualizar(Usuario actor, Long id, MembresiaPlanRequest request) {
        MembresiaPlan plan = buscarDelCentro(id, tenantScope.scopeId(actor));
        aplicar(plan, request);
        return toDto(membresiaPlanRepository.save(plan));
    }

    @Transactional
    public void desactivar(Usuario actor, Long id) {
        MembresiaPlan plan = buscarDelCentro(id, tenantScope.scopeId(actor));
        plan.setActivo(false);
        membresiaPlanRepository.save(plan);
    }

    private void aplicar(MembresiaPlan plan, MembresiaPlanRequest request) {
        TipoPlanMembresia tipo = TipoPlanMembresia.valueOf(request.tipoPlan());

        Integer duracionCantidad = request.duracionCantidad();
        UnidadDuracion duracionUnidad = request.duracionUnidad() != null ? UnidadDuracion.valueOf(request.duracionUnidad()) : null;
        Integer numeroClases = request.numeroClasesIncluidas();

        switch (tipo) {
            case PERIODO -> {
                if (duracionCantidad == null || duracionCantidad <= 0 || duracionUnidad == null) {
                    throw new IllegalArgumentException("Un plan por periodo requiere duracion (cantidad + unidad)");
                }
                numeroClases = null;
            }
            case POR_CLASES -> {
                if (numeroClases == null || numeroClases <= 0) {
                    throw new IllegalArgumentException("Un plan por clases requiere el numero de clases incluidas");
                }
                // duracionCantidad/duracionUnidad quedan libres: vigencia opcional (ver seccion 17).
                if ((duracionCantidad == null) != (duracionUnidad == null)) {
                    throw new IllegalArgumentException("Si defines vigencia para el paquete de clases, indica cantidad y unidad");
                }
            }
            case PASE -> {
                numeroClases = 1; // fijo por definicion: una clase individual.
                duracionCantidad = null;
                duracionUnidad = null;
            }
            case PERSONALIZADO -> {
                if (duracionCantidad == null && numeroClases == null) {
                    throw new IllegalArgumentException("Un plan personalizado requiere al menos duracion o numero de clases");
                }
                if ((duracionCantidad == null) != (duracionUnidad == null)) {
                    throw new IllegalArgumentException("La duracion requiere cantidad y unidad juntas");
                }
            }
        }

        plan.setNombre(request.nombre());
        plan.setTipoPlan(tipo);
        plan.setDuracionCantidad(duracionCantidad);
        plan.setDuracionUnidad(duracionUnidad);
        plan.setNumeroClasesIncluidas(numeroClases);
        plan.setPrecio(request.precio());
        plan.setAccesoCompleto(request.accesoCompleto());
        plan.setPermiteAbonos(request.permiteAbonos());
        plan.setMontoMinimoAbono(request.permiteAbonos() ? request.montoMinimoAbono() : null);
        plan.setLimiteAlumnos(request.limiteAlumnos());

        Set<Disciplina> disciplinas = new HashSet<>();
        if (!request.accesoCompleto() && request.disciplinaIds() != null) {
            request.disciplinaIds().forEach(id -> disciplinas.add(disciplinaRepository.getReferenceById(id)));
        }
        plan.setDisciplinas(disciplinas);

        if (request.accesoCompleto()) {
            plan.setMaxDisciplinasSeleccionables(null);
        } else if (!disciplinas.isEmpty()) {
            int max = request.maxDisciplinasSeleccionables() != null ? request.maxDisciplinasSeleccionables() : 1;
            if (max < 1 || max > disciplinas.size()) {
                throw new IllegalArgumentException("El maximo de disciplinas seleccionables debe estar entre 1 y " + disciplinas.size());
            }
            plan.setMaxDisciplinasSeleccionables(max);
        } else {
            plan.setMaxDisciplinasSeleccionables(null);
        }
    }

    public MembresiaPlanDto toDto(MembresiaPlan p) {
        return new MembresiaPlanDto(
                p.getId(), p.getNombre(), p.getTipoPlan().name(),
                p.getDuracionCantidad(), p.getDuracionUnidad() != null ? p.getDuracionUnidad().name() : null,
                p.getNumeroClasesIncluidas(), p.getPrecio(), p.isAccesoCompleto(), p.getMaxDisciplinasSeleccionables(),
                p.isPermiteAbonos(), p.getMontoMinimoAbono(), p.getLimiteAlumnos(), p.isActivo(),
                p.getDisciplinas().stream().map(Disciplina::getId).collect(Collectors.toSet()),
                p.getDisciplinas().stream().map(Disciplina::getNombre).collect(Collectors.toSet())
        );
    }
}
