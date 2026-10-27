package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Diploma o titulo de un instructor, asociado a una de las disciplinas que imparte. */
@Entity
@Table(name = "documentos_instructor")
@Getter
@Setter
public class DocumentoInstructor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private Instructor instructor;

    /** Ya no se usa al subir (un solo diploma por instructor, ver DocumentoInstructorService);
     * solo queda en los documentos anteriores. La columna se volvio nullable en MigracionesUnaVez. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disciplina_id")
    private Disciplina disciplina;

    @Column(nullable = false, length = 300)
    private String ruta;

    @Column(name = "nombre_original", length = 200)
    private String nombreOriginal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
