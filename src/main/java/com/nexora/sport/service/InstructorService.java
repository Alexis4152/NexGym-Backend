package com.nexora.sport.service;

import com.nexora.sport.dto.InstructorDto;
import com.nexora.sport.dto.InstructorRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.Disciplina;
import com.nexora.sport.model.Instructor;
import com.nexora.sport.model.Sucursal;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.DisciplinaRepository;
import com.nexora.sport.repository.InstructorRepository;
import com.nexora.sport.repository.SucursalRepository;
import com.nexora.sport.repository.UsuarioRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InstructorService {

    private static final Set<String> FOTO_TIPOS_PERMITIDOS = Set.of("image/png", "image/jpeg", "image/webp");
    private static final long FOTO_TAMANIO_MAXIMO = 5L * 1024 * 1024; // 5MB

    private final InstructorRepository instructorRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final CentroRepository centroRepository;
    private final UsuarioRepository usuarioRepository;
    private final SucursalRepository sucursalRepository;
    private final TenantScope tenantScope;
    private final FileStorageService fileStorageService;

    public InstructorService(InstructorRepository instructorRepository, DisciplinaRepository disciplinaRepository,
                              CentroRepository centroRepository, UsuarioRepository usuarioRepository,
                              SucursalRepository sucursalRepository, TenantScope tenantScope,
                              FileStorageService fileStorageService) {
        this.instructorRepository = instructorRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.centroRepository = centroRepository;
        this.usuarioRepository = usuarioRepository;
        this.sucursalRepository = sucursalRepository;
        this.tenantScope = tenantScope;
        this.fileStorageService = fileStorageService;
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
        aplicar(centroId, instructor, request);
        return toDto(instructorRepository.save(instructor));
    }

    @Transactional
    public InstructorDto actualizar(Usuario actor, Long id, InstructorRequest request) {
        Long centroId = tenantScope.scopeId(actor);
        Instructor instructor = buscarDelCentro(actor, centroId, id);
        aplicar(centroId, instructor, request);
        return toDto(instructorRepository.save(instructor));
    }

    @Transactional
    public void desactivar(Usuario actor, Long id) {
        Instructor instructor = buscarDelCentro(actor, tenantScope.scopeId(actor), id);
        instructor.setActivo(false);
        instructorRepository.save(instructor);
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

    private void aplicar(Long centroId, Instructor instructor, InstructorRequest request) {
        instructor.setNombre(request.nombre());
        instructor.setTelefono(request.telefono());
        instructor.setEmail(request.email());
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

        if (request.usuarioId() == null) {
            instructor.setUsuario(null);
        } else {
            Usuario u = usuarioRepository.findById(request.usuarioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
            if (u.getCentro() == null || !u.getCentro().getId().equals(centroId)) {
                throw new ResourceNotFoundException("Usuario no encontrado");
            }
            instructor.setUsuario(u);
        }
    }

    public InstructorDto toDto(Instructor i) {
        return new InstructorDto(
                i.getId(),
                i.getUsuario() != null ? i.getUsuario().getId() : null,
                i.getUsuario() != null ? i.getUsuario().getNombre() : null,
                i.getNombre(), i.getTelefono(), i.getEmail(), i.getEspecialidad(), i.getFotoUrl(),
                i.isActivo(),
                i.getDisciplinas().stream().map(Disciplina::getId).collect(Collectors.toSet()),
                i.getDisciplinas().stream().map(Disciplina::getNombre).collect(Collectors.toSet()),
                i.getSucursales().stream().map(Sucursal::getId).collect(Collectors.toSet()),
                i.getSucursales().stream().map(Sucursal::getNombre).collect(Collectors.toSet())
        );
    }
}
