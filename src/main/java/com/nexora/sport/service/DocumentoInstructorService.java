package com.nexora.sport.service;

import com.nexora.sport.dto.DocumentoInstructorDto;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.Disciplina;
import com.nexora.sport.model.DocumentoInstructor;
import com.nexora.sport.model.Instructor;
import com.nexora.sport.repository.DisciplinaRepository;
import com.nexora.sport.repository.DocumentoInstructorRepository;
import com.nexora.sport.repository.InstructorRepository;
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

/** Diplomas/titulos de un instructor, uno o varios por disciplina que imparte. */
@Service
public class DocumentoInstructorService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoInstructorService.class);
    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/png", "image/jpeg", "image/webp", "application/pdf");
    private static final long TAMANIO_MAXIMO = 10L * 1024 * 1024; // 10MB

    private final DocumentoInstructorRepository documentoRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final InstructorRepository instructorRepository;

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    public DocumentoInstructorService(DocumentoInstructorRepository documentoRepository, DisciplinaRepository disciplinaRepository,
                                       InstructorRepository instructorRepository) {
        this.documentoRepository = documentoRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.instructorRepository = instructorRepository;
    }

    @Transactional(readOnly = true)
    public List<DocumentoInstructorDto> listar(Long instructorId) {
        return documentoRepository.findByInstructorIdOrderByCreatedAtDesc(instructorId).stream().map(this::toDto).toList();
    }

    @Transactional
    public DocumentoInstructorDto subir(Long instructorId, Long disciplinaId, MultipartFile file) {
        Instructor instructor = instructorRepository.findWithDisciplinasById(instructorId)
                .orElseThrow(() -> new ResourceNotFoundException("Instructor no encontrado"));
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selecciona un archivo");
        }
        if (file.getSize() > TAMANIO_MAXIMO) {
            throw new IllegalArgumentException("El archivo no puede pesar mas de 10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType)) {
            throw new IllegalArgumentException("Formato no soportado (usa jpg, png, webp o pdf)");
        }
        Disciplina disciplina = disciplinaRepository.findById(disciplinaId)
                .orElseThrow(() -> new ResourceNotFoundException("Disciplina no encontrada"));
        boolean instructorImparte = instructor.getDisciplinas().stream().anyMatch(d -> d.getId().equals(disciplinaId));
        if (!instructorImparte) {
            throw new IllegalArgumentException("El instructor no imparte esa disciplina");
        }

        try {
            Path carpeta = Paths.get(uploadsDir, "instructores", String.valueOf(instructor.getId())).toAbsolutePath().normalize();
            Files.createDirectories(carpeta);
            String extension = extensionPara(contentType);
            String nombreArchivo = UUID.randomUUID() + extension;
            Path destino = carpeta.resolve(nombreArchivo);
            Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            DocumentoInstructor documento = new DocumentoInstructor();
            documento.setInstructor(instructor);
            documento.setDisciplina(disciplina);
            documento.setRuta("/uploads/instructores/" + instructor.getId() + "/" + nombreArchivo);
            documento.setNombreOriginal(file.getOriginalFilename());
            return toDto(documentoRepository.save(documento));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo", e);
        }
    }

    @Transactional
    public void eliminar(Long instructorId, Long documentoId) {
        DocumentoInstructor documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
        if (!documento.getInstructor().getId().equals(instructorId)) {
            throw new ResourceNotFoundException("Documento no encontrado");
        }
        documentoRepository.delete(documento);
        eliminarArchivo(documento.getRuta());
    }

    private void eliminarArchivo(String ruta) {
        try {
            String relativo = ruta.replaceFirst("^/uploads/", "");
            Path archivo = Paths.get(uploadsDir, relativo).toAbsolutePath().normalize();
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            log.warn("No se pudo borrar el archivo de documento {}: {}", ruta, e.getMessage());
        }
    }

    private String extensionPara(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "application/pdf" -> ".pdf";
            default -> ".jpg";
        };
    }

    public DocumentoInstructorDto toDto(DocumentoInstructor d) {
        return new DocumentoInstructorDto(
                d.getId(), d.getDisciplina().getId(), d.getDisciplina().getNombre(),
                d.getRuta(), d.getNombreOriginal(), d.getCreatedAt()
        );
    }
}
