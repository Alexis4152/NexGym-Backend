package com.nexora.sport.service;

import com.nexora.sport.model.Alumno;
import com.nexora.sport.model.Clase;
import com.nexora.sport.model.Instructor;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.AlumnoRepository;
import com.nexora.sport.repository.ClaseRepository;
import com.nexora.sport.repository.InstructorRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Alcance de un ENTRENADOR: una cuenta OPERATIVA ligada a una ficha de Instructor solo ve sus
 * propias clases y los alumnos registrados a ellas, y nunca agrega, modifica ni elimina alumnos
 * ni sus inscripciones (aunque el dueno le llegara a dar esos permisos en el rol).
 * Dueno/Supervisor/Admin nunca se acotan asi, aunque tambien tengan ficha de instructor.
 *
 * "Alumno registrado a una clase" = inscrito a la disciplina de la clase, con membresia
 * vigente/pendiente que la cubra, o con una reserva activa en esa clase.
 */
@Service
public class AlcanceInstructorService {

    private final InstructorRepository instructorRepository;
    private final ClaseRepository claseRepository;
    private final AlumnoRepository alumnoRepository;
    private final TenantScope tenantScope;

    public AlcanceInstructorService(InstructorRepository instructorRepository, ClaseRepository claseRepository,
                                    AlumnoRepository alumnoRepository, TenantScope tenantScope) {
        this.instructorRepository = instructorRepository;
        this.claseRepository = claseRepository;
        this.alumnoRepository = alumnoRepository;
        this.tenantScope = tenantScope;
    }

    /** La ficha de instructor del actor si debe quedar acotado a ella; vacio = sin restriccion. */
    @Transactional(readOnly = true)
    public Optional<Instructor> instructorAcotado(Usuario actor) {
        if (actor == null || tenantScope.isAdminOSuperior(actor)) return Optional.empty();
        return instructorRepository.findByUsuarioId(actor.getId());
    }

    public boolean esInstructorAcotado(Usuario actor) {
        return instructorAcotado(actor).isPresent();
    }

    /** Para toda accion que agrega/modifica/elimina alumnos o inscripciones. */
    public void assertNoEsInstructor(Usuario actor) {
        if (esInstructorAcotado(actor)) {
            // IllegalStateException (400 con mensaje) en vez de AccessDenied (403 generico) para
            // que el instructor vea por que no puede hacerlo.
            throw new IllegalStateException("Como instructor solo puedes consultar a los alumnos de tus clases");
        }
    }

    @Transactional(readOnly = true)
    public List<Clase> clasesActivas(Instructor instructor) {
        return claseRepository.findByInstructorId(instructor.getId()).stream().filter(Clase::isActivo).toList();
    }

    @Transactional(readOnly = true)
    public Set<Long> claseIds(Instructor instructor) {
        return claseRepository.findByInstructorId(instructor.getId()).stream().map(Clase::getId).collect(Collectors.toSet());
    }

    /** Alumnos de un grupo de clases (las del instructor, o una sola para ver su lista). */
    @Transactional(readOnly = true)
    public Page<Alumno> alumnosDeClases(Long centroId, List<Clase> clases, String q, Pageable pageable) {
        if (clases.isEmpty()) return new PageImpl<>(List.of(), pageable, 0);
        Set<Long> disciplinaIds = clases.stream().map(c -> c.getDisciplina().getId()).collect(Collectors.toSet());
        Set<Long> claseIds = clases.stream().map(Clase::getId).collect(Collectors.toSet());
        String texto = q == null || q.isBlank() ? null : q.trim();
        return alumnoRepository.buscarDeClases(centroId, texto, disciplinaIds, claseIds, pageable);
    }

    @Transactional(readOnly = true)
    public boolean esAlumnoDeSusClases(Instructor instructor, Long centroId, Long alumnoId) {
        List<Clase> clases = clasesActivas(instructor);
        if (clases.isEmpty()) return false;
        // Paginar de a uno filtrando por id seria mas fino; con los tamanos reales de un
        // gimnasio basta revisar la lista completa del instructor.
        return alumnosDeClases(centroId, clases, null, Pageable.unpaged()).stream()
                .anyMatch(a -> a.getId().equals(alumnoId));
    }
}
