package com.nexora.sport.service;

import com.nexora.sport.dto.publico.PublicArticuloApartadoDto;
import com.nexora.sport.dto.publico.PublicArticuloDto;
import com.nexora.sport.dto.publico.PublicCentroDto;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.ArticuloInventario;
import com.nexora.sport.model.CategoriaInventario;
import com.nexora.sport.model.Centro;
import com.nexora.sport.model.ImagenArticulo;
import com.nexora.sport.repository.ArticuloInventarioRepository;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.ImagenArticuloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

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
                .map(a -> {
                    List<String> imagenes = imagenesDe(a);
                    return new PublicArticuloDto(
                            a.getId(), a.getNombre(), nombresCategorias(a),
                            a.getPrecioVenta(), imagenPrincipal(a, imagenes), imagenes);
                })
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
                    List<String> imagenes = imagenesDe(a);
                    return new PublicArticuloApartadoDto(
                            a.getId(), a.getNombre(), nombresCategorias(a),
                            precio, descuentoPct, precioConDescuento, imagenPrincipal(a, imagenes), imagenes, a.getStock());
                })
                .toList();
    }

    private String nombresCategorias(ArticuloInventario a) {
        return a.getCategorias().isEmpty() ? null
                : a.getCategorias().stream().map(CategoriaInventario::getNombre).collect(Collectors.joining(", "));
    }

    /** Hasta 3 fotos (ver ImagenArticuloService, tope de 3 al subir): la portada
     * (esPrincipal) siempre primero -- puede no coincidir con "orden" si se cambio de
     * portada despues de subir varias -- seguida del resto en su orden. */
    private List<String> imagenesDe(ArticuloInventario a) {
        List<ImagenArticulo> todas = imagenArticuloRepository.findByArticuloIdOrderByOrdenAsc(a.getId());
        List<String> ordenadas = new java.util.ArrayList<>();
        todas.stream().filter(ImagenArticulo::isEsPrincipal).findFirst()
                .ifPresent(p -> ordenadas.add(p.getRuta()));
        todas.stream().filter(img -> !img.isEsPrincipal()).forEach(img -> ordenadas.add(img.getRuta()));
        return ordenadas.stream().limit(3).toList();
    }

    private String imagenPrincipal(ArticuloInventario a, List<String> imagenes) {
        return !imagenes.isEmpty() ? imagenes.get(0) : a.getImagenUrl();
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
