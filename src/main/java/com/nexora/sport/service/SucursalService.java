package com.nexora.sport.service;

import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.dto.SucursalDto;
import com.nexora.sport.dto.SucursalRequest;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.Centro;
import com.nexora.sport.model.Sucursal;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.SucursalRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SucursalService {

    private static final String NOMBRE_SUCURSAL_PRINCIPAL = "Sucursal Principal";

    private final SucursalRepository sucursalRepository;
    private final CentroRepository centroRepository;
    private final TenantScope tenantScope;

    public SucursalService(SucursalRepository sucursalRepository, CentroRepository centroRepository, TenantScope tenantScope) {
        this.sucursalRepository = sucursalRepository;
        this.centroRepository = centroRepository;
        this.tenantScope = tenantScope;
    }

    @Transactional(readOnly = true)
    public PageResponse<SucursalDto> listar(Usuario actor, Pageable pageable) {
        return PageResponse.of(sucursalRepository.findByCentroId(tenantScope.scopeId(actor), pageable), this::toDto);
    }

    @Transactional(readOnly = true)
    public List<SucursalDto> listarActivas(Usuario actor) {
        return sucursalRepository.findByCentroIdAndActivoTrueOrderByNombre(tenantScope.scopeId(actor)).stream()
                .map(this::toDto).toList();
    }

    public Sucursal buscar(Long id) {
        return sucursalRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
    }

    @Transactional
    public SucursalDto crear(Usuario actor, SucursalRequest request) {
        Sucursal s = new Sucursal();
        s.setCentro(centroRepository.getReferenceById(tenantScope.scopeId(actor)));
        aplicar(s, request);
        return toDto(sucursalRepository.save(s));
    }

    @Transactional
    public SucursalDto actualizar(Long id, SucursalRequest request) {
        Sucursal s = buscar(id);
        aplicar(s, request);
        return toDto(sucursalRepository.save(s));
    }

    @Transactional
    public void desactivar(Long id) {
        Sucursal s = buscar(id);
        s.setActivo(false);
        sucursalRepository.save(s);
    }

    /** Siembra la sucursal por defecto de un centro nuevo. Reutilizado tambien por la migracion de datos historicos. */
    @Transactional
    public Sucursal seedSucursalPorDefecto(Centro centro) {
        return sucursalRepository.findByCentroIdAndActivoTrueOrderByNombre(centro.getId()).stream()
                .findFirst()
                .orElseGet(() -> {
                    Sucursal s = new Sucursal();
                    s.setCentro(centro);
                    s.setNombre(NOMBRE_SUCURSAL_PRINCIPAL);
                    return sucursalRepository.save(s);
                });
    }

    private void aplicar(Sucursal s, SucursalRequest request) {
        s.setNombre(request.nombre());
        s.setDireccion(request.direccion());
        s.setNotas(request.notas());
    }

    public SucursalDto toDto(Sucursal s) {
        return new SucursalDto(s.getId(), s.getNombre(), s.getDireccion(), s.getNotas(), s.isActivo());
    }
}
