package com.nexora.sport.service;

import com.nexora.sport.dto.RolDto;
import com.nexora.sport.dto.RolRequest;
import com.nexora.sport.exception.FieldConflictException;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.Centro;
import com.nexora.sport.model.NivelJerarquia;
import com.nexora.sport.model.Permiso;
import com.nexora.sport.model.Rol;
import com.nexora.sport.model.Seccion;
import com.nexora.sport.repository.RolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.nexora.sport.model.Permiso.*;
import static com.nexora.sport.model.Seccion.*;

@Service
public class RolService {

    private final RolRepository rolRepository;

    public RolService(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @Transactional(readOnly = true)
    public List<RolDto> listar(Long centroId) {
        return rolRepository.findByCentroIdOrCentroIsNull(centroId).stream().map(this::toDto).toList();
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
        rol.setNivel(parseNivelAsignable(request.nivel()));
        rol.setPermisos(parsePermisos(request.permisos()));
        return toDto(rolRepository.save(rol));
    }

    @Transactional
    public RolDto actualizar(Long centroId, Long id, RolRequest request) {
        Rol rol = buscarDelCentro(centroId, id);
        if (rol.isEsSistema()) {
            throw new IllegalStateException("No se puede modificar un rol de sistema");
        }
        rol.setNombre(request.nombre());
        rol.setSecciones(parseSecciones(request.secciones()));
        rol.setNivel(parseNivelAsignable(request.nivel()));
        rol.setPermisos(parsePermisos(request.permisos()));
        return toDto(rolRepository.save(rol));
    }

    @Transactional
    public void eliminar(Long centroId, Long id) {
        Rol rol = buscarDelCentro(centroId, id);
        if (rol.isEsSistema()) {
            throw new IllegalStateException("No se puede eliminar un rol de sistema");
        }
        rol.setActivo(false);
        rolRepository.save(rol);
    }

    /** Nunca devuelve un rol de otro centro (ni el global SUPER_ADMIN): evita que un
     * Dueno/Administrador edite o borre roles ajenos adivinando el id. */
    private Rol buscarDelCentro(Long centroId, Long id) {
        Rol rol = rolRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        if (rol.getCentro() == null || !rol.getCentro().getId().equals(centroId)) {
            throw new ResourceNotFoundException("Rol no encontrado");
        }
        return rol;
    }

    private Set<Seccion> parseSecciones(Set<String> nombres) {
        return nombres.stream().map(Seccion::valueOf).collect(Collectors.toSet());
    }

    private Set<Permiso> parsePermisos(Set<String> nombres) {
        if (nombres == null) return Set.of();
        return nombres.stream().map(Permiso::valueOf).collect(Collectors.toSet());
    }

    /** Un rol creado/editado desde la API de un centro NUNCA puede ser SUPERVISOR ni
     * SUPER_ADMIN (seccion 23 del encargo: nadie puede auto-escalarse ni escalar a
     * otros mas alla de lo que la plataforma ya le dio). Esos dos niveles solo existen
     * via el seed automatico (Dueno al crear un Centro) o el DataSeeder de plataforma. */
    private NivelJerarquia parseNivelAsignable(String nivel) {
        if (nivel == null || nivel.isBlank()) return NivelJerarquia.OPERATIVO;
        NivelJerarquia n = NivelJerarquia.valueOf(nivel);
        if (n == NivelJerarquia.SUPERVISOR || n == NivelJerarquia.SUPER_ADMIN) {
            throw new IllegalStateException("No puedes crear un rol de nivel " + n);
        }
        return n;
    }

    /** Crea el paquete de roles por defecto para un centro nuevo: Dueno/Administrador/
     * Recepcion/Entrenador (ya existian) + dos plantillas sugeridas nuevas (Caja/Ventas,
     * Encargado de Inventario, seccion 10-14 del encargo). Todas con nivel + permisos
     * granulares ademas de secciones, para que "ADMIN" deje de significar acceso total. */
    @Transactional
    public void seedRolesPorDefecto(Centro centro) {
        crearRolSistemaSiNoExiste(centro, "Dueno", NivelJerarquia.SUPERVISOR,
                EnumSet.allOf(Seccion.class), EnumSet.allOf(Permiso.class));

        crearRolSistemaSiNoExiste(centro, "Administrador", NivelJerarquia.ADMIN,
                EnumSet.of(DASHBOARD, ALUMNOS, MEMBRESIAS, CAJA, COMPRAS, DISCIPLINAS, INSTRUCTORES,
                        CLASES, ASISTENCIA, INVENTARIO, TIENDA, REPORTES, USUARIOS, NOTIFICACIONES),
                EnumSet.of(ALUMNOS_VER, ALUMNOS_CREAR, ALUMNOS_EDITAR, ALUMNOS_BAJA,
                        MEMBRESIAS_VER, MEMBRESIAS_CREAR, MEMBRESIAS_RENOVAR, MEMBRESIAS_SUSPENDER, MEMBRESIAS_CANCELAR,
                        PAGOS_VER, PAGOS_REGISTRAR, PAGOS_CANCELAR,
                        CAJA_VER, CAJA_ABRIR, CAJA_MOVIMIENTO, CAJA_CERRAR,
                        INVENTARIO_VER, INVENTARIO_CREAR, INVENTARIO_EDITAR, INVENTARIO_ENTRADA, INVENTARIO_AJUSTE_NEGATIVO,
                        VENTAS_VER, VENTAS_CREAR, VENTAS_CANCELAR,
                        APARTADOS_VER, APARTADOS_GESTIONAR, APARTADOS_CANCELAR,
                        CLASES_VER, CLASES_CREAR, CLASES_EDITAR, CLASES_CANCELAR,
                        ASISTENCIAS_VER, ASISTENCIAS_REGISTRAR, INSTRUCTORES_VER, INSTRUCTORES_ADMINISTRAR,
                        REPORTES_OPERATIVOS, REPORTES_FINANCIEROS, USUARIOS_VER, USUARIOS_CREAR, USUARIOS_EDITAR,
                        NOTIFICACIONES_CONFIGURAR));

        crearRolSistemaSiNoExiste(centro, "Recepcion", NivelJerarquia.OPERATIVO,
                EnumSet.of(DASHBOARD, ALUMNOS, MEMBRESIAS, CAJA, CLASES, ASISTENCIA, INVENTARIO, TIENDA, NOTIFICACIONES),
                EnumSet.of(ALUMNOS_VER, ALUMNOS_CREAR, ALUMNOS_EDITAR,
                        MEMBRESIAS_VER, MEMBRESIAS_CREAR, MEMBRESIAS_RENOVAR, PAGOS_VER, PAGOS_REGISTRAR,
                        APARTADOS_VER, CLASES_VER, ASISTENCIAS_VER, ASISTENCIAS_REGISTRAR));

        crearRolSistemaSiNoExiste(centro, "Entrenador", NivelJerarquia.OPERATIVO,
                EnumSet.of(CLASES, ASISTENCIA, ALUMNOS),
                EnumSet.of(CLASES_VER, ASISTENCIAS_VER, ASISTENCIAS_REGISTRAR, ALUMNOS_VER));

        crearRolSistemaSiNoExiste(centro, "Caja / Ventas", NivelJerarquia.OPERATIVO,
                EnumSet.of(DASHBOARD, CAJA, TIENDA, INVENTARIO),
                EnumSet.of(CAJA_VER, CAJA_ABRIR, CAJA_MOVIMIENTO, CAJA_CERRAR,
                        VENTAS_VER, VENTAS_CREAR, INVENTARIO_VER, APARTADOS_VER, APARTADOS_GESTIONAR));

        crearRolSistemaSiNoExiste(centro, "Encargado de Inventario", NivelJerarquia.OPERATIVO,
                EnumSet.of(DASHBOARD, INVENTARIO),
                EnumSet.of(INVENTARIO_VER, INVENTARIO_CREAR, INVENTARIO_EDITAR, INVENTARIO_ENTRADA));
    }

    private void crearRolSistemaSiNoExiste(Centro centro, String nombre, NivelJerarquia nivel, Set<Seccion> secciones, Set<Permiso> permisos) {
        if (rolRepository.existsByCentroIdAndNombre(centro.getId(), nombre)) return;
        Rol rol = new Rol();
        rol.setCentro(centro);
        rol.setNombre(nombre);
        rol.setEsSistema(true);
        rol.setNivel(nivel);
        rol.setSecciones(secciones);
        rol.setPermisos(permisos);
        rolRepository.save(rol);
    }

    public RolDto toDto(Rol r) {
        return new RolDto(
                r.getId(),
                r.getCentro() != null ? r.getCentro().getId() : null,
                r.getNombre(),
                r.isEsSistema(),
                r.isActivo(),
                r.getSecciones().stream().map(Enum::name).collect(Collectors.toSet()),
                r.getNivel().name(),
                r.getPermisos().stream().map(Enum::name).collect(Collectors.toSet())
        );
    }
}
