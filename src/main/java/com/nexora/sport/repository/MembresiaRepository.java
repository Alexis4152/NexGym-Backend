package com.nexora.sport.repository;

import com.nexora.sport.model.EstadoMembresia;
import com.nexora.sport.model.Membresia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MembresiaRepository extends JpaRepository<Membresia, Long> {

    Page<Membresia> findByCentroId(Long centroId, Pageable pageable);

    Page<Membresia> findByAlumnoId(Long alumnoId, Pageable pageable);

    List<Membresia> findByAlumnoIdAndEstado(Long alumnoId, EstadoMembresia estado);

    long countByCentroIdAndEstado(Long centroId, EstadoMembresia estado);

    @Query("select m from Membresia m where m.centro.id = :centroId and m.estado = 'ACTIVA' " +
           "and m.fechaFin between :hoy and :limite order by m.fechaFin asc")
    List<Membresia> findProximasAVencer(@Param("centroId") Long centroId,
                                         @Param("hoy") LocalDate hoy,
                                         @Param("limite") LocalDate limite);

    @Query("select m from Membresia m where m.centro.id = :centroId and m.estado = 'ACTIVA' " +
           "and m.fechaFin < :hoy order by m.fechaFin asc")
    List<Membresia> findVencidasNoActualizadas(@Param("centroId") Long centroId, @Param("hoy") LocalDate hoy);

    @Query("select m from Membresia m where m.estado = 'ACTIVA' and m.fechaFin < :hoy")
    List<Membresia> findAllVencidasNoActualizadas(@Param("hoy") LocalDate hoy);
}
