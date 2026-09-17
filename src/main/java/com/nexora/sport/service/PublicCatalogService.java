package com.nexora.sport.service;

import com.nexora.sport.dto.publico.PublicArticuloDto;
import com.nexora.sport.dto.publico.PublicCentroDto;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.ArticuloInventario;
import com.nexora.sport.model.Centro;
import com.nexora.sport.repository.ArticuloInventarioRepository;
import com.nexora.sport.repository.CentroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Catalogo publico sin login, aislado por slug (mismo patron que "Apartados" en DemoPV). */
@Service
public class PublicCatalogService {

    private final CentroRepository centroRepository;
    private final ArticuloInventarioRepository articuloRepository;

    public PublicCatalogService(CentroRepository centroRepository, ArticuloInventarioRepository articuloRepository) {
        this.centroRepository = centroRepository;
        this.articuloRepository = articuloRepository;
    }

    @Transactional(readOnly = true)
    public PublicCentroDto obtenerCentro(String slug) {
        Centro centro = resolverCentro(slug);
        return new PublicCentroDto(centro.getNombre(), centro.getLogoUrl(), centro.getColorPrimario(),
                centro.getTelefono(), centro.getDireccion());
    }

    @Transactional(readOnly = true)
    public List<PublicArticuloDto> listarProductos(String slug) {
        Centro centro = resolverCentro(slug);
        return articuloRepository.findByCentroIdAndVendibleTrueAndActivoTrueAndDeletedAtIsNull(centro.getId()).stream()
                .map(a -> new PublicArticuloDto(
                        a.getId(), a.getNombre(),
                        a.getCategoria() != null ? a.getCategoria().getNombre() : null,
                        a.getPrecioVenta(), a.getImagenUrl()))
                .toList();
    }

    private Centro resolverCentro(String slug) {
        return centroRepository.findBySlugPublicoAndActivoTrue(slug)
                .filter(Centro::isCatalogoPublicoActivo)
                .orElseThrow(() -> new ResourceNotFoundException("Catalogo no disponible"));
    }
}
