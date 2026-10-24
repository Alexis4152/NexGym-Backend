package com.nexora.sport.service;

import com.nexora.sport.dto.DomicilioDto;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.dto.SucursalDto;
import com.nexora.sport.dto.SucursalRequest;
import com.nexora.sport.exception.FieldConflictException;
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
import java.util.Set;

@Service
public class SucursalService {

    private static final String NOMBRE_SUCURSAL_PRINCIPAL = "Sucursal Principal";

    private final SucursalRepository sucursalRepository;
    private final CentroRepository centroRepository;
    private final TenantScope tenantScope;
    private final CodigoPostalService codigoPostalService;

    public SucursalService(SucursalRepository sucursalRepository, CentroRepository centroRepository, TenantScope tenantScope,
                           CodigoPostalService codigoPostalService) {
        this.sucursalRepository = sucursalRepository;
        this.centroRepository = centroRepository;
        this.tenantScope = tenantScope;
        this.codigoPostalService = codigoPostalService;
    }

    /** Un Admin/Operativo solo ve las sucursales que tiene asignadas (su "hogar" +
     * adicionales), nunca todas las del centro (seccion 27 del encargo de jerarquias):
     * "no podran ver las sucursales, solo su sucursal". Dueno/SUPER_ADMIN sin cambios. */
    @Transactional(readOnly = true)
    public PageResponse<SucursalDto> listar(Usuario actor, Pageable pageable) {
        Long centroId = tenantScope.scopeId(actor);
        Set<Long> permitidas = tenantScope.sucursalesPermitidas(actor);
        var page = permitidas == null
                ? sucursalRepository.findByCentroId(centroId, pageable)
                : sucursalRepository.findByCentroIdAndIdIn(centroId, permitidas, pageable);
        return PageResponse.of(page, this::toDto);
    }

    @Transactional(readOnly = true)
    public List<SucursalDto> listarActivas(Usuario actor) {
        Long centroId = tenantScope.scopeId(actor);
        Set<Long> permitidas = tenantScope.sucursalesPermitidas(actor);
        var lista = permitidas == null
                ? sucursalRepository.findByCentroIdAndActivoTrueOrderByNombre(centroId)
                : sucursalRepository.findByCentroIdAndIdInAndActivoTrueOrderByNombre(centroId, permitidas);
        return lista.stream().map(this::toDto).toList();
    }

    public Sucursal buscar(Long id) {
        return sucursalRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
    }

    /** Crear/editar/desactivar sucursales es exclusivo del Dueno del negocio (SUPERVISOR)
     * o de la plataforma (SUPER_ADMIN) -- un Admin/encargado de sucursal NUNCA puede
     * activar, desactivar ni editar nada de ninguna sucursal, ni siquiera la suya. */
    private void assertPuedeAdministrarSucursales(Usuario actor) {
        if (!tenantScope.isSupervisorOSuperior(actor)) {
            throw new IllegalStateException("Solo el dueno del centro puede administrar sucursales");
        }
    }

    @Transactional
    public SucursalDto crear(Usuario actor, SucursalRequest request) {
        assertPuedeAdministrarSucursales(actor);
        Sucursal s = new Sucursal();
        s.setCentro(centroRepository.getReferenceById(tenantScope.scopeId(actor)));
        aplicar(s, request);
        return toDto(sucursalRepository.save(s));
    }

    @Transactional
    public SucursalDto actualizar(Usuario actor, Long id, SucursalRequest request) {
        assertPuedeAdministrarSucursales(actor);
        Sucursal s = buscar(id);
        aplicar(s, request);
        return toDto(sucursalRepository.save(s));
    }

    @Transactional
    public void desactivar(Usuario actor, Long id) {
        assertPuedeAdministrarSucursales(actor);
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
        if (codigoPostalService.esInexistente(request.domicilio().codigoPostal())) {
            throw new FieldConflictException("domicilio.codigoPostal", "Ese codigo postal no existe en el catalogo de SEPOMEX");
        }
        s.setNombre(request.nombre());
        s.setDomicilio(request.domicilio().aEntidad());
        s.setDireccion(s.getDomicilio().formatear(300));
        s.setNotas(request.notas());
    }

    /** La "Sucursal Principal" sembrada al crear el centro nace sin domicilio: cuando el dueno
     * captura el del centro, se le copia mientras siga vacio (asi la tienda en linea ya puede
     * mostrar donde recoger). */
    @Transactional
    public void heredarDomicilioDelCentro(Centro centro) {
        if (centro.getDomicilio() == null || centro.getDomicilio().estaVacio()) return;
        for (Sucursal s : sucursalRepository.findByCentroIdAndActivoTrueOrderByNombre(centro.getId())) {
            if (NOMBRE_SUCURSAL_PRINCIPAL.equals(s.getNombre()) && (s.getDomicilio() == null || s.getDomicilio().estaVacio())) {
                s.setDomicilio(centro.getDomicilio().copia());
                s.setDireccion(s.getDomicilio().formatear(300));
                sucursalRepository.save(s);
            }
        }
    }

    public SucursalDto toDto(Sucursal s) {
        return new SucursalDto(s.getId(), s.getNombre(), s.getDireccion(), DomicilioDto.de(s.getDomicilio()),
                s.getNotas(), s.isActivo());
    }
}
