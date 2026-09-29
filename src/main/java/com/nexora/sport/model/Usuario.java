package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_id")
    private Centro centro;

    /** Alcance por sucursal (nivel ADMIN/OPERATIVO tipico): sucursal "hogar". Sin
     * sucursal Y sin sucursalesAdicionales = todas las sucursales del centro. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    /** Sucursales adicionales que un ADMIN/OPERATIVO (gerente, recepcionista, etc.)
     * tambien puede ver/operar, ademas de su sucursal "hogar" -- el Dueno se las asigna.
     * Aditivo: un usuario de una sola sucursal deja esto vacio y todo sigue igual. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "usuario_sucursales_adicionales",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "sucursal_id"))
    private Set<Sucursal> sucursalesAdicionales = new HashSet<>();

    /**
     * Centros adicionales que un SUPERVISOR (Dueno) puede tambien administrar,
     * ademas de su centro "hogar" (campo centro, arriba). Aditivo: un dueno de
     * un solo centro deja esto vacio y todo funciona exactamente igual que
     * antes (centro fijo, sin selector). Reutiliza el mismo mecanismo de
     * header X-Acting-Centro-Id + pantalla SelectCentro que ya usaba SUPER_ADMIN.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "usuario_centros_adicionales",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "centro_id"))
    private Set<Centro> centrosAdicionales = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Rol rol;

    /** Excepciones puntuales sobre los permisos de su Rol (seccion 22 del encargo: sin crear un rol nuevo por una persona). */
    @ElementCollection(targetClass = Permiso.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_permisos_extra", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permiso")
    private Set<Permiso> permisosExtra = new HashSet<>();

    @ElementCollection(targetClass = Permiso.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_permisos_revocados", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permiso")
    private Set<Permiso> permisosRevocados = new HashSet<>();

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ---- UserDetails ----
    @Override
    @Transient
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.getNombre().toUpperCase().replace(' ', '_')));
    }

    @Override
    @Transient
    public String getPassword() {
        return passwordHash;
    }

    @Override
    @Transient
    public String getUsername() {
        return email;
    }

    @Override
    @Transient
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @Transient
    public boolean isAccountNonLocked() {
        return activo && deletedAt == null;
    }

    @Override
    @Transient
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @Transient
    public boolean isEnabled() {
        return activo && deletedAt == null;
    }
}
