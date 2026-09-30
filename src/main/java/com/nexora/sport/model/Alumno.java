package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "alumnos")
@Getter
@Setter
public class Alumno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_id", nullable = false)
    private Centro centro;

    /** Sucursal donde esta inscrito (alcance de un Admin restringido a una sucursal). Null = visible centro-wide. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(length = 30)
    private String telefono;

    @Column(length = 150)
    private String email;

    @Column(name = "contacto_emergencia_nombre", length = 150)
    private String contactoEmergenciaNombre;

    @Column(name = "contacto_emergencia_telefono", length = 30)
    private String contactoEmergenciaTelefono;

    @Column(name = "foto_url", length = 300)
    private String fotoUrl;

    /** Token opaco unico (UUID) para el QR personal del alumno: lo genera
     * AlumnoService#crear y lo escanea un lector fisico (mismo mecanismo que un
     * codigo de barras) en Asistencia/POS/formularios de busqueda para identificarlo
     * sin escribir su nombre. Globalmente unico (no solo por centro), asi que su
     * busqueda no necesita acotarse por centro antes de validar pertenencia. */
    @Column(name = "codigo_qr", unique = true, length = 40)
    private String codigoQr;

    @Column(length = 1000)
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAlumno estado = EstadoAlumno.ACTIVO;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso = LocalDate.now();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Momento de la baja mas reciente (estado paso a INACTIVO/SUSPENDIDO). NO se limpia
     * al reactivarse (se conserva como historial), igual que Membresia#suspendidaMotivo.
     * Es el unico evento realmente timestamped para el reporte de "bajas por periodo".
     */
    @Column(name = "fecha_baja")
    private LocalDateTime fechaBaja;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "alumno_disciplinas",
            joinColumns = @JoinColumn(name = "alumno_id"),
            inverseJoinColumns = @JoinColumn(name = "disciplina_id")
    )
    private Set<Disciplina> disciplinas = new HashSet<>();
}
