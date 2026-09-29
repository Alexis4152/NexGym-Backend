package com.nexora.sport.config;

import com.nexora.sport.model.NivelJerarquia;
import com.nexora.sport.model.Permiso;
import com.nexora.sport.model.Rol;
import com.nexora.sport.model.Seccion;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.RolRepository;
import com.nexora.sport.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

/** Siembra el rol SUPER_ADMIN de plataforma y un primer usuario super admin si no existen. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.super-admin-email:admin@nexorasport.com}")
    private String superAdminEmail;

    @Value("${app.seed.super-admin-password:CambiaEsta123}")
    private String superAdminPassword;

    public DataSeeder(RolRepository rolRepository, UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Rol superAdminRol = rolRepository.findByNombreAndEsSistemaTrue("SUPER_ADMIN").orElseGet(() -> {
            Rol r = new Rol();
            r.setCentro(null);
            r.setNombre("SUPER_ADMIN");
            r.setEsSistema(true);
            r.setNivel(NivelJerarquia.SUPER_ADMIN);
            r.setSecciones(EnumSet.allOf(Seccion.class));
            r.setPermisos(EnumSet.allOf(Permiso.class));
            return rolRepository.save(r);
        });

        if (!usuarioRepository.existsByEmail(superAdminEmail)) {
            Usuario u = new Usuario();
            u.setCentro(null);
            u.setRol(superAdminRol);
            u.setNombre("Super Admin");
            u.setEmail(superAdminEmail);
            u.setPasswordHash(passwordEncoder.encode(superAdminPassword));
            u.setMustChangePassword(true);
            usuarioRepository.save(u);
            log.info("Usuario SUPER_ADMIN creado: {} (cambia la contrasena temporal al entrar)", superAdminEmail);
        }
    }
}
