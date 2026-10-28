package com.nexora.sport.model;

import com.nexora.sport.security.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Solicitud de derechos ARCO (ver {@link TipoDerechoArco}) sobre los datos personales que
 * la plataforma guarda de alguien: personal con cuenta, alumnos o clientes de la tienda.
 * Replica el modulo de DemoPV.
 *
 * A proposito NO esta ligada a un Centro: es obligacion de la plataforma completa, la
 * atiende solo el SUPER_ADMIN (igual que MailConfig). Por el formulario publico solo puede
 * levantarla alguien cuyo correo ya existe en el sistema (usuario o alumno) y debe
 * confirmarlo por link antes de que corra el plazo; en MOSTRADOR el SUPER_ADMIN registra a
 * alguien verificado en persona, con o sin correo.
 *
 * Plazos: 20 dias habiles para responder desde la recepcion formal (una sola extension de
 * otros 20), 10 para que el titular conteste informacion pedida y 15 para ejecutar una
 * aprobada (ver DiasHabiles). Solo Acceso (resumen de datos) y Cancelacion (desactiva la
 * cuenta) tienen accion automatica al ejecutarse; lo demas lo describe el SUPER_ADMIN.
 */
@Entity
@Table(name = "solicitudes_arco")
@Getter
@Setter
public class SolicitudArco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitante_nombre", nullable = false, length = 150)
    private String solicitanteNombre;

    /** Null solo en MOSTRADOR, para alguien sin correo. */
    @Column(name = "solicitante_email", length = 150)
    private String solicitanteEmail;

    @Column(name = "solicitante_telefono", length = 30)
    private String solicitanteTelefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoDerechoArco tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitudArco estado = EstadoSolicitudArco.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private CanalSolicitudArco canal = CanalSolicitudArco.WEB;

    @Column(columnDefinition = "TEXT")
    private String detalle;

    @Column(nullable = false)
    private boolean representante = false;

    @Column(name = "representado_nombre", length = 150)
    private String representadoNombre;

    @Column(name = "aviso_privacidad_aceptado", nullable = false)
    private boolean avisoPrivacidadAceptado = false;

    // -- Seguimiento por link ------------------------------------------------------------
    /** SHA-256 del token del link de seguimiento: el token en claro nunca se busca por columna. */
    @Column(name = "token_hash", length = 64, unique = true)
    private String tokenHash;

    /** Token en claro cifrado (AES-GCM), para volver a mandar el link cuando se pide informacion. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "token_cifrado", columnDefinition = "TEXT")
    private String tokenCifrado;

    // -- Recepcion formal (arranca el plazo) ---------------------------------------------
    @Column(name = "recibida_en")
    private LocalDateTime recibidaEn;

    @Column(name = "correo_verificado_en")
    private LocalDateTime correoVerificadoEn;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    // -- Identidad -----------------------------------------------------------------------
    @Column(name = "identidad_verificada_en")
    private LocalDateTime identidadVerificadaEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "identidad_metodo", length = 15)
    private MetodoIdentidadArco identidadMetodo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "identidad_verificada_por")
    private Usuario identidadVerificadaPor;

    @Column(name = "identidad_notas", columnDefinition = "TEXT")
    private String identidadNotas;

    // -- Informacion pedida al titular ---------------------------------------------------
    @Column(name = "info_solicitada", columnDefinition = "TEXT")
    private String infoSolicitada;

    @Column(name = "info_fecha_limite")
    private LocalDate infoFechaLimite;

    @Column(name = "info_respuesta", columnDefinition = "TEXT")
    private String infoRespuesta;

    // -- Extension del plazo (una vez) ---------------------------------------------------
    @Column(nullable = false)
    private boolean extendida = false;

    @Column(name = "motivo_extension", columnDefinition = "TEXT")
    private String motivoExtension;

    // -- Respuesta legal -----------------------------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private ResultadoArco resultado;

    /** Obligatorio si el resultado es PARCIAL o IMPROCEDENTE. */
    @Column(columnDefinition = "TEXT")
    private String fundamento;

    @Column(name = "respuesta", columnDefinition = "TEXT")
    private String respuesta;

    @Column(name = "respondida_en")
    private LocalDateTime respondidaEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respondida_por")
    private Usuario respondidaPor;

    // -- Ejecucion -----------------------------------------------------------------------
    @Column(name = "ejecucion_fecha_limite")
    private LocalDate ejecucionFechaLimite;

    @Column(name = "ejecutada_en")
    private LocalDateTime ejecutadaEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ejecutada_por")
    private Usuario ejecutadaPor;

    @Column(name = "resumen_ejecucion", columnDefinition = "TEXT")
    private String resumenEjecucion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Cuenta encontrada por el correo al crearse (null si es alumno/cliente sin cuenta). Alimenta "Mis solicitudes". */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_usuario_id")
    private Usuario solicitanteUsuario;

    /** Quien la registro en mostrador. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrada_por")
    private Usuario registradaPor;
}
