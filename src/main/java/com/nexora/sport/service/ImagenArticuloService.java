package com.nexora.sport.service;

import com.nexora.sport.dto.ImagenArticuloDto;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.ArticuloInventario;
import com.nexora.sport.model.ImagenArticulo;
import com.nexora.sport.repository.ImagenArticuloRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Galeria de fotos de un articulo de inventario (una portada, varias secundarias). */
@Service
public class ImagenArticuloService {

    private static final Logger log = LoggerFactory.getLogger(ImagenArticuloService.class);
    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/png", "image/jpeg", "image/webp");
    private static final long TAMANIO_MAXIMO = 5L * 1024 * 1024; // 5MB

    private final ImagenArticuloRepository imagenRepository;

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    public ImagenArticuloService(ImagenArticuloRepository imagenRepository) {
        this.imagenRepository = imagenRepository;
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
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selecciona una imagen");
        }
        if (file.getSize() > TAMANIO_MAXIMO) {
            throw new IllegalArgumentException("La imagen no puede pesar mas de 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType)) {
            throw new IllegalArgumentException("Formato de imagen no soportado (usa jpg, png o webp)");
        }

        try {
            Path carpeta = Paths.get(uploadsDir, "articulos", String.valueOf(articulo.getId())).toAbsolutePath().normalize();
            Files.createDirectories(carpeta);
            String extension = extensionPara(contentType);
            String nombreArchivo = UUID.randomUUID() + extension;
            Path destino = carpeta.resolve(nombreArchivo);
            Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            ImagenArticulo imagen = new ImagenArticulo();
            imagen.setArticulo(articulo);
            imagen.setRuta("/uploads/articulos/" + articulo.getId() + "/" + nombreArchivo);
            long existentes = imagenRepository.countByArticuloId(articulo.getId());
            imagen.setEsPrincipal(existentes == 0);
            imagen.setOrden((int) existentes);
            return toDto(imagenRepository.save(imagen));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen", e);
        }
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
        eliminarArchivo(imagen.getRuta());

        if (eraPrincipal) {
            imagenRepository.findFirstByArticuloIdOrderByOrdenAsc(articuloId).ifPresent(siguiente -> {
                siguiente.setEsPrincipal(true);
                imagenRepository.save(siguiente);
            });
        }
    }

    private void eliminarArchivo(String ruta) {
        try {
            String relativo = ruta.replaceFirst("^/uploads/", "");
            Path archivo = Paths.get(uploadsDir, relativo).toAbsolutePath().normalize();
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            log.warn("No se pudo borrar el archivo de imagen {}: {}", ruta, e.getMessage());
        }
    }

    private String extensionPara(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    public ImagenArticuloDto toDto(ImagenArticulo i) {
        return new ImagenArticuloDto(i.getId(), i.getRuta(), i.isEsPrincipal(), i.getOrden());
    }
}
