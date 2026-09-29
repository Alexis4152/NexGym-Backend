package com.nexora.sport.service;

import com.nexora.sport.dto.AlumnoDto;
import com.nexora.sport.dto.AlumnoRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.Alumno;
import com.nexora.sport.model.Disciplina;
import com.nexora.sport.model.EstadoAlumno;
import com.nexora.sport.model.Sucursal;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.AlumnoRepository;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.DisciplinaRepository;
import com.nexora.sport.repository.SucursalRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AlumnoService {

    private static final Set<String> FOTO_TIPOS_PERMITIDOS = Set.of("image/png", "image/jpeg", "image/webp");
    private static final long FOTO_TAMANIO_MAXIMO = 5L * 1024 * 1024; // 5MB

    private final AlumnoRepository alumnoRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final CentroRepository centroRepository;
    private final SucursalRepository sucursalRepository;
    private final TenantScope tenantScope;
    private final FileStorageService fileStorageService;

    public AlumnoService(AlumnoRepository alumnoRepository, DisciplinaRepository disciplinaRepository,
                          CentroRepository centroRepository, SucursalRepository sucursalRepository,
                          TenantScope tenantScope, FileStorageService fileStorageService) {
        this.alumnoRepository = alumnoRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.centroRepository = centroRepository;
        this.sucursalRepository = sucursalRepository;
        this.tenantScope = tenantScope;
        this.fileStorageService = fileStorageService;
    }

    /** Los alumnos ya NO estan ligados a una sucursal especifica (seccion 27 del encargo
     * de jerarquias): a que sucursal(es) "pertenecen" lo determinan las clases/disciplinas
     * de su membresia, no un campo fijo. Por eso listar() y buscarEnAlcance() ya solo
     * acotan por Centro -- cualquier Admin/Operativo del centro puede ver/gestionar
     * cualquier alumno, sin importar en que sucursal se inscribio originalmente. */
    @Transactional(readOnly = true)
    public PageResponse<AlumnoDto> listar(Usuario actor, String q, Pageable pageable) {
        Long centroId = tenantScope.scopeId(actor);
        var page = (q == null || q.isBlank())
                ? alumnoRepository.findByCentroIdAndDeletedAtIsNull(centroId, pageable)
                : alumnoRepository.findByCentroIdAndDeletedAtIsNullAndNombreContainingIgnoreCase(centroId, q, pageable);
        return PageResponse.of(page, this::toDto);
    }

    public Alumno buscar(Long id) {
        return alumnoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));
    }

    /** Nunca devuelve un alumno de otro Centro (evita que alguien cruce de tenant
     * adivinando el id) -- ya no acota por sucursal, ver nota de arriba. */
    private Alumno buscarEnAlcance(Usuario actor, Long id) {
        Alumno alumno = buscar(id);
        Long centroId = tenantScope.scopeId(actor);
        if (alumno.getCentro() == null || !alumno.getCentro().getId().equals(centroId)) {
            throw new ResourceNotFoundException("Alumno no encontrado");
        }
        return alumno;
    }

    @Transactional(readOnly = true)
    public AlumnoDto obtener(Usuario actor, Long id) {
        return toDto(buscarEnAlcance(actor, id));
    }

    @Transactional
    public AlumnoDto crear(Usuario actor, AlumnoRequest request) {
        Long centroId = tenantScope.scopeId(actor);
        Alumno alumno = new Alumno();
        alumno.setCentro(centroRepository.getReferenceById(centroId));
        alumno.setSucursal(resolverSucursal(centroId, request.sucursalId()));
        aplicar(alumno, request);
        return toDto(alumnoRepository.save(alumno));
    }

    @Transactional
    public AlumnoDto actualizar(Usuario actor, Long id, AlumnoRequest request) {
        Long centroId = tenantScope.scopeId(actor);
        Alumno alumno = buscarEnAlcance(actor, id);
        alumno.setSucursal(resolverSucursal(centroId, request.sucursalId()));
        aplicar(alumno, request);
        return toDto(alumnoRepository.save(alumno));
    }

    @Transactional
    public void desactivar(Usuario actor, Long id) {
        Alumno alumno = buscarEnAlcance(actor, id);
        alumno.setEstado(EstadoAlumno.INACTIVO);
        alumno.setFechaBaja(java.time.LocalDateTime.now());
        alumnoRepository.save(alumno);
    }

    @Transactional
    public AlumnoDto subirFoto(Usuario actor, Long id, MultipartFile file) {
        Alumno alumno = buscarEnAlcance(actor, id);
        String rutaAnterior = alumno.getFotoUrl();
        String ruta = fileStorageService.guardar("alumnos", alumno.getId(), file, FOTO_TIPOS_PERMITIDOS, FOTO_TAMANIO_MAXIMO);
        alumno.setFotoUrl(ruta);
        alumno = alumnoRepository.save(alumno);
        if (rutaAnterior != null) fileStorageService.eliminar(rutaAnterior);
        return toDto(alumno);
    }

    /** Puramente informativo (p. ej. "donde se registro"): ya no se usa para control de
     * acceso (ver nota en listar()/buscarEnAlcance()), asi que solo valida que la
     * sucursal elegida (si eligieron alguna) sea del mismo Centro. */
    private Sucursal resolverSucursal(Long centroId, Long sucursalIdSolicitada) {
        if (sucursalIdSolicitada == null) return null;
        Sucursal s = sucursalRepository.findById(sucursalIdSolicitada).orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
        if (!s.getCentro().getId().equals(centroId)) {
            throw new ResourceNotFoundException("Sucursal no encontrada");
        }
        return s;
    }

    private void aplicar(Alumno alumno, AlumnoRequest request) {
        alumno.setNombre(request.nombre());
        alumno.setFechaNacimiento(request.fechaNacimiento());
        alumno.setTelefono(request.telefono());
        alumno.setEmail(request.email());
        alumno.setContactoEmergenciaNombre(request.contactoEmergenciaNombre());
        alumno.setContactoEmergenciaTelefono(request.contactoEmergenciaTelefono());
        alumno.setObservaciones(request.observaciones());
        if (request.estado() != null) {
            EstadoAlumno nuevoEstado = EstadoAlumno.valueOf(request.estado());
            // Registra el momento de la baja mas reciente; no se limpia al reactivarse (ver Alumno#fechaBaja).
            if (nuevoEstado != EstadoAlumno.ACTIVO && alumno.getEstado() == EstadoAlumno.ACTIVO) {
                alumno.setFechaBaja(java.time.LocalDateTime.now());
            }
            alumno.setEstado(nuevoEstado);
        }
        Set<Disciplina> disciplinas = new HashSet<>();
        if (request.disciplinaIds() != null) {
            request.disciplinaIds().forEach(id -> disciplinas.add(disciplinaRepository.getReferenceById(id)));
        }
        alumno.setDisciplinas(disciplinas);
    }

    public AlumnoDto toDto(Alumno a) {
        return new AlumnoDto(
                a.getId(),
                a.getSucursal() != null ? a.getSucursal().getId() : null,
                a.getSucursal() != null ? a.getSucursal().getNombre() : null,
                a.getNombre(), a.getFechaNacimiento(), a.getTelefono(), a.getEmail(),
                a.getContactoEmergenciaNombre(), a.getContactoEmergenciaTelefono(), a.getFotoUrl(),
                a.getObservaciones(), a.getEstado().name(), a.getFechaIngreso(),
                a.getDisciplinas().stream().map(Disciplina::getId).collect(Collectors.toSet()),
                a.getDisciplinas().stream().map(Disciplina::getNombre).collect(Collectors.toSet())
        );
    }
}
