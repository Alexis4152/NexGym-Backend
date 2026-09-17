package com.nexora.sport.service;

import com.nexora.sport.dto.RolDto;
import com.nexora.sport.dto.RolRequest;
import com.nexora.sport.exception.FieldConflictException;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.Centro;
import com.nexora.sport.model.Rol;
import com.nexora.sport.model.Seccion;
import com.nexora.sport.repository.RolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RolService {

    private final RolRepository rolRepository;

    public RolService(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @Transactional(readOnly = true)
    public List<RolDto> listar(Long centroId) {
        return rolRepository.findByCentroIdOrEsSistemaTrue(centroId).stream().map(this::toDto).toList();
    }

    @Transactional
    public RolDto crear(Long centroId, Centro centro, RolRequest request) {
        if (rolRepository.existsByCentroIdAndNombre(centroId, request.nombre())) {
            throw new FieldConflictException("nombre", "Ya existe un rol con ese nombre");
        }
        Rol rol = new Rol();
        rol.setCentro(centro);
        rol.setNombre(request.nombre());
        rol.setSecciones(parseSecciones(request.secciones()));
        return toDto(rolRepository.save(rol));
    }

    @Transactional
    public RolDto actualizar(Long id, RolRequest request) {
        Rol rol = rolRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        if (rol.isEsSistema()) {
            throw new IllegalStateException("No se puede modificar un rol de sistema");
        }
        rol.setNombre(request.nombre());
        rol.setSecciones(parseSecciones(request.secciones()));
        return toDto(rolRepository.save(rol));
    }

    @Transactional
    public void eliminar(Long id) {
        Rol rol = rolRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        if (rol.isEsSistema()) {
            throw new IllegalStateException("No se puede eliminar un rol de sistema");
        }
        rol.setActivo(false);
        rolRepository.save(rol);
    }

    private Set<Seccion> parseSecciones(Set<String> nombres) {
        return nombres.stream().map(Seccion::valueOf).collect(Collectors.toSet());
    }

    /** Crea el paquete de roles por defecto (Dueno, Administrador, Recepcion, Entrenador) para un centro nuevo. */
    @Transactional
    public void seedRolesPorDefecto(Centro centro) {
        crearRolSistemaSiNoExiste(centro, "Dueno", EnumSet.allOf(Seccion.class));

        crearRolSistemaSiNoExiste(centro, "Administrador", EnumSet.of(
                Seccion.DASHBOARD, Seccion.ALUMNOS, Seccion.MEMBRESIAS, Seccion.CAJA, Seccion.COMPRAS,
                Seccion.DISCIPLINAS, Seccion.INSTRUCTORES, Seccion.CLASES, Seccion.ASISTENCIA,
                Seccion.INVENTARIO, Seccion.TIENDA, Seccion.REPORTES, Seccion.USUARIOS));

        crearRolSistemaSiNoExiste(centro, "Recepcion", EnumSet.of(
                Seccion.DASHBOARD, Seccion.ALUMNOS, Seccion.MEMBRESIAS, Seccion.CAJA,
                Seccion.CLASES, Seccion.ASISTENCIA, Seccion.INVENTARIO, Seccion.TIENDA));

        crearRolSistemaSiNoExiste(centro, "Entrenador", EnumSet.of(
                Seccion.CLASES, Seccion.ASISTENCIA, Seccion.ALUMNOS));
    }

    private void crearRolSistemaSiNoExiste(Centro centro, String nombre, Set<Seccion> secciones) {
        if (rolRepository.existsByCentroIdAndNombre(centro.getId(), nombre)) return;
        Rol rol = new Rol();
        rol.setCentro(centro);
        rol.setNombre(nombre);
        rol.setEsSistema(true);
        rol.setSecciones(secciones);
        rolRepository.save(rol);
    }

    public RolDto toDto(Rol r) {
        return new RolDto(
                r.getId(),
                r.getCentro() != null ? r.getCentro().getId() : null,
                r.getNombre(),
                r.isEsSistema(),
                r.isActivo(),
                r.getSecciones().stream().map(Enum::name).collect(Collectors.toSet())
        );
    }
}
