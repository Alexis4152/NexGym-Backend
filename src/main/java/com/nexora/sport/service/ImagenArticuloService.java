package com.nexora.sport.service;

import com.nexora.sport.dto.ImagenArticuloDto;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.ArticuloInventario;
import com.nexora.sport.model.ImagenArticulo;
import com.nexora.sport.repository.ImagenArticuloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

/** Galeria de fotos de un articulo de inventario (una portada, varias secundarias). */
@Service
public class ImagenArticuloService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/png", "image/jpeg", "image/webp");
    private static final long TAMANIO_MAXIMO = 5L * 1024 * 1024; // 5MB

    private final ImagenArticuloRepository imagenRepository;
    private final FileStorageService fileStorageService;

    public ImagenArticuloService(ImagenArticuloRepository imagenRepository, FileStorageService fileStorageService) {
        this.imagenRepository = imagenRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public List<ImagenArticuloDto> listar(Long articuloId) {
        return imagenRepository.findByArticuloIdOrderByOrdenAsc(articuloId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public String rutaPrincipal(Long articuloId) {
        return imagenRepository.findFirstByArticuloIdAndEsPrincipalTrue(articuloId)
                .map(ImagenArticulo::getRuta).orElse(null);
    }

    @Transactional
    public ImagenArticuloDto subir(ArticuloInventario articulo, MultipartFile file) {
        String ruta = fileStorageService.guardar("articulos", articulo.getId(), file, TIPOS_PERMITIDOS, TAMANIO_MAXIMO);

        ImagenArticulo imagen = new ImagenArticulo();
        imagen.setArticulo(articulo);
        imagen.setRuta(ruta);
        long existentes = imagenRepository.countByArticuloId(articulo.getId());
        imagen.setEsPrincipal(existentes == 0);
        imagen.setOrden((int) existentes);
        return toDto(imagenRepository.save(imagen));
    }

    @Transactional
    public void marcarPrincipal(Long articuloId, Long imagenId) {
        List<ImagenArticulo> imagenes = imagenRepository.findByArticuloIdOrderByOrdenAsc(articuloId);
        boolean encontrada = false;
        for (ImagenArticulo img : imagenes) {
            boolean esEsta = img.getId().equals(imagenId);
            img.setEsPrincipal(esEsta);
            if (esEsta) encontrada = true;
        }
        if (!encontrada) throw new ResourceNotFoundException("Imagen no encontrada");
        imagenRepository.saveAll(imagenes);
    }

    @Transactional
    public void eliminar(Long articuloId, Long imagenId) {
        ImagenArticulo imagen = imagenRepository.findById(imagenId)
                .orElseThrow(() -> new ResourceNotFoundException("Imagen no encontrada"));
        if (!imagen.getArticulo().getId().equals(articuloId)) {
            throw new ResourceNotFoundException("Imagen no encontrada");
        }
        boolean eraPrincipal = imagen.isEsPrincipal();
        imagenRepository.delete(imagen);
        fileStorageService.eliminar(imagen.getRuta());

        if (eraPrincipal) {
            imagenRepository.findFirstByArticuloIdOrderByOrdenAsc(articuloId).ifPresent(siguiente -> {
                siguiente.setEsPrincipal(true);
                imagenRepository.save(siguiente);
            });
        }
    }

    public ImagenArticuloDto toDto(ImagenArticulo i) {
        return new ImagenArticuloDto(i.getId(), i.getRuta(), i.isEsPrincipal(), i.getOrden());
    }
}
