package com.nexora.sport.service;

import com.nexora.sport.dto.LugarDto;
import com.nexora.sport.dto.LugarRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.Disciplina;
import com.nexora.sport.model.Lugar;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.DisciplinaRepository;
import com.nexora.sport.repository.LugarRepository;
import com.nexora.sport.repository.SucursalRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LugarService {

    private final LugarRepository lugarRepository;
    private final SucursalRepository sucursalRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final CentroRepository centroRepository;
    private final TenantScope tenantScope;

    public LugarService(LugarRepository lugarRepository, SucursalRepository sucursalRepository,
                         DisciplinaRepository disciplinaRepository, CentroRepository centroRepository,
                         TenantScope tenantScope) {
        this.lugarRepository = lugarRepository;
        this.sucursalRepository = sucursalRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.centroRepository = centroRepository;
        this.tenantScope = tenantScope;
    }

    @Transactional(readOnly = true)
    public PageResponse<LugarDto> listar(Usuario actor, Pageable pageable) {
        return PageResponse.of(lugarRepository.findByCentroId(tenantScope.scopeId(actor), pageable), this::toDto);
    }

    @Transactional(readOnly = true)
    public List<LugarDto> listarActivos(Usuario actor, Long sucursalId, Long disciplinaId) {
        return lugarRepository.buscarActivos(tenantScope.scopeId(actor), sucursalId, disciplinaId).stream()
                .map(this::toDto).toList();
    }

    public Lugar buscar(Long id) {
        return lugarRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lugar no encontrado"));
    }

    @Transactional
    public LugarDto crear(Usuario actor, LugarRequest request) {
        Lugar l = new Lugar();
        l.setCentro(centroRepository.getReferenceById(tenantScope.scopeId(actor)));
        aplicar(l, request);
        return toDto(lugarRepository.save(l));
    }

    @Transactional
    public LugarDto actualizar(Long id, LugarRequest request) {
        Lugar l = buscar(id);
        aplicar(l, request);
        return toDto(lugarRepository.save(l));
    }

    @Transactional
    public void desactivar(Long id) {
        Lugar l = buscar(id);
        l.setActivo(false);
        lugarRepository.save(l);
    }

    private void aplicar(Lugar l, LugarRequest request) {
        l.setSucursal(sucursalRepository.getReferenceById(request.sucursalId()));
        l.setNombre(request.nombre());
        l.setDireccion(request.direccion());
        l.setNotas(request.notas());
        l.setCapacidadMaxima(request.capacidadMaxima());
        Set<Disciplina> disciplinas = request.disciplinaIds() == null ? Set.of()
                : request.disciplinaIds().stream().map(disciplinaRepository::getReferenceById).collect(Collectors.toSet());
        l.setDisciplinas(new HashSet<>(disciplinas));
    }

    public LugarDto toDto(Lugar l) {
        return new LugarDto(
                l.getId(), l.getSucursal().getId(), l.getSucursal().getNombre(),
                l.getNombre(), l.getDireccion(), l.getNotas(), l.getCapacidadMaxima(), l.isActivo(),
                l.getDisciplinas().stream().map(Disciplina::getId).collect(Collectors.toSet()),
                l.getDisciplinas().stream().map(Disciplina::getNombre).collect(Collectors.toSet())
        );
    }
}
