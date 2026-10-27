package com.nexora.sport.service;

import com.nexora.sport.dto.InstructorDto;
import com.nexora.sport.dto.InstructorRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.exception.FieldConflictException;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.Disciplina;
import com.nexora.sport.model.Instructor;
import com.nexora.sport.model.Sucursal;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.DisciplinaRepository;
import com.nexora.sport.repository.InstructorRepository;
import com.nexora.sport.repository.SucursalRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InstructorService {

    private static final Set<String> FOTO_TIPOS_PERMITIDOS = Set.of("image/png", "image/jpeg", "image/webp");
    private static final long FOTO_TAMANIO_MAXIMO = 5L * 1024 * 1024; // 5MB

    private final InstructorRepository instructorRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final CentroRepository centroRepository;
    private final SucursalRepository sucursalRepository;
    private final TenantScope tenantScope;
    private final FileStorageService fileStorageService;
    private final NotificacionService notificacionService;
    private final UsuarioService usuarioService;

    public InstructorService(InstructorRepository instructorRepository, DisciplinaRepository disciplinaRepository,
                              CentroRepository centroRepository, SucursalRepository sucursalRepository,
                              TenantScope tenantScope, FileStorageService fileStorageService,
                              NotificacionService notificacionService, UsuarioService usuarioService) {
        this.instructorRepository = instructorRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.centroRepository = centroRepository;
        this.sucursalRepository = sucursalRepository;
        this.tenantScope = tenantScope;
        this.fileStorageService = fileStorageService;
        this.notificacionService = notificacionService;
        this.usuarioService = usuarioService;
    }

    /** Filtra por la SUCURSAL ACTIVA (una sola, ver TenantScope#sucursalActivaId):
     * un instructor disponible en varias sucursales solo aparece cuando la activa es
     * una de las suyas (o si el instructor no tiene ninguna asignada = disponible en
     * todas). No ve instructores de otra sucursal (seccion 27 del encargo). */
    @Transactional(readOnly = true)
    public PageResponse<InstructorDto> listar(Usuario actor, String q, Pageable pageable) {
        Long centroId = tenantScope.scopeId(actor);
        Long sucursalActiva = tenantScope.sucursalActivaId(actor);
        String texto = (q == null || q.isBlank()) ? null : q;
        // Sin restriccion (SUPER_ADMIN/SUPERVISOR): metodos simples, sin filtro de
        // sucursal -- ver nota en InstructorRepository sobre por que NO se le pasa un
        // Set nulo a buscarEnAlcance.
        if (sucursalActiva == null) {
            var page = texto == null
                    ? instructorRepository.findByCentroId(centroId, pageable)
                    : instructorRepository.findByCentroIdAndNombreContainingIgnoreCase(centroId, texto, pageable);
            return PageResponse.of(page, this::toDto);
        }
        return PageResponse.of(
                instructorRepository.buscarEnAlcance(centroId, Set.of(sucursalActiva), texto, pageable), this::toDto);
    }

    public Instructor buscar(Long id) {
        return instructorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Instructor no encontrado"));
    }

    private Instructor buscarDelCentro(Usuario actor, Long centroId, Long id) {
        Instructor i = buscar(id);
        if (i.getCentro() == null || !i.getCentro().getId().equals(centroId)) {
            throw new ResourceNotFoundException("Instructor no encontrado");
        }
        boolean disponibleEnAlcance = i.getSucursales().isEmpty()
                || i.getSucursales().stream().anyMatch(s -> tenantScope.sucursalPermite(actor, s.getId()));
        if (!disponibleEnAlcance) {
            throw new ResourceNotFoundException("Instructor no encontrado");
        }
        return i;
    }

    @Transactional
    public InstructorDto crear(Usuario actor, InstructorRequest request) {
        Long centroId = tenantScope.scopeId(actor);
        Instructor instructor = new Instructor();
        instructor.setCentro(centroRepository.getReferenceById(centroId));
        aplicar(actor, centroId, instructor, request);
        return toDto(instructorRepository.save(instructor));
    }

    @Transactional
    public InstructorDto actualizar(Usuario actor, Long id, InstructorRequest request) {
        Long centroId = tenantScope.scopeId(actor);
        Instructor instructor = buscarDelCentro(actor, centroId, id);
        Set<Long> disciplinasAntes = instructor.getDisciplinas().stream().map(Disciplina::getId).collect(Collectors.toSet());
        Set<Long> sucursalesAntesIds = instructor.getSucursales().stream().map(Sucursal::getId).collect(Collectors.toSet());

        aplicar(actor, centroId, instructor, request);
        instructor = instructorRepository.save(instructor);

        for (Disciplina d : instructor.getDisciplinas()) {
            if (!disciplinasAntes.contains(d.getId())) {
                notificacionService.notificarInstructorDisciplinaAsignada(instructor, d);
            }
        }
        Set<Long> sucursalesDespuesIds = instructor.getSucursales().stream().map(Sucursal::getId).collect(Collectors.toSet());
        if (!sucursalesAntesIds.equals(sucursalesDespuesIds)) {
            String texto = instructor.getSucursales().isEmpty() ? "todas"
                    : instructor.getSucursales().stream().map(Sucursal::getNombre).collect(Collectors.joining(", "));
            notificacionService.notificarInstructorSucursalCambiada(instructor, texto);
        }
        return toDto(instructor);
    }

    @Transactional
    public void desactivar(Usuario actor, Long id) {
        Instructor instructor = buscarDelCentro(actor, tenantScope.scopeId(actor), id);
        instructor.setActivo(false);
        instructorRepository.save(instructor);
        usuarioService.desactivarCuentaInstructor(instructor.getUsuario());
    }

    @Transactional
    public InstructorDto subirFoto(Usuario actor, Long id, MultipartFile file) {
        Instructor instructor = buscarDelCentro(actor, tenantScope.scopeId(actor), id);
        String rutaAnterior = instructor.getFotoUrl();
        // Carpeta distinta a la de DocumentoInstructorService ("instructores") para no
        // mezclar fotos de perfil con diplomas/documentos en el mismo directorio.
        String ruta = fileStorageService.guardar("instructores-fotos", instructor.getId(), file, FOTO_TIPOS_PERMITIDOS, FOTO_TAMANIO_MAXIMO);
        instructor.setFotoUrl(ruta);
        instructor = instructorRepository.save(instructor);
        if (rutaAnterior != null) fileStorageService.eliminar(rutaAnterior);
        return toDto(instructor);
    }

    private void aplicar(Usuario actor, Long centroId, Instructor instructor, InstructorRequest request) {
        instructor.setNombre(request.nombre().trim());
        instructor.setTelefono(request.telefono());
        instructor.setEmail(normalizarEmail(request.email()));
        instructor.setEmailPersonal(normalizarEmail(request.emailPersonal()));
        instructor.setEspecialidad(request.especialidad());
        Set<Disciplina> disciplinas = new HashSet<>();
        if (request.disciplinaIds() != null) {
            request.disciplinaIds().forEach(id -> disciplinas.add(disciplinaRepository.getReferenceById(id)));
        }
        instructor.setDisciplinas(disciplinas);

        Set<Sucursal> sucursales = new HashSet<>();
        if (request.sucursalIds() != null) {
            for (Long id : request.sucursalIds()) {
                Sucursal s = sucursalRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
                if (!s.getCentro().getId().equals(centroId)) {
                    throw new ResourceNotFoundException("Sucursal no encontrada");
                }
                sucursales.add(s);
            }
        }
        instructor.setSucursales(sucursales);

        vincularCuenta(actor, centroId, instructor);
    }

    /** El correo de acceso de la ficha ES el usuario de login del instructor: si la ficha aun no
     * tiene cuenta (alta nueva, o instructor anterior a esta regla) se crea o se liga la existente
     * con ese correo; si ya la tiene, se le sincronizan nombre, correo y sucursales. */
    private void vincularCuenta(Usuario actor, Long centroId, Instructor instructor) {
        List<Sucursal> sucursalesCuenta = sucursalesParaCuenta(actor, centroId, instructor);
        Usuario cuenta = instructor.getUsuario();
        if (cuenta != null) {
            usuarioService.sincronizarCuentaInstructor(actor, cuenta, instructor.getNombre(), instructor.getEmail(), sucursalesCuenta);
            return;
        }
        cuenta = usuarioService.obtenerOCrearCuentaInstructor(actor, centroId, instructor.getNombre(), instructor.getEmail(), sucursalesCuenta);
        Long cuentaId = cuenta.getId();
        instructorRepository.findByUsuarioId(cuentaId)
                .filter(otra -> !otra.getId().equals(instructor.getId()))
                .ifPresent(otra -> {
                    throw new FieldConflictException("email", "Ese correo ya es el acceso del instructor \"" + otra.getNombre() + "\".");
                });
        instructor.setUsuario(cuenta);
    }

    /** Las sucursales donde da clases; si son "todas" (ninguna marcada), todas las activas que el
     * actor administra -- ordenadas por nombre para que la sucursal "hogar" sea estable. */
    private List<Sucursal> sucursalesParaCuenta(Usuario actor, Long centroId, Instructor instructor) {
        List<Sucursal> base = instructor.getSucursales().isEmpty()
                ? sucursalRepository.findByCentroIdAndActivoTrueOrderByNombre(centroId).stream()
                        .filter(s -> tenantScope.sucursalPermite(actor, s.getId())).toList()
                : new ArrayList<>(instructor.getSucursales());
        return base.stream().sorted(Comparator.comparing(Sucursal::getNombre)).toList();
    }

    private static String normalizarEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase();
    }

    public InstructorDto toDto(Instructor i) {
        return new InstructorDto(
                i.getId(),
                i.getUsuario() != null ? i.getUsuario().getId() : null,
                i.getUsuario() != null ? i.getUsuario().getNombre() : null,
                i.getNombre(), i.getTelefono(), i.getEmail(), i.getEmailPersonal(), i.getEspecialidad(), i.getFotoUrl(),
                i.isActivo(),
                i.getDisciplinas().stream().map(Disciplina::getId).collect(Collectors.toSet()),
                i.getDisciplinas().stream().map(Disciplina::getNombre).collect(Collectors.toSet()),
                i.getSucursales().stream().map(Sucursal::getId).collect(Collectors.toSet()),
                i.getSucursales().stream().map(Sucursal::getNombre).collect(Collectors.toSet())
        );
    }
}
