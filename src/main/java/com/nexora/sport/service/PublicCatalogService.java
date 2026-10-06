package com.nexora.sport.service;

import com.nexora.sport.dto.publico.PublicApartadoSucursalDto;
import com.nexora.sport.dto.publico.PublicArticuloApartadoDto;
import com.nexora.sport.dto.publico.PublicCentroDto;
import com.nexora.sport.dto.publico.PublicPlanDto;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.ArticuloInventario;
import com.nexora.sport.model.CategoriaInventario;
import com.nexora.sport.model.Centro;
import com.nexora.sport.model.Disciplina;
import com.nexora.sport.model.ImagenArticulo;
import com.nexora.sport.model.MembresiaPlan;
import com.nexora.sport.repository.ArticuloInventarioRepository;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.ImagenArticuloRepository;
import com.nexora.sport.repository.MembresiaPlanRepository;
import com.nexora.sport.repository.MembresiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Catalogo publico sin login, aislado por slug (mismo patron que "Apartados" en DemoPV). */
@Service
public class PublicCatalogService {

    private final CentroRepository centroRepository;
    private final ArticuloInventarioRepository articuloRepository;
    private final ImagenArticuloRepository imagenArticuloRepository;
    private final MembresiaPlanRepository membresiaPlanRepository;
    private final MembresiaRepository membresiaRepository;

    public PublicCatalogService(CentroRepository centroRepository, ArticuloInventarioRepository articuloRepository,
                                 ImagenArticuloRepository imagenArticuloRepository,
                                 MembresiaPlanRepository membresiaPlanRepository, MembresiaRepository membresiaRepository) {
        this.centroRepository = centroRepository;
        this.articuloRepository = articuloRepository;
        this.imagenArticuloRepository = imagenArticuloRepository;
        this.membresiaPlanRepository = membresiaPlanRepository;
        this.membresiaRepository = membresiaRepository;
    }

    /** La "tienda publica" independiente (catalogo de solo lectura) ya no existe -- queda
     * reemplazada por esta misma pagina de apartados, asi que el candado es apartadosActivo
     * (antes era un flag aparte, catalogoPublicoActivo, que ya no se expone en el admin). */
    @Transactional(readOnly = true)
    public PublicCentroDto obtenerCentro(String slug) {
        Centro centro = resolverCentroApartados(slug);
        return new PublicCentroDto(centro.getNombre(), centro.getLogoUrl(), centro.getColorPrimario(),
                centro.getTelefono(), centro.getDireccion(), centro.isApartadosActivo());
    }

    /** Agrupa por nombre (seccion 31 del encargo): ahora cada sucursal tiene su propio
     * ArticuloInventario independiente, pero el cliente ve UN producto y elige en cual
     * sucursal recogerlo -- las que no tienen stock quedan en la lista igual (el frontend
     * las muestra en gris, "Sin stock disponible"), nunca se ocultan una por una. Un grupo
     * completo si desaparece si NINGUNA de sus sucursales tiene existencia. */
    @Transactional(readOnly = true)
    public List<PublicArticuloApartadoDto> listarApartables(String slug) {
        Centro centro = resolverCentroApartados(slug);
        List<ArticuloInventario> articulos = articuloRepository
                .findByCentroIdAndReservableTrueAndActivoTrueAndDeletedAtIsNull(centro.getId());

        Map<String, List<ArticuloInventario>> grupos = new LinkedHashMap<>();
        for (ArticuloInventario a : articulos) {
            grupos.computeIfAbsent(a.getNombre().trim().toLowerCase(), k -> new java.util.ArrayList<>()).add(a);
        }

        return grupos.values().stream()
                .filter(grupo -> grupo.stream().anyMatch(a -> a.getStock() > 0))
                .map(this::toPublicApartadoDto)
                .toList();
    }

    private PublicArticuloApartadoDto toPublicApartadoDto(List<ArticuloInventario> grupo) {
        // El articulo "representante" (textos/fotos/precio a mostrar en la tarjeta): el
        // primero con stock, o el primero del grupo si todos estan en 0.
        ArticuloInventario rep = grupo.stream().filter(a -> a.getStock() > 0).findFirst().orElse(grupo.get(0));
        BigDecimal precio = rep.getPrecioVenta() != null ? rep.getPrecioVenta() : BigDecimal.ZERO;
        BigDecimal descuentoPct = rep.getDescuentoApartadoPorcentaje();
        BigDecimal precioConDescuento = descuentoPct != null
                ? precio.subtract(precio.multiply(descuentoPct).divide(BigDecimal.valueOf(100)))
                : precio;
        List<String> imagenes = imagenesDe(rep);
        List<PublicApartadoSucursalDto> sucursales = grupo.stream()
                .sorted(Comparator.comparing((ArticuloInventario a) -> a.getStock() > 0 ? 0 : 1)
                        .thenComparing(a -> a.getSucursal().getNombre()))
                .map(a -> new PublicApartadoSucursalDto(a.getId(), a.getSucursal().getId(), a.getSucursal().getNombre(),
                        a.getSucursal().getDireccion(), a.getStock()))
                .toList();
        int stockTotal = grupo.stream().mapToInt(ArticuloInventario::getStock).sum();
        return new PublicArticuloApartadoDto(
                rep.getId(), rep.getNombre(), nombresCategorias(rep), nombresCategoriasLista(rep),
                precio, descuentoPct, precioConDescuento, imagenPrincipal(rep, imagenes), imagenes, stockTotal, sucursales);
    }

    /** Los planes activos del centro, para la pestaña "Planes" de la tienda publica de
     * apartados (seccion 28 del encargo): mismo candado que listarApartables (apartadosActivo),
     * porque viven en la misma pagina publica. */
    @Transactional(readOnly = true)
    public List<PublicPlanDto> listarPlanes(String slug) {
        Centro centro = resolverCentroApartados(slug);
        return membresiaPlanRepository.findByCentroIdAndActivoTrue(centro.getId()).stream()
                .map(this::toPublicPlanDto)
                .toList();
    }

    private PublicPlanDto toPublicPlanDto(MembresiaPlan p) {
        long inscritos = membresiaRepository.countVigentesPorPlan(p.getId());
        Integer cupoDisponible = p.getLimiteAlumnos() != null ? Math.max(0, p.getLimiteAlumnos() - (int) inscritos) : null;
        return new PublicPlanDto(
                p.getId(), p.getNombre(), p.getTipoPlan().name(),
                p.getDuracionCantidad(), p.getDuracionUnidad() != null ? p.getDuracionUnidad().name() : null,
                p.getNumeroClasesIncluidas(), p.getPrecio(), p.isAccesoCompleto(), p.isPermiteAbonos(),
                p.getLimiteAlumnos(), cupoDisponible,
                p.getDisciplinas().stream().map(Disciplina::getNombre).toList()
        );
    }

    private String nombresCategorias(ArticuloInventario a) {
        return a.getCategorias().isEmpty() ? null
                : a.getCategorias().stream().map(CategoriaInventario::getNombre).collect(Collectors.joining(", "));
    }

    private List<String> nombresCategoriasLista(ArticuloInventario a) {
        return a.getCategorias().stream().map(CategoriaInventario::getNombre).sorted().toList();
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

    private Centro resolverCentroApartados(String slug) {
        return centroRepository.findBySlugPublicoAndActivoTrue(slug)
                .filter(Centro::isApartadosActivo)
                .orElseThrow(() -> new ResourceNotFoundException("Apartados no disponibles"));
    }
}
