package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Un centro deportivo / gimnasio: la frontera de tenant de todo el sistema. */
@Entity
@Table(name = "centros")
@Getter
@Setter
public class Centro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "slug_publico", unique = true, length = 80)
    private String slugPublico;

    @Column(name = "catalogo_publico_activo", nullable = false)
    private boolean catalogoPublicoActivo = false;

    @Column(name = "color_primario", length = 9)
    private String colorPrimario = "#1c6690";

    @Column(name = "logo_url", length = 300)
    private String logoUrl;

    @Column(length = 30)
    private String telefono;

    @Column(name = "email_contacto", length = 150)
    private String emailContacto;

    @Column(length = 250)
    private String direccion;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "apartados_activo", nullable = false)
    private boolean apartadosActivo = false;

    @Column(name = "horas_apartado_default", nullable = false)
    private int horasApartadoDefault = 24;

    @Column(name = "monto_maximo_descuento_apartado", precision = 10, scale = 2)
    private BigDecimal montoMaximoDescuentoApartado;

    @Column(name = "porcentaje_maximo_descuento_apartado", precision = 5, scale = 2)
    private BigDecimal porcentajeMaximoDescuentoApartado;

    /**
     * Politica global del centro: si un alumno con saldo pendiente puede seguir
     * accediendo (check-in). Vive aqui (no en Plan/Membresia) porque es una decision
     * operativa del gimnasio, igual que apartadosActivo. El futuro modulo de
     * Asistencia es quien debera leerla; por ahora solo se deja disponible.
     */
    @Column(name = "permitir_acceso_con_adeudo", nullable = false)
    private boolean permitirAccesoConAdeudo = true;

    /** Dias sin asistir para que una membresia activa aparezca en el reporte de riesgo de abandono. */
    @Column(name = "dias_inactividad_riesgo", nullable = false)
    private int diasInactividadRiesgo = 14;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
