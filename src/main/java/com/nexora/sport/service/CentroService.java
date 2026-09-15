package com.nexora.sport.service;

import com.nexora.sport.dto.CentroDto;
import com.nexora.sport.dto.CentroRequest;
import com.nexora.sport.exception.FieldConflictException;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.Centro;
import com.nexora.sport.repository.CentroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;

@Service
public class CentroService {

    private final CentroRepository centroRepository;
    private final RolService rolService;
    private final CajaService cajaService;

    public CentroService(CentroRepository centroRepository, RolService rolService, CajaService cajaService) {
        this.centroRepository = centroRepository;
        this.rolService = rolService;
        this.cajaService = cajaService;
    }

    public List<CentroDto> listar() {
        return centroRepository.findAll().stream().map(this::toDto).toList();
    }

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
        return toDto(centro);
    }

    @Transactional
    public CentroDto actualizar(Long id, CentroRequest request) {
        Centro centro = buscar(id);
        aplicar(centro, request);
        return toDto(centroRepository.save(centro));
    }

    private void aplicar(Centro centro, CentroRequest request) {
        centro.setNombre(request.nombre());
        centro.setTelefono(request.telefono());
        centro.setEmailContacto(request.emailContacto());
        centro.setDireccion(request.direccion());
        if (request.colorPrimario() != null) centro.setColorPrimario(request.colorPrimario());
        if (request.catalogoPublicoActivo() != null) centro.setCatalogoPublicoActivo(request.catalogoPublicoActivo());

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
                c.isActivo());
    }
}
