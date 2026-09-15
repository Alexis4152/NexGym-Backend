package com.nexora.sport.repository;

import com.nexora.sport.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {
    List<Rol> findByCentroIdOrEsSistemaTrue(Long centroId);
    Optional<Rol> findByNombreAndEsSistemaTrue(String nombre);
    boolean existsByCentroIdAndNombre(Long centroId, String nombre);
}
