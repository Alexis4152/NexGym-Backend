package com.nexora.sport.service;

import com.nexora.sport.dto.publico.PublicArticuloApartadoDto;
import com.nexora.sport.dto.publico.PublicArticuloDto;
import com.nexora.sport.dto.publico.PublicCentroDto;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.ArticuloInventario;
import com.nexora.sport.model.Centro;
import com.nexora.sport.model.ImagenArticulo;
import com.nexora.sport.repository.ArticuloInventarioRepository;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.ImagenArticuloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Catalogo publico sin login, aislado por slug (mismo patron que "Apartados" en DemoPV). */
@Service
public class PublicCatalogService {

    private final CentroRepository centroRepository;
    private final ArticuloInventarioRepository articuloRepository;
    private final ImagenArticuloRepository imagenArticuloRepository;

    public PublicCatalogService(CentroRepository centroRepository, ArticuloInventarioRepository articuloRepository,
                                 ImagenArticuloRepository imagenArticuloRepository) {
        this.centroRepository = centroRepository;
        this.articuloRepository = articuloRepository;
        this.imagenArticuloRepository = imagenArticuloRepository;
    }

    @Transactional(readOnly = true)
    public PublicCentroDto obtenerCentro(String slug) {
        Centro centro = resolverCentro(slug);
        return new PublicCentroDto(centro.getNombre(), centro.getLogoUrl(), centro.getColorPrimario(),
                centro.getTelefono(), centro.getDireccion(), centro.isApartadosActivo());
    }

    @Transactional(readOnly = true)
    public List<PublicArticuloDto> listarProductos(String slug) {
        Centro centro = resolverCentro(slug);
        return articuloRepository.findByCentroIdAndVendibleTrueAndActivoTrueAndDeletedAtIsNull(centro.getId()).stream()
                .map(a -> new PublicArticuloDto(
                        a.getId(), a.getNombre(),
                        a.getCategoria() != null ? a.getCategoria().getNombre() : null,
                        a.getPrecioVenta(), imagenPrincipal(a)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicArticuloApartadoDto> listarApartables(String slug) {
        Centro centro = resolverCentroApartados(slug);
        return articuloRepository.findByCentroIdAndReservableTrueAndActivoTrueAndDeletedAtIsNull(centro.getId()).stream()
                .filter(a -> a.getStock() > 0)
                .map(a -> {
                    BigDecimal precio = a.getPrecioVenta() != null ? a.getPrecioVenta() : BigDecimal.ZERO;
                    BigDecimal descuentoPct = a.getDescuentoApartadoPorcentaje();
                    BigDecimal precioConDescuento = descuentoPct != null
                            ? precio.subtract(precio.multiply(descuentoPct).divide(BigDecimal.valueOf(100)))
                            : precio;
                    return new PublicArticuloApartadoDto(
                            a.getId(), a.getNombre(), a.getCategoria() != null ? a.getCategoria().getNombre() : null,
                            precio, descuentoPct, precioConDescuento, imagenPrincipal(a), a.getStock());
                })
                .toList();
    }

    private String imagenPrincipal(ArticuloInventario a) {
        return imagenArticuloRepository.findFirstByArticuloIdAndEsPrincipalTrue(a.getId())
                .map(ImagenArticulo::getRuta).orElse(a.getImagenUrl());
    }

    private Centro resolverCentro(String slug) {
        return centroRepository.findBySlugPublicoAndActivoTrue(slug)
                .filter(Centro::isCatalogoPublicoActivo)
                .orElseThrow(() -> new ResourceNotFoundException("Catalogo no disponible"));
    }

    private Centro resolverCentroApartados(String slug) {
        return centroRepository.findBySlugPublicoAndActivoTrue(slug)
                .filter(Centro::isApartadosActivo)
                .orElseThrow(() -> new ResourceNotFoundException("Apartados no disponibles"));
    }
}
