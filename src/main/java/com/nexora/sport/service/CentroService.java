package com.nexora.sport.service;

import com.nexora.sport.dto.CentroDto;
import com.nexora.sport.dto.CentroRequest;
import com.nexora.sport.exception.FieldConflictException;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.Centro;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class CentroService {

    private static final Set<String> LOGO_TIPOS_PERMITIDOS = Set.of("image/png", "image/jpeg", "image/webp");
    private static final long LOGO_TAMANIO_MAXIMO = 3L * 1024 * 1024; // 3MB

    private final CentroRepository centroRepository;
    private final RolService rolService;
    private final CajaService cajaService;
    private final SucursalService sucursalService;
    private final FileStorageService fileStorageService;
    private final TenantScope tenantScope;

    public CentroService(CentroRepository centroRepository, RolService rolService, CajaService cajaService,
                          SucursalService sucursalService, FileStorageService fileStorageService, TenantScope tenantScope) {
        this.centroRepository = centroRepository;
        this.rolService = rolService;
        this.cajaService = cajaService;
        this.sucursalService = sucursalService;
        this.fileStorageService = fileStorageService;
        this.tenantScope = tenantScope;
    }

    @Transactional(readOnly = true)
    public List<CentroDto> listar() {
        return centroRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<CentroDto> disponiblesPara(Usuario actorPrincipal) {
        if (tenantScope.isSuperAdmin(actorPrincipal)) {
            return listar();
        }
        // actorPrincipal (@AuthenticationPrincipal) quedo detached de su sesion original
        // (spring.jpa.open-in-view=false) -- recargarlo aqui es necesario antes de tocar
        // centrosAdicionales (coleccion @Lazy), o revienta con LazyInitializationException.
        Usuario actor = tenantScope.actorConAsociacionesCargadas(actorPrincipal);
        List<Centro> centros = new ArrayList<>();
        if (actor.getCentro() != null) centros.add(actor.getCentro());
        if (tenantScope.isSupervisor(actor)) centros.addAll(actor.getCentrosAdicionales());
        return centros.stream().distinct().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CentroDto obtener(Long id) {
        return toDto(buscar(id));
    }

    public Centro buscar(Long id) {
        return centroRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Centro no encontrado"));
    }

    @Transactional
    public CentroDto crear(CentroRequest request) {
        Centro centro = new Centro();
        aplicar(centro, request);
        centro = centroRepository.save(centro);
        rolService.seedRolesPorDefecto(centro);
        cajaService.seedCategoriasPorDefecto(centro);
        sucursalService.seedSucursalPorDefecto(centro);
        return toDto(centro);
    }

    @Transactional
    public CentroDto actualizar(Long id, CentroRequest request) {
        Centro centro = buscar(id);
        aplicar(centro, request);
        return toDto(centroRepository.save(centro));
    }

    @Transactional
    public CentroDto subirLogo(Long id, MultipartFile file) {
        Centro centro = buscar(id);
        String rutaAnterior = centro.getLogoUrl();
        String ruta = fileStorageService.guardar("centros", centro.getId(), file, LOGO_TIPOS_PERMITIDOS, LOGO_TAMANIO_MAXIMO);
        centro.setLogoUrl(ruta);
        centro = centroRepository.save(centro);
        if (rutaAnterior != null) fileStorageService.eliminar(rutaAnterior);
        return toDto(centro);
    }

    private void aplicar(Centro centro, CentroRequest request) {
        centro.setNombre(request.nombre());
        centro.setTelefono(request.telefono());
        centro.setEmailContacto(request.emailContacto());
        centro.setDireccion(request.direccion());
        if (request.colorPrimario() != null) centro.setColorPrimario(request.colorPrimario());
        if (request.catalogoPublicoActivo() != null) centro.setCatalogoPublicoActivo(request.catalogoPublicoActivo());
        if (request.apartadosActivo() != null) centro.setApartadosActivo(request.apartadosActivo());
        if (request.horasApartadoDefault() != null) centro.setHorasApartadoDefault(request.horasApartadoDefault());
        centro.setMontoMaximoDescuentoApartado(request.montoMaximoDescuentoApartado());
        centro.setPorcentajeMaximoDescuentoApartado(request.porcentajeMaximoDescuentoApartado());
        if (request.permitirAccesoConAdeudo() != null) centro.setPermitirAccesoConAdeudo(request.permitirAccesoConAdeudo());
        if (request.diasInactividadRiesgo() != null) centro.setDiasInactividadRiesgo(request.diasInactividadRiesgo());
        if (request.notificacionesMembresiaActivo() != null) centro.setNotificacionesMembresiaActivo(request.notificacionesMembresiaActivo());
        if (request.notificacionesClaseActivo() != null) centro.setNotificacionesClaseActivo(request.notificacionesClaseActivo());
        if (request.notificacionesEmailActivo() != null) centro.setNotificacionesEmailActivo(request.notificacionesEmailActivo());
        if (request.notificacionesInternoActivo() != null) centro.setNotificacionesInternoActivo(request.notificacionesInternoActivo());
        if (request.notificacionesDiasAntesVencimiento() != null) centro.setNotificacionesDiasAntesVencimiento(request.notificacionesDiasAntesVencimiento());
        if (request.notificacionesHorasAntesClase() != null) centro.setNotificacionesHorasAntesClase(request.notificacionesHorasAntesClase());
        centro.setHoraCierreAutomaticoCorte(request.horaCierreAutomaticoCorte());

        String slug = request.slugPublico();
        if (slug == null || slug.isBlank()) {
            slug = centro.getSlugPublico() != null ? centro.getSlugPublico() : slugify(request.nombre());
        }
        if (!slug.equals(centro.getSlugPublico()) && centroRepository.existsBySlugPublico(slug)) {
            throw new FieldConflictException("slugPublico", "Ese enlace publico ya esta en uso");
        }
        centro.setSlugPublico(slug);
    }

    private String slugify(String nombre) {
        String normalized = Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String base = normalized.toLowerCase().trim().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        String slug = base;
        int suffix = 1;
        while (centroRepository.existsBySlugPublico(slug)) {
            slug = base + "-" + (++suffix);
        }
        return slug;
    }

    public CentroDto toDto(Centro c) {
        return new CentroDto(c.getId(), c.getNombre(), c.getSlugPublico(), c.isCatalogoPublicoActivo(),
                c.getColorPrimario(), c.getLogoUrl(), c.getTelefono(), c.getEmailContacto(), c.getDireccion(),
                c.isActivo(), c.isApartadosActivo(), c.getHorasApartadoDefault(),
                c.getMontoMaximoDescuentoApartado(), c.getPorcentajeMaximoDescuentoApartado(),
                c.isPermitirAccesoConAdeudo(), c.getDiasInactividadRiesgo(),
                c.isNotificacionesMembresiaActivo(), c.isNotificacionesClaseActivo(),
                c.isNotificacionesEmailActivo(), c.isNotificacionesInternoActivo(),
                c.getNotificacionesDiasAntesVencimiento(), c.getNotificacionesHorasAntesClase(),
                c.getHoraCierreAutomaticoCorte());
    }
}
