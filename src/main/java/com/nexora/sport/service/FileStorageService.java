package com.nexora.sport.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Estrategia UNICA de subida/borrado de archivos, reutilizable por cualquier modulo
 * (Articulo, Instructor, Alumno, Centro...). Antes de esto, ImagenArticuloService y
 * DocumentoInstructorService duplicaban la misma logica de validar/nombrar/guardar/
 * borrar con distintos limites — este servicio la centraliza; ambos siguen existiendo
 * porque manejan galerias/entidades propias (varias imagenes, relacion con Disciplina),
 * pero delegan aqui el I/O real.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final Map<String, String> EXTENSIONES = Map.of(
            "image/png", ".png",
            "image/webp", ".webp",
            "image/jpeg", ".jpg",
            "application/pdf", ".pdf"
    );

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    /**
     * Valida, guarda el archivo en {@code uploadsDir/carpeta/entidadId/} con un nombre
     * UUID (nunca el nombre original: evita path traversal y colisiones) y devuelve la
     * ruta publica (p.ej. "/uploads/centros/3/&lt;uuid&gt;.png") para guardar en BD.
     */
    public String guardar(String carpeta, Long entidadId, MultipartFile file, Set<String> tiposPermitidos, long tamanioMaximoBytes) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selecciona un archivo");
        }
        if (file.getSize() > tamanioMaximoBytes) {
            throw new IllegalArgumentException("El archivo no puede pesar mas de " + (tamanioMaximoBytes / (1024 * 1024)) + "MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !tiposPermitidos.contains(contentType)) {
            throw new IllegalArgumentException("Formato de archivo no soportado");
        }
        try {
            Path directorio = Paths.get(uploadsDir, carpeta, String.valueOf(entidadId)).toAbsolutePath().normalize();
            Files.createDirectories(directorio);
            String nombreArchivo = UUID.randomUUID() + EXTENSIONES.getOrDefault(contentType, ".bin");
            Files.copy(file.getInputStream(), directorio.resolve(nombreArchivo), StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + carpeta + "/" + entidadId + "/" + nombreArchivo;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo", e);
        }
    }

    /** Borra el archivo fisico de una ruta publica ya guardada. Best-effort: un fallo solo se loguea. */
    public void eliminar(String rutaPublica) {
        if (rutaPublica == null || rutaPublica.isBlank()) return;
        try {
            String relativo = rutaPublica.replaceFirst("^/uploads/", "");
            Path archivo = Paths.get(uploadsDir, relativo).toAbsolutePath().normalize();
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            log.warn("No se pudo borrar el archivo {}: {}", rutaPublica, e.getMessage());
        }
    }
}
