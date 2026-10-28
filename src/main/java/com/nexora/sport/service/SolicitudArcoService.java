package com.nexora.sport.service;

import com.nexora.sport.dto.arco.ArcoDtos;
import com.nexora.sport.dto.arco.SolicitudArcoDto;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.AlumnoRepository;
import com.nexora.sport.repository.ApartadoPlanRepository;
import com.nexora.sport.repository.ApartadoRepository;
import com.nexora.sport.repository.SolicitudArcoRepository;
import com.nexora.sport.repository.UsuarioRepository;
import com.nexora.sport.repository.VentaRepository;
import com.nexora.sport.util.DiasHabiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/**
 * Ciclo de vida de las solicitudes de derechos ARCO, replicado de DemoPV: tres formas de
 * levantarla (formulario publico, cuenta propia, mostrador) y la atencion del SUPER_ADMIN
 * (verificar identidad, pedir informacion, extender plazo, responder y ejecutar). Ver
 * {@link SolicitudArco} y {@link EstadoSolicitudArco}.
 *
 * Diferencia con DemoPV: en un gimnasio la mayoria de los titulares son alumnos sin cuenta
 * de acceso, asi que el formulario publico acepta el correo de un usuario O de un alumno.
 */
@Service
public class SolicitudArcoService {

    private static final Logger log = LoggerFactory.getLogger(SolicitudArcoService.class);

    private static final int DIAS_RESPUESTA = 20;
    private static final int DIAS_RESPUESTA_INFO = 10;
    private static final int DIAS_EJECUCION = 15;
    private static final int DIAS_PURGA_SIN_CONFIRMAR = 3;

