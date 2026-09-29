package com.nexora.sport.repository;

import com.nexora.sport.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {
    /** Roles del centro + el unico rol global de plataforma (SUPER_ADMIN, centro=null).
     * OJO: NO usar "OrEsSistemaTrue" aqui — Dueno/Administrador/Recepcion/Entrenador de
     * CUALQUIER centro tambien son esSistema=true, asi que esa variante filtraba mal y
     * devolvia los roles de sistema de TODOS los centros (fuga de aislamiento). */
    List<Rol> findByCentroIdOrCentroIsNull(Long centroId);
    Optional<Rol> findByNombreAndEsSistemaTrue(String nombre);
    boolean existsByCentroIdAndNombre(Long centroId, String nombre);
}
