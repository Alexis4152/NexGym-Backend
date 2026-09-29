package com.nexora.sport.service;

import com.nexora.sport.dto.DocumentoInstructorDto;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.Disciplina;
import com.nexora.sport.model.DocumentoInstructor;
import com.nexora.sport.model.Instructor;
import com.nexora.sport.repository.DisciplinaRepository;
import com.nexora.sport.repository.DocumentoInstructorRepository;
import com.nexora.sport.repository.InstructorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

/** Diplomas/titulos de un instructor, uno o varios por disciplina que imparte. */
@Service
public class DocumentoInstructorService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/png", "image/jpeg", "image/webp", "application/pdf");
    private static final long TAMANIO_MAXIMO = 10L * 1024 * 1024; // 10MB

    private final DocumentoInstructorRepository documentoRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final InstructorRepository instructorRepository;
    private final FileStorageService fileStorageService;

    public DocumentoInstructorService(DocumentoInstructorRepository documentoRepository, DisciplinaRepository disciplinaRepository,
                                       InstructorRepository instructorRepository, FileStorageService fileStorageService) {
        this.documentoRepository = documentoRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.instructorRepository = instructorRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public List<DocumentoInstructorDto> listar(Long instructorId) {
        return documentoRepository.findByInstructorIdOrderByCreatedAtDesc(instructorId).stream().map(this::toDto).toList();
    }

    @Transactional
    public DocumentoInstructorDto subir(Long instructorId, Long disciplinaId, MultipartFile file) {
        Instructor instructor = instructorRepository.findWithDisciplinasById(instructorId)
                .orElseThrow(() -> new ResourceNotFoundException("Instructor no encontrado"));
        Disciplina disciplina = disciplinaRepository.findById(disciplinaId)
                .orElseThrow(() -> new ResourceNotFoundException("Disciplina no encontrada"));
        boolean instructorImparte = instructor.getDisciplinas().stream().anyMatch(d -> d.getId().equals(disciplinaId));
        if (!instructorImparte) {
            throw new IllegalArgumentException("El instructor no imparte esa disciplina");
        }

        String ruta = fileStorageService.guardar("instructores", instructor.getId(), file, TIPOS_PERMITIDOS, TAMANIO_MAXIMO);

        DocumentoInstructor documento = new DocumentoInstructor();
        documento.setInstructor(instructor);
        documento.setDisciplina(disciplina);
        documento.setRuta(ruta);
        documento.setNombreOriginal(file.getOriginalFilename());
        return toDto(documentoRepository.save(documento));
    }

    @Transactional
    public void eliminar(Long instructorId, Long documentoId) {
        DocumentoInstructor documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
        if (!documento.getInstructor().getId().equals(instructorId)) {
            throw new ResourceNotFoundException("Documento no encontrado");
        }
        documentoRepository.delete(documento);
        fileStorageService.eliminar(documento.getRuta());
    }

    public DocumentoInstructorDto toDto(DocumentoInstructor d) {
        return new DocumentoInstructorDto(
                d.getId(), d.getDisciplina().getId(), d.getDisciplina().getNombre(),
                d.getRuta(), d.getNombreOriginal(), d.getCreatedAt()
        );
    }
}