    private final SolicitudArcoRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final VentaRepository ventaRepository;
    private final ApartadoRepository apartadoRepository;
    private final ApartadoPlanRepository apartadoPlanRepository;
    private final MailService mailService;
    private final ArcoConstanciaPdfService constanciaPdfService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public SolicitudArcoService(SolicitudArcoRepository repository, UsuarioRepository usuarioRepository,
                                AlumnoRepository alumnoRepository, VentaRepository ventaRepository,
                                ApartadoRepository apartadoRepository, ApartadoPlanRepository apartadoPlanRepository,
                                MailService mailService, ArcoConstanciaPdfService constanciaPdfService,
                                PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.ventaRepository = ventaRepository;
        this.apartadoRepository = apartadoRepository;
        this.apartadoPlanRepository = apartadoPlanRepository;
        this.mailService = mailService;
        this.constanciaPdfService = constanciaPdfService;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------------------------------------------------------------- alta

    /**
     * Formulario publico (canal WEB). El correo debe ser de un usuario o de un alumno; queda
     * PENDIENTE_CORREO hasta que abre el link que se le manda, para que nadie levante una
     * solicitud a nombre de un correo que no controla.
     */
    @Transactional
    public void crearPublica(ArcoDtos.CrearRequest req) {
        String nombre = requerido(req.nombre(), "Tu nombre es obligatorio");
        String email = requerido(req.email(), "Tu correo es obligatorio");
        Usuario usuario = usuarioRepository.buscarPorEmailArco(email).orElse(null);
        if (usuario == null && alumnoRepository.buscarPorEmailArco(email).isEmpty()) {
            throw new IllegalArgumentException(
                    "Ese correo no está registrado en el sistema. Usa el correo que diste de alta en tu centro deportivo.");
        }
        validarRepresentante(req.representante(), req.representadoNombre());

        String token = generarToken();
        SolicitudArco s = nueva(nombre, email, req.telefono(), req.tipo(), req.detalle(),
                req.representante(), req.representadoNombre(), token);
        s.setAvisoPrivacidadAceptado(req.aceptaAvisoPrivacidad());
        s.setCanal(CanalSolicitudArco.WEB);
        s.setEstado(EstadoSolicitudArco.PENDIENTE_CORREO);
        s.setSolicitanteUsuario(usuario);
        s = repository.save(s);
        log.info("Solicitud ARCO {} ({}) creada via WEB, pendiente de confirmar correo", s.getId(), s.getTipo());

        mailService.send(email, "Confirma tu solicitud de derechos ARCO",
                "Hola " + nombre + ",\n\n" +
                "Recibimos tu solicitud de " + s.getTipo().etiqueta() + " sobre tus datos personales. Para que empiece " +
                "a correr el plazo legal de respuesta, confirma que este es tu correo entrando al siguiente link:\n\n" +
                urlSeguimiento(token) + "\n\n" +
                "Si tú no la pediste, puedes ignorar este correo.");
    }

    /**
     * Desde la cuenta en sesion (canal CUENTA): nombre/correo salen de la sesion. Los tipos
     * de identidad fuerte piden volver a escribir la contrasena. Queda PENDIENTE de inmediato.
     */
    @Transactional
    public void crearPropia(ArcoDtos.CrearRequest req, Usuario actor) {
        validarRepresentante(req.representante(), req.representadoNombre());
        boolean fuerte = req.tipo().requiereIdentidadFuerte();
        if (fuerte && (req.password() == null || req.password().isBlank()
                || !passwordEncoder.matches(req.password(), actor.getPasswordHash()))) {
            throw new IllegalArgumentException("Tu contraseña no es correcta");
        }

        LocalDateTime ahora = LocalDateTime.now();
        SolicitudArco s = nueva(actor.getNombre(), actor.getEmail(), req.telefono(), req.tipo(), req.detalle(),
                req.representante(), req.representadoNombre(), generarToken());
        s.setAvisoPrivacidadAceptado(req.aceptaAvisoPrivacidad());
        s.setCanal(CanalSolicitudArco.CUENTA);
        s.setEstado(EstadoSolicitudArco.PENDIENTE);
        s.setRecibidaEn(ahora);
        s.setFechaLimite(DiasHabiles.sumar(ahora.toLocalDate(), DIAS_RESPUESTA));
        s.setSolicitanteUsuario(actor);
        if (fuerte) {
            s.setIdentidadVerificadaEn(ahora);
            s.setIdentidadMetodo(MetodoIdentidadArco.CONTRASENA);
            s.setIdentidadNotas("Confirmó su contraseña al levantar la solicitud desde su cuenta.");
        }
        s = repository.save(s);
        log.info("Solicitud ARCO {} ({}) creada via CUENTA por {}", s.getId(), s.getTipo(), actor.getEmail());
        avisarNuevaASuperAdmins(s);
    }

    /** Registro en mostrador (SUPER_ADMIN) para alguien verificado en persona, con o sin cuenta. */
    @Transactional
    public SolicitudArcoDto crearEnMostrador(ArcoDtos.MostradorRequest req, Usuario actor) {
        validarRepresentante(req.representante(), req.representadoNombre());
        String email = vacioANull(req.email());
        Usuario usuario = email != null ? usuarioRepository.buscarPorEmailArco(email).orElse(null) : null;

        LocalDateTime ahora = LocalDateTime.now();
        SolicitudArco s = nueva(req.nombre().trim(), email, req.telefono(), req.tipo(), req.detalle(),
                req.representante(), req.representadoNombre(), generarToken());
        s.setAvisoPrivacidadAceptado(true);
        s.setCanal(CanalSolicitudArco.MOSTRADOR);
        s.setEstado(EstadoSolicitudArco.PENDIENTE);
        s.setRecibidaEn(ahora);
        s.setFechaLimite(DiasHabiles.sumar(ahora.toLocalDate(), DIAS_RESPUESTA));
        s.setSolicitanteUsuario(usuario);
        s.setIdentidadVerificadaEn(ahora);
        s.setIdentidadMetodo(MetodoIdentidadArco.EN_PERSONA);
        s.setIdentidadVerificadaPor(actor);
        s.setIdentidadNotas(req.identidadNotas().trim());
        s.setRegistradaPor(actor);
        s = repository.save(s);
        log.info("Solicitud ARCO {} ({}) registrada en mostrador por {}", s.getId(), s.getTipo(), actor.getEmail());
        avisarNuevaASuperAdmins(s);
        return toDto(s);
    }

    // ---------------------------------------------------------------- seguimiento por link

    /** Abrir el link de seguimiento ES la confirmacion del correo si seguia PENDIENTE_CORREO. */
    @Transactional
    public SolicitudArcoDto seguimiento(String token) {
        SolicitudArco s = porToken(token);
        if (s.getEstado() == EstadoSolicitudArco.PENDIENTE_CORREO) {
            LocalDateTime ahora = LocalDateTime.now();
            s.setCorreoVerificadoEn(ahora);
            s.setRecibidaEn(ahora);
            s.setFechaLimite(DiasHabiles.sumar(ahora.toLocalDate(), DIAS_RESPUESTA));
            s.setEstado(EstadoSolicitudArco.PENDIENTE);
            s = repository.save(s);
            log.info("Solicitud ARCO {} confirmo su correo, arranca el plazo legal", s.getId());
            avisarNuevaASuperAdmins(s);
        }
        return toDto(s);
    }

    @Transactional
    public SolicitudArcoDto responderInfoPorToken(String token, ArcoDtos.ResponderInfoRequest req) {
        return responderInfo(porToken(token), req);
    }

    @Transactional
    public SolicitudArcoDto responderInfoPropia(Long id, ArcoDtos.ResponderInfoRequest req, Usuario actor) {
        SolicitudArco s = buscar(id);
        if (s.getSolicitanteUsuario() == null || !s.getSolicitanteUsuario().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Esta solicitud no es tuya");
        }
        return responderInfo(s, req);
    }

    private SolicitudArcoDto responderInfo(SolicitudArco s, ArcoDtos.ResponderInfoRequest req) {
        if (s.getEstado() != EstadoSolicitudArco.INFO_SOLICITADA) {
            throw new IllegalStateException("Esta solicitud no está esperando información");
        }
        s.setInfoRespuesta(req.respuesta().trim());
        s.setEstado(EstadoSolicitudArco.PENDIENTE);
        s = repository.save(s);
        log.info("Solicitud ARCO {} recibio la informacion pedida", s.getId());
        for (Usuario admin : usuarioRepository.superAdminsActivos()) {
            mailService.send(admin.getEmail(), "Respondieron información de la solicitud ARCO #" + s.getId(),
                    s.getSolicitanteNombre() + " ya respondió lo que le pediste:\n\n" + s.getInfoRespuesta()
                            + "\n\nAtiéndela aquí:\n" + urlAdmin());
        }
        return toDto(s);
    }

    // ---------------------------------------------------------------- consulta

    @Transactional(readOnly = true)
    public List<SolicitudArcoDto> listar(EstadoSolicitudArco estado) {
        return repository.buscar(estado).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long contarAbiertas() {
        return repository.contarAbiertas();
    }

    @Transactional(readOnly = true)
    public List<SolicitudArcoDto> listarPropias(Usuario actor) {
        return repository.findBySolicitanteUsuarioIdOrderByCreatedAtDesc(actor.getId()).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public SolicitudArcoDto obtener(Long id) {
        return toDto(buscar(id));
    }

    @Transactional(readOnly = true)
    public byte[] constanciaPdf(Long id) {
        return constanciaPdfService.generar(buscar(id));
    }

    // ---------------------------------------------------------------- atencion (SUPER_ADMIN)

    @Transactional
    public SolicitudArcoDto verificarIdentidad(Long id, ArcoDtos.VerificarIdentidadRequest req, Usuario actor) {
        SolicitudArco s = buscar(id);
        if (s.getEstado().cerrada()) throw new IllegalStateException("Esta solicitud ya está cerrada");
        s.setIdentidadVerificadaEn(LocalDateTime.now());
        s.setIdentidadMetodo(req.metodo());
        s.setIdentidadNotas(req.notas().trim());
        s.setIdentidadVerificadaPor(actor);
        return toDto(repository.save(s));
    }

    @Transactional
    public SolicitudArcoDto pedirInfo(Long id, ArcoDtos.PedirInfoRequest req) {
        SolicitudArco s = buscar(id);
        if (s.getEstado() != EstadoSolicitudArco.PENDIENTE) {
            throw new IllegalStateException("Solo se puede pedir información a una solicitud pendiente");
        }
        s.setInfoSolicitada(req.infoSolicitada().trim());
        s.setInfoFechaLimite(DiasHabiles.sumar(LocalDate.now(), DIAS_RESPUESTA_INFO));
        s.setEstado(EstadoSolicitudArco.INFO_SOLICITADA);
        s = repository.save(s);
        log.info("Solicitud ARCO {} pasa a INFO_SOLICITADA, responde antes del {}", s.getId(), s.getInfoFechaLimite());

        String cuerpo = "Hola " + s.getSolicitanteNombre() + ",\n\n" +
                "Para resolver tu solicitud de " + s.getTipo().etiqueta() + " necesitamos lo siguiente:\n\n" +
                s.getInfoSolicitada() + "\n\n" +
                "Por favor respóndenos antes del " + s.getInfoFechaLimite() + "; si no tenemos noticias tuyas para " +
                "entonces, la solicitud se cerrará por falta de información.\n\n" +
                (s.getTokenCifrado() != null
                        ? "Responde aquí:\n" + urlSeguimiento(s.getTokenCifrado())
                        : "Responde entrando a tu cuenta, en \"Mis solicitudes ARCO\".");
        mailService.send(s.getSolicitanteEmail(), "Necesitamos más información: tu solicitud ARCO #" + s.getId(), cuerpo);
        return toDto(s);
    }

    @Transactional
    public SolicitudArcoDto extender(Long id, ArcoDtos.ExtenderRequest req) {
        SolicitudArco s = buscar(id);
        if (s.isExtendida()) throw new IllegalStateException("Esta solicitud ya se extendió una vez; no se puede extender de nuevo");
        if (s.getFechaLimite() == null) throw new IllegalStateException("Esta solicitud todavía no tiene un plazo que extender");
        if (s.getEstado().cerrada()) throw new IllegalStateException("Esta solicitud ya está cerrada");
        s.setFechaLimite(DiasHabiles.sumar(s.getFechaLimite(), DIAS_RESPUESTA));
        s.setExtendida(true);
        s.setMotivoExtension(req.motivo().trim());
        return toDto(repository.save(s));
    }

    /**
     * Respuesta legal de una PENDIENTE. IMPROCEDENTE la cierra (RECHAZADA); cualquier otro
     * resultado la deja APROBADA para ejecutarse. Manda la constancia al titular.
     */
    @Transactional
    public SolicitudArcoDto responder(Long id, ArcoDtos.ResponderRequest req, Usuario actor) {
        SolicitudArco s = buscar(id);
        if (s.getEstado() != EstadoSolicitudArco.PENDIENTE) {
            throw new IllegalStateException("Solo se puede responder una solicitud pendiente "
                    + "(si está esperando información, el titular debe contestarla primero)");
        }
        if (s.getTipo().requiereIdentidadFuerte() && s.getIdentidadVerificadaEn() == null) {
            throw new IllegalStateException("Verifica la identidad de quien pide antes de responder este tipo de solicitud");
        }
        if (req.resultado() != ResultadoArco.PROCEDENTE && (req.fundamento() == null || req.fundamento().isBlank())) {
            throw new IllegalArgumentException("Explica el motivo/fundamento cuando el resultado no es totalmente procedente");
        }

        s.setResultado(req.resultado());
        s.setFundamento(vacioANull(req.fundamento()));
        s.setRespuesta(req.respuesta().trim());
        s.setRespondidaEn(LocalDateTime.now());
        s.setRespondidaPor(actor);
        if (req.resultado() == ResultadoArco.IMPROCEDENTE) {
            s.setEstado(EstadoSolicitudArco.RECHAZADA);
        } else {
            s.setEstado(EstadoSolicitudArco.APROBADA);
            s.setEjecucionFechaLimite(DiasHabiles.sumar(LocalDate.now(), DIAS_EJECUCION));
        }
        s = repository.save(s);
        log.info("Solicitud ARCO {} respondida: {} -> {}", s.getId(), s.getResultado(), s.getEstado());

        String cuerpo = "Hola " + s.getSolicitanteNombre() + ",\n\n" +
                "Ya respondimos tu solicitud de " + s.getTipo().etiqueta() + ":\n\n" + s.getRespuesta() + "\n\n" +
                (s.getEstado() == EstadoSolicitudArco.APROBADA ? "La vamos a ejecutar en los próximos días.\n\n" : "") +
                "Adjuntamos la constancia de esta respuesta.";
        mailService.enviarConAdjunto(null, s.getSolicitanteEmail(), "Tu solicitud ARCO #" + s.getId() + " fue respondida",
                cuerpo, "constancia-arco-" + s.getId() + ".pdf", constanciaPdfService.generar(s));
        return toDto(s);
    }

    /**
     * Ejecuta una APROBADA. Acceso: agrega al resumen los datos encontrados en el sistema.
     * Cancelacion: desactiva la cuenta de acceso del titular si tiene (ventas, apartados y
     * fichas de alumno se conservan por obligacion fiscal/contable). Lo demas es manual y lo
     * describe el SUPER_ADMIN.
     */
    @Transactional
    public SolicitudArcoDto ejecutar(Long id, ArcoDtos.EjecutarRequest req, Usuario actor) {
        SolicitudArco s = buscar(id);
        if (s.getEstado() != EstadoSolicitudArco.APROBADA) {
            throw new IllegalStateException("Solo se puede ejecutar una solicitud ya aprobada");
        }

        StringBuilder resumen = new StringBuilder(req.resumen().trim());
        Usuario titular = s.getSolicitanteUsuario();
        if (s.getTipo() == TipoDerechoArco.CANCELACION && titular != null && titular.isActivo()) {
            if (titular.getRol().getNivel() == NivelJerarquia.SUPER_ADMIN) {
                resumen.append("\n\n[Automático] La cuenta del titular es de SUPER_ADMIN: no se desactivó automáticamente.");
            } else {
                titular.setActivo(false);
                usuarioRepository.save(titular);
                resumen.append("\n\n[Automático] Se desactivó la cuenta de acceso del titular. Sus ventas, apartados y "
                        + "membresías históricas se conservan por obligación fiscal/contable.");
            }
        } else if (s.getTipo() == TipoDerechoArco.ACCESO) {
            resumen.append("\n\n[Automático] ").append(resumenDatos(s));
        }

        s.setEjecutadaEn(LocalDateTime.now());
        s.setEjecutadaPor(actor);
        s.setResumenEjecucion(resumen.toString());
        s.setEstado(EstadoSolicitudArco.COMPLETADA);
        s = repository.save(s);
        log.info("Solicitud ARCO {} ejecutada y completada", s.getId());

        mailService.enviarConAdjunto(null, s.getSolicitanteEmail(), "Tu solicitud ARCO #" + s.getId() + " fue completada",
                "Hola " + s.getSolicitanteNombre() + ",\n\n" +
                        "Tu solicitud de " + s.getTipo().etiqueta() + " ya fue ejecutada y quedó cerrada.\n\n" +
                        "Adjuntamos la constancia final.",
                "constancia-arco-" + s.getId() + ".pdf", constanciaPdfService.generar(s));
        return toDto(s);
    }

    /** Resumen para Acceso: cuenta, fichas de alumno, ventas y apartados donde aparece por correo o telefono. */
    private String resumenDatos(SolicitudArco s) {
        StringBuilder sb = new StringBuilder("Datos encontrados en el sistema:");
        Usuario u = s.getSolicitanteUsuario();
        if (u != null) {
            sb.append("\n- Cuenta de acceso: ").append(u.getNombre()).append(" <").append(u.getEmail()).append(">")
                    .append(", rol ").append(u.getRol().getNombre());
            if (u.getCentro() != null) sb.append(", centro ").append(u.getCentro().getNombre());
        }
        String email = s.getSolicitanteEmail() != null ? s.getSolicitanteEmail().trim() : "";
        String telefono = s.getSolicitanteTelefono() != null ? s.getSolicitanteTelefono().trim() : "";
        if (email.isEmpty() && telefono.isEmpty()) return sb.toString();

        List<Alumno> alumnos = alumnoRepository.buscarTitularArco(email, telefono);
        sb.append("\n- ").append(alumnos.size()).append(" ficha(s) de alumno");
        for (Alumno a : alumnos) {
            sb.append("\n  · ").append(a.getNombre()).append(" en ").append(a.getCentro().getNombre())
                    .append(" (alta ").append(a.getFechaIngreso()).append(")")
                    .append(a.getTelefono() != null ? ", tel. " + a.getTelefono() : "")
                    .append(a.getEmail() != null ? ", " + a.getEmail() : "")
                    .append(a.getFechaNacimiento() != null ? ", nacimiento " + a.getFechaNacimiento() : "")
                    .append(a.getContactoEmergenciaNombre() != null ? ", contacto de emergencia " + a.getContactoEmergenciaNombre() : "");
        }
        List<Venta> ventas = email.isEmpty() ? List.of() : ventaRepository.buscarPorClienteEmailArco(email);
        sb.append("\n- ").append(ventas.size()).append(" venta(s) con su correo");
        for (Venta v : ventas) {
            sb.append("\n  · Venta #").append(v.getId()).append(" en ").append(v.getCentro().getNombre())
                    .append(" (").append(v.getCreatedAt().toLocalDate()).append(")");
        }
        List<Apartado> apartados = apartadoRepository.buscarTitularArco(email, telefono);
        sb.append("\n- ").append(apartados.size()).append(" apartado(s) de productos");
        for (Apartado a : apartados) {
            sb.append("\n  · Apartado #").append(a.getId()).append(" en ").append(a.getCentro().getNombre())
                    .append(" (").append(a.getSolicitadoEn().toLocalDate()).append(")");
        }
        List<ApartadoPlan> apartadosPlan = apartadoPlanRepository.buscarTitularArco(email, telefono);
        sb.append("\n- ").append(apartadosPlan.size()).append(" apartado(s) de planes");
        for (ApartadoPlan a : apartadosPlan) {
            sb.append("\n  · Apartado de plan #").append(a.getId()).append(" en ").append(a.getCentro().getNombre())
                    .append(" (").append(a.getSolicitadoEn().toLocalDate()).append(")");
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- mantenimiento

    /**
     * Borra las WEB que nunca confirmaron su correo (mas de 3 dias) y cierra como
     * NO_PRESENTADA las que esperaban informacion con el plazo vencido.
     */
    @Transactional
    public int mantenimiento() {
        int purgadas = repository.borrarPorEstadoAntesDe(EstadoSolicitudArco.PENDIENTE_CORREO,
                LocalDateTime.now().minusDays(DIAS_PURGA_SIN_CONFIRMAR));
        List<SolicitudArco> vencidas = repository.findByEstadoAndInfoFechaLimiteBefore(
                EstadoSolicitudArco.INFO_SOLICITADA, LocalDate.now());
        for (SolicitudArco s : vencidas) {
            s.setEstado(EstadoSolicitudArco.NO_PRESENTADA);
            repository.save(s);
            log.info("Solicitud ARCO {} cerrada como NO_PRESENTADA: no respondio la informacion a tiempo", s.getId());
        }
        return purgadas + vencidas.size();
    }

    // ---------------------------------------------------------------- helpers

    private SolicitudArco nueva(String nombre, String email, String telefono, TipoDerechoArco tipo, String detalle,
                                Boolean representante, String representadoNombre, String token) {
        SolicitudArco s = new SolicitudArco();
        s.setSolicitanteNombre(nombre);
        s.setSolicitanteEmail(email);
        s.setSolicitanteTelefono(vacioANull(telefono));
        s.setTipo(tipo);
        s.setDetalle(vacioANull(detalle));
        s.setRepresentante(Boolean.TRUE.equals(representante));
        s.setRepresentadoNombre(Boolean.TRUE.equals(representante) ? vacioANull(representadoNombre) : null);
        s.setTokenHash(hash(token));
        s.setTokenCifrado(token);
        return s;
    }

    private SolicitudArco buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada: " + id));
    }

    private SolicitudArco porToken(String token) {
        return repository.findByTokenHash(hash(token))
                .orElseThrow(() -> new IllegalArgumentException("Link inválido o vencido"));
    }

    private void validarRepresentante(Boolean representante, String representadoNombre) {
        if (Boolean.TRUE.equals(representante) && (representadoNombre == null || representadoNombre.isBlank())) {
            throw new IllegalArgumentException("Escribe el nombre de la persona a la que representas");
        }
    }

    private void avisarNuevaASuperAdmins(SolicitudArco s) {
        String cuerpo = "Llegó una nueva solicitud de derechos ARCO:\n\n" +
                "Tipo: " + s.getTipo().etiqueta() + "\n" +
                "Nombre: " + s.getSolicitanteNombre() + "\n" +
                "Correo: " + (s.getSolicitanteEmail() != null ? s.getSolicitanteEmail() : "(sin correo)") + "\n" +
                (s.getSolicitanteTelefono() != null ? "Teléfono: " + s.getSolicitanteTelefono() + "\n" : "") +
                "Plazo de respuesta: " + s.getFechaLimite() + "\n\n" +
                "Detalle:\n" + (s.getDetalle() != null ? s.getDetalle() : "(sin detalle adicional)") +
                "\n\nAtiéndela aquí:\n" + urlAdmin();
        for (Usuario admin : usuarioRepository.superAdminsActivos()) {
            mailService.send(admin.getEmail(), "Nueva solicitud ARCO (" + s.getTipo().etiqueta() + ")", cuerpo);
        }
    }

    private String urlAdmin() {
        return frontendUrl + "/solicitudes-arco";
    }

    private String urlSeguimiento(String token) {
        return frontendUrl + "/arco/seguimiento/" + token;
    }

    private String generarToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String requerido(String s, String mensaje) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException(mensaje);
        return s.trim();
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private SolicitudArcoDto toDto(SolicitudArco s) {
        return new SolicitudArcoDto(
                s.getId(), s.getSolicitanteNombre(), s.getSolicitanteEmail(), s.getSolicitanteTelefono(),
                s.getTipo().name(), s.getEstado().name(), s.getCanal().name(), s.getDetalle(),
                s.isRepresentante(), s.getRepresentadoNombre(), s.getCreatedAt(), s.getRecibidaEn(), s.getFechaLimite(),
                s.getIdentidadVerificadaEn(), s.getIdentidadMetodo() != null ? s.getIdentidadMetodo().name() : null,
                s.getIdentidadNotas(), s.getInfoSolicitada(), s.getInfoFechaLimite(), s.getInfoRespuesta(),
                s.isExtendida(), s.getMotivoExtension(),
                s.getResultado() != null ? s.getResultado().name() : null, s.getFundamento(), s.getRespuesta(),
                s.getRespondidaEn(), s.getRespondidaPor() != null ? s.getRespondidaPor().getNombre() : null,
                s.getEjecucionFechaLimite(), s.getEjecutadaEn(),
                s.getEjecutadaPor() != null ? s.getEjecutadaPor().getNombre() : null,
                s.getResumenEjecucion(),
                s.getRegistradaPor() != null ? s.getRegistradaPor().getNombre() : null);
    }
}
