package com.nexora.sport.service;

import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.dto.ProveedorDto;
import com.nexora.sport.dto.ProveedorRequest;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.Proveedor;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.ProveedorRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final CentroRepository centroRepository;
    private final TenantScope tenantScope;

    public ProveedorService(ProveedorRepository proveedorRepository, CentroRepository centroRepository, TenantScope tenantScope) {
        this.proveedorRepository = proveedorRepository;
        this.centroRepository = centroRepository;
        this.tenantScope = tenantScope;
    }

    public PageResponse<ProveedorDto> listar(Usuario actor, Pageable pageable) {
        return PageResponse.of(proveedorRepository.findByCentroIdAndActivoTrue(tenantScope.scopeId(actor), pageable), this::toDto);
    }

    public Proveedor buscar(Long id) {
        return proveedorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
    }

    @Transactional
    public ProveedorDto crear(Usuario actor, ProveedorRequest request) {
        Proveedor p = new Proveedor();
        p.setCentro(centroRepository.getReferenceById(tenantScope.scopeId(actor)));
        aplicar(p, request);
        return toDto(proveedorRepository.save(p));
    }

    @Transactional
    public ProveedorDto actualizar(Long id, ProveedorRequest request) {
        Proveedor p = buscar(id);
        aplicar(p, request);
        return toDto(proveedorRepository.save(p));
    }

    @Transactional
    public void desactivar(Long id) {
        Proveedor p = buscar(id);
        p.setActivo(false);
        proveedorRepository.save(p);
    }

    private void aplicar(Proveedor p, ProveedorRequest request) {
        p.setNombre(request.nombre());
        p.setContacto(request.contacto());
        p.setTelefono(request.telefono());
        p.setEmail(request.email());
        p.setNotas(request.notas());
    }

    public ProveedorDto toDto(Proveedor p) {
        return new ProveedorDto(p.getId(), p.getNombre(), p.getContacto(), p.getTelefono(), p.getEmail(), p.getNotas(), p.isActivo());
    }
}
