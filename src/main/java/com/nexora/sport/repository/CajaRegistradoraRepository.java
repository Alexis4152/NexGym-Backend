package com.nexora.sport.repository;

import com.nexora.sport.model.CajaRegistradora;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** El bloqueo de fila al abrir/cerrar/mover efectivo de una caja va por BloqueoFilas. */
public interface CajaRegistradoraRepository extends JpaRepository<CajaRegistradora, Long> {

    List<CajaRegistradora> findByCentroIdOrderByIdAsc(Long centroId);

    boolean existsBySucursalId(Long sucursalId);

    boolean existsByCentroIdAndSucursalIsNull(Long centroId);
}
