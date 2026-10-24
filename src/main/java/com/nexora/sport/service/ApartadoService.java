package com.nexora.sport.service;

import com.nexora.sport.dto.*;
import com.nexora.sport.dto.publico.PublicApartadoConsultaDto;
import com.nexora.sport.dto.publico.PublicApartadoResponseDto;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.*;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reservas ("apartados") de la tienda publica: solicitar (sin login) -> confirmar (descuenta
 * stock) -> completar (genera Venta) o cancelar -> vencimiento automatico si nadie lo recoge.
 */
@Service
public class ApartadoService {

    private final ApartadoRepository apartadoRepository;
    private final ArticuloInventarioRepository articuloRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final CentroRepository centroRepository;
    private final UsuarioRepository usuarioRepository;
    private final VentaService ventaService;
    private final MailService mailService;
    private final TenantScope tenantScope;
    private final NotificacionService notificacionService;
    private final BloqueoFilas bloqueoFilas;

    public ApartadoService(ApartadoRepository apartadoRepository, ArticuloInventarioRepository articuloRepository,
                            MovimientoInventarioRepository movimientoInventarioRepository, CentroRepository centroRepository,
                            UsuarioRepository usuarioRepository, VentaService ventaService, MailService mailService,
                            TenantScope tenantScope, NotificacionService notificacionService, BloqueoFilas bloqueoFilas) {
        this.bloqueoFilas = bloqueoFilas;
        this.notificacionService = notificacionService;
        this.apartadoRepository = apartadoRepository;
        this.articuloRepository = articuloRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.centroRepository = centroRepository;
        this.usuarioRepository = usuarioRepository;
        this.ventaService = ventaService;
        this.mailService = mailService;
        this.tenantScope = tenantScope;
    }

    // ---------------------------------------------------------------- publico
    @Transactional
    public PublicApartadoResponseDto crearPublico(String slug, ApartadoRequest request) {
        Centro centro = centroRepository.findBySlugPublicoAndActivoTrue(slug)
                .filter(Centro::isApartadosActivo)
                .orElseThrow(() -> new ResourceNotFoundException("Apartados no disponibles"));

        Apartado apartado = new Apartado();
        apartado.setCentro(centro);
        apartado.setClienteNombre(request.clienteNombre());
        apartado.setClienteTelefono(request.clienteTelefono());
        apartado.setClienteEmail(request.clienteEmail());
        apartado.setNotas(request.notas());
        apartado.setEstado(EstadoApartado.PENDIENTE);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal descuentoTotal = BigDecimal.ZERO;
        for (ApartadoRequest.ItemRequest ir : request.items()) {
            ArticuloInventario articulo = articuloRepository.findById(ir.articuloId())
                    .orElseThrow(() -> new ResourceNotFoundException("Articulo no encontrado"));
            if (!articulo.getCentro().getId().equals(centro.getId())) {
                throw new ResourceNotFoundException("Articulo no encontrado");
            }
            if (!articulo.isActivo() || !articulo.isReservable()) {
                throw new IllegalArgumentException("\"" + articulo.getNombre() + "\" no esta disponible para apartar");
            }
            if (articulo.getStock() < ir.cantidad()) {
                throw new IllegalStateException("No hay suficiente stock de \"" + articulo.getNombre() + "\"");
            }
            BigDecimal precio = articulo.getPrecioVenta() != null ? articulo.getPrecioVenta() : BigDecimal.ZERO;
            BigDecimal bruto = precio.multiply(BigDecimal.valueOf(ir.cantidad()));
            BigDecimal descuentoPct = articulo.getDescuentoApartadoPorcentaje();
            BigDecimal descuentoLinea = descuentoPct != null
                    ? bruto.multiply(descuentoPct).divide(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            ApartadoItem item = new ApartadoItem();
            item.setApartado(apartado);
            item.setArticulo(articulo);
            item.setArticuloNombre(articulo.getNombre());
            item.setPrecioUnitario(precio);
            item.setCantidad(ir.cantidad());
            item.setDescuento(descuentoLinea);
            item.setSubtotal(bruto.subtract(descuentoLinea));
            apartado.getItems().add(item);

            subtotal = subtotal.add(bruto);
            descuentoTotal = descuentoTotal.add(descuentoLinea);
        }
        apartado.setSubtotal(subtotal);
        apartado.setDescuento(descuentoTotal);
        apartado.setTotal(subtotal.subtract(descuentoTotal));
        apartado = apartadoRepository.save(apartado);

        String detalle = detalleItems(apartado);
        for (Usuario staff : staffParaNotificar(centro.getId(), null)) {
            mailService.send(staff.getEmail(), "Nueva solicitud de apartado #" + apartado.getId(),
                    "Cliente: " + apartado.getClienteNombre() + " (" + apartado.getClienteTelefono() + ")\n\n"
                            + detalle + "\n\nRevisa y confirma la solicitud desde el panel para reservar el stock.");
        }
        notificacionService.notificarApartadoNuevo(apartado, sucursalUnica(apartado));

        return new PublicApartadoResponseDto(apartado.getId(), apartado.getEstado().name(), apartado.getTotal(),
                centro.getHorasApartadoDefault(), apartado.getSolicitadoEn());
    }

    // ---------------------------------------------------------------- admin
    @Transactional(readOnly = true)
    public PageResponse<ApartadoDto> listar(Usuario actor, EstadoApartado estado, java.time.LocalDate desde,
                                             java.time.LocalDate hasta, String q, Pageable pageable) {
        Long centroId = tenantScope.scopeId(actor);
        LocalDateTime desdeFecha = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaFecha = hasta != null ? hasta.plusDays(1).atStartOfDay() : null;
        String texto = (q == null || q.isBlank()) ? null : q;
        return PageResponse.of(apartadoRepository.buscar(centroId, estado, desdeFecha, hastaFecha, texto, pageable), a -> toDto(a, false));
    }

    public Apartado buscar(Long id) {
        return apartadoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Apartado no encontrado"));
    }

    // ------------------------------------------------------------- concurrencia
    // Homologado con DemoPV: toda operacion que cambia el estado de un apartado o el stock de
    // sus articulos toma un bloqueo de fila (SELECT ... FOR UPDATE) ANTES de leer. Asi, si dos
    // personas confirman a la vez dos solicitudes por la ultima pieza (o la misma solicitud dos
    // veces), la segunda espera a que la primera termine, ve el stock/estado ya actualizado y
    // falla con un mensaje claro en vez de que a las dos se les descuente el mismo stock.

    /** El apartado bloqueado y releido (y del centro del actor, si hay actor). */
    private Apartado bloquear(Long id, Usuario actor) {
        Apartado apartado = bloqueoFilas.bloquear(Apartado.class, id, "Apartado no encontrado");
        if (actor != null && !apartado.getCentro().getId().equals(tenantScope.scopeId(actor))) {
            throw new ResourceNotFoundException("Apartado no encontrado");
        }
        return apartado;
    }

    /** Bloquea los articulos de los items en orden de id (ver BloqueoFilas#bloquearEnOrden). */
    private Map<Long, ArticuloInventario> bloquearArticulos(List<ApartadoItem> items) {
        return bloqueoFilas.bloquearEnOrden(ArticuloInventario.class,
                items.stream().map(ApartadoItem::getArticulo).filter(java.util.Objects::nonNull)
                        .map(ArticuloInventario::getId).toList(),
                "Un articulo del apartado ya no existe");
    }

    @Transactional(readOnly = true)
    public ApartadoDto obtener(Long id) {
        return toDto(buscar(id), true);
    }

    @Transactional
    public ApartadoDto confirmar(Long id, Usuario actor, ApartadoConfirmRequest request) {
        // Bloqueo del apartado: dos personas confirmando la MISMA solicitud a la vez -> la
        // segunda espera y ya la ve ACTIVO ("ya fue procesado"), no descuenta el stock dos veces.
        Apartado apartado = bloquear(id, actor);
        if (apartado.getEstado() != EstadoApartado.PENDIENTE) {
            throw new IllegalStateException("Este apartado ya fue confirmado, completado o cancelado por alguien mas");
        }
        Map<Long, BigDecimal> descuentosPorArticulo = new HashMap<>();
        if (request.descuentos() != null) {
            request.descuentos().forEach(d -> descuentosPorArticulo.put(d.articuloId(), d.descuento()));
        }

        // Bloqueo de los articulos ANTES de leer su stock: dos solicitudes distintas por la ultima
        // pieza -> la segunda confirmacion espera, lee el stock ya descontado y falla. Se valida lo
        // que pide el apartado en TOTAL por articulo (por si un articulo viniera en dos lineas).
        Map<Long, ArticuloInventario> articulos = bloquearArticulos(apartado.getItems());
        Map<Long, Integer> pedidoPorArticulo = new HashMap<>();
        for (ApartadoItem item : apartado.getItems()) {
            if (item.getArticulo() == null) {
                throw new IllegalStateException("\"" + item.getArticuloNombre() + "\" ya no existe en el inventario");
            }
            pedidoPorArticulo.merge(item.getArticulo().getId(), item.getCantidad(), Integer::sum);
        }
        for (Map.Entry<Long, Integer> e : pedidoPorArticulo.entrySet()) {
            ArticuloInventario a = articulos.get(e.getKey());
            if (a.getStock() < e.getValue()) {
                throw new IllegalStateException("Ya no hay suficiente stock de \"" + a.getNombre()
                        + "\" para confirmar este apartado (disponible: " + a.getStock() + "). "
                        + "Probablemente otra solicitud se confirmo primero o se vendio en tienda.");
            }
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal descuentoTotal = BigDecimal.ZERO;
        for (ApartadoItem item : apartado.getItems()) {
            ArticuloInventario articulo = articulos.get(item.getArticulo().getId());
            BigDecimal bruto = item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad()));
            BigDecimal nuevoDescuento = descuentosPorArticulo.getOrDefault(articulo.getId(), item.getDescuento());
            validarDescuentoApartado(apartado.getCentro(), nuevoDescuento, bruto);
            item.setDescuento(nuevoDescuento);
            item.setSubtotal(bruto.subtract(nuevoDescuento));
            subtotal = subtotal.add(bruto);
            descuentoTotal = descuentoTotal.add(nuevoDescuento);

            int anterior = articulo.getStock();
            int nuevo = anterior - item.getCantidad();
            articulo.setStock(nuevo);
            articuloRepository.save(articulo);

            MovimientoInventario mov = new MovimientoInventario();
            mov.setArticulo(articulo);
            mov.setTipo(TipoMovimientoInventario.SALIDA);
            mov.setStockAnterior(anterior);
            mov.setStockNuevo(nuevo);
            mov.setRazon("Apartado #" + apartado.getId() + " confirmado");
            mov.setRegistradoPor(actor);
            movimientoInventarioRepository.save(mov);
        }
        apartado.setSubtotal(subtotal);
        apartado.setDescuento(descuentoTotal);
        apartado.setTotal(subtotal.subtract(descuentoTotal));

        int horas = request.horasVigencia() != null ? request.horasVigencia() : apartado.getCentro().getHorasApartadoDefault();
        LocalDateTime ahora = LocalDateTime.now();
        apartado.setHorasVigencia(horas);
        apartado.setConfirmadoEn(ahora);
        apartado.setExpiraEn(ahora.plusHours(horas));
        apartado.setConfirmadoPor(actor);
        apartado.setEstado(EstadoApartado.ACTIVO);
        apartado = apartadoRepository.save(apartado);

        for (Usuario staff : staffParaNotificar(apartado.getCentro().getId(), actor)) {
            mailService.send(staff.getEmail(), "Apartado #" + apartado.getId() + " confirmado",
                    actor.getNombre() + " confirmo el apartado de " + apartado.getClienteNombre()
                            + ". Vigente hasta " + apartado.getExpiraEn() + ".");
        }
        if (apartado.getClienteEmail() != null && !apartado.getClienteEmail().isBlank()) {
            mailService.send(apartado.getClienteEmail(), "Tu apartado #" + apartado.getId() + " fue confirmado",
                    "Tu reserva quedo confirmada. Tienes hasta " + apartado.getExpiraEn()
                            + " para recogerla y pagarla en la tienda.\n\n" + detalleItems(apartado));
        }
        return toDto(apartado, false);
    }

    @Transactional
    public ApartadoDto completar(Long id, Usuario actor, ApartadoCompleteRequest request) {
        Apartado apartado = bloquear(id, actor); // completar dos veces el mismo apartado -> la segunda ya lo ve COMPLETADO
        if (apartado.getEstado() != EstadoApartado.ACTIVO) {
            throw new IllegalStateException("Este apartado no esta activo");
        }
        VentaDto venta = ventaService.completarDesdeApartado(apartado, request.metodoPago(), request.tipoTicket(),
                request.montoRecibido(), request.clienteEmail(), actor);
        apartado.setEstado(EstadoApartado.COMPLETADO);
        apartado.setCompletadoPor(actor);
        apartado.setVentaId(venta.id());
        apartado = apartadoRepository.save(apartado);

        for (Usuario staff : staffParaNotificar(apartado.getCentro().getId(), actor)) {
            mailService.send(staff.getEmail(), "Apartado #" + apartado.getId() + " completado",
                    actor.getNombre() + " completo el apartado de " + apartado.getClienteNombre() + " (venta #" + venta.id() + ").");
        }
        return toDto(apartado, false);
    }

    @Transactional
    public ApartadoDto cancelar(Long id, Usuario actor, ApartadoCancelRequest request) {
        return toDto(cancelarInterno(bloquear(id, actor), actor, request.motivo()), false);
    }

    /**
     * Cancelacion compartida por el staff (actor != null) y por el propio cliente desde la
     * tienda publica (actor == null, ver cancelarPublico). Si ya estaba ACTIVO, el stock que
     * se habia reservado al confirmarlo se devuelve de inmediato.
     */
    private Apartado cancelarInterno(Apartado apartadoLeido, Usuario actor, String motivo) {
        // Se vuelve a bloquear y releer aqui (aunque el llamador ya lo haya hecho): la tienda
        // publica lo trae de una consulta sin bloqueo, y cancelar dos veces devolveria el stock doble.
        Apartado apartado = bloquear(apartadoLeido.getId(), null);
        if (apartado.getEstado() != EstadoApartado.PENDIENTE && apartado.getEstado() != EstadoApartado.ACTIVO) {
            throw new IllegalStateException("Este apartado ya no se puede cancelar");
        }
        boolean porCliente = actor == null;
        if (apartado.getEstado() == EstadoApartado.ACTIVO) {
            restaurarStock(apartado, "Apartado #" + apartado.getId()
                    + (porCliente ? " cancelado por el cliente" : " cancelado"), actor);
        }
        apartado.setEstado(EstadoApartado.CANCELADO);
        apartado.setCanceladoPor(actor);
        apartado.setCanceladoEn(LocalDateTime.now());
        apartado.setMotivoCancelacion(motivo);
        apartado = apartadoRepository.save(apartado);

        String lineaMotivo = motivo != null && !motivo.isBlank() ? ("\nMotivo: " + motivo) : "";
        String quien = porCliente
                ? "El cliente " + apartado.getClienteNombre() + " (" + apartado.getClienteTelefono()
                        + ") cancelo su apartado desde la tienda en linea"
                : actor.getNombre() + " cancelo el apartado de " + apartado.getClienteNombre();
        for (Usuario staff : staffParaNotificar(apartado.getCentro().getId(), actor)) {
            mailService.send(staff.getEmail(), "Apartado #" + apartado.getId() + " cancelado", quien + lineaMotivo);
        }
        // El cliente que cancela el mismo no necesita un correo avisandole lo que acaba de hacer.
        if (!porCliente && apartado.getClienteEmail() != null && !apartado.getClienteEmail().isBlank()) {
            mailService.send(apartado.getClienteEmail(), "Tu apartado #" + apartado.getId() + " fue cancelado",
                    "Tu reserva fue cancelada." + lineaMotivo);
        }
        if (porCliente) {
            notificacionService.notificarApartadoCanceladoPorCliente(apartado, sucursalUnica(apartado));
        }
        return apartado;
    }

    /** La sucursal de recoleccion si todos los productos son de la misma (para segmentar la
     * notificacion a su personal); null si se mezclan varias (la ve todo el centro). */
    private Sucursal sucursalUnica(Apartado apartado) {
        List<Sucursal> distintas = apartado.getItems().stream()
                .map(i -> i.getArticulo() != null ? i.getArticulo().getSucursal() : null)
                .distinct().toList();
        return distintas.size() == 1 ? distintas.get(0) : null;
    }

    /** Pendientes de atender (badge del menu y aviso en la pantalla de Apartados). */
    @Transactional(readOnly = true)
    public long contarPendientes(Usuario actor) {
        return apartadoRepository.countByCentroIdAndEstado(tenantScope.scopeId(actor), EstadoApartado.PENDIENTE);
    }

    // ------------------------------------------------- seguimiento desde la tienda publica
    // (homologado con DemoPV: consultar por folio+telefono, buscar por telefono, auto-cancelar)

    private static final String CONSULTA_NO_ENCONTRADA =
            "No encontramos un apartado con ese folio y telefono en esta tienda";
    /** Cuantos apartados recientes del centro se revisan al buscar solo por telefono, y cuantos
     * se devuelven como maximo. El telefono se compara normalizado (solo digitos, ultimos 10)
     * en Java y no en SQL, para empatar aunque se haya capturado con espacios/guiones/lada. */
    private static final int CANDIDATOS_BUSQUEDA_TELEFONO = 200;
    private static final int RESULTADOS_BUSQUEDA_TELEFONO = 10;

    @Transactional(readOnly = true)
    public PublicApartadoConsultaDto consultarPublico(String slug, Long folio, String telefono) {
        return toConsultaDto(resolverYVerificarPublico(slug, folio, telefono));
    }

    /**
     * "¿No tienes tu folio?" -- a proposito menos estricto que consultarPublico: quien conozca
     * el telefono de alguien puede ver sus apartados en ESTE centro (mismo criterio que DemoPV).
     */
    @Transactional(readOnly = true)
    public List<PublicApartadoConsultaDto> buscarPorTelefonoPublico(String slug, String telefono) {
        Centro centro = resolverCentroPublico(slug);
        if (soloDigitos(telefono).length() < 7) return List.of();
        return apartadoRepository.findByCentroIdOrderBySolicitadoEnDesc(centro.getId(),
                        PageRequest.of(0, CANDIDATOS_BUSQUEDA_TELEFONO)).stream()
                .filter(a -> telefonosCoinciden(a.getClienteTelefono(), telefono))
                .limit(RESULTADOS_BUSQUEDA_TELEFONO)
                .map(this::toConsultaDto)
                .toList();
    }

    /** El cliente cancela su propio apartado. Folio+telefono se revalidan aqui desde cero:
     * nunca se confia en que el frontend ya lo verifico al consultarlo. Base tambien de
     * "Editar" en la tienda (cancela este y el cliente arma uno nuevo con los mismos productos). */
    @Transactional
    public PublicApartadoConsultaDto cancelarPublico(String slug, Long folio, String telefono, String motivo) {
        Apartado apartado = resolverYVerificarPublico(slug, folio, telefono);
        return toConsultaDto(cancelarInterno(apartado, null, motivo));
    }

    private Centro resolverCentroPublico(String slug) {
        return centroRepository.findBySlugPublicoAndActivoTrue(slug)
                .filter(Centro::isApartadosActivo)
                .orElseThrow(() -> new ResourceNotFoundException("Apartados no disponibles"));
    }

    /** Mismo mensaje generico sea cual sea la falla (folio inexistente, de otro centro, o
     * telefono que no coincide), para no ayudar a adivinar folios ajenos por eliminacion. */
    private Apartado resolverYVerificarPublico(String slug, Long folio, String telefono) {
        Centro centro = resolverCentroPublico(slug);
        Apartado apartado = apartadoRepository.findById(folio)
                .filter(a -> a.getCentro().getId().equals(centro.getId()))
                .orElseThrow(() -> new IllegalArgumentException(CONSULTA_NO_ENCONTRADA));
        if (!telefonosCoinciden(apartado.getClienteTelefono(), telefono)) {
            throw new IllegalArgumentException(CONSULTA_NO_ENCONTRADA);
        }
        return apartado;
    }

    private static String soloDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    private static boolean telefonosCoinciden(String guardado, String capturado) {
        String a = soloDigitos(guardado);
        String b = soloDigitos(capturado);
        if (a.length() < 7 || b.length() < 7) return false;
        return a.substring(Math.max(0, a.length() - 10)).equals(b.substring(Math.max(0, b.length() - 10)));
    }

    private PublicApartadoConsultaDto toConsultaDto(Apartado a) {
        return new PublicApartadoConsultaDto(
                a.getId(),
                a.getEstado().name(),
                a.getTotal(),
                a.getSolicitadoEn(),
                a.getExpiraEn(),
                a.getCanceladoEn(),
                a.getMotivoCancelacion(),
                a.getItems().stream().map(i -> new PublicApartadoConsultaDto.Item(
                        i.getArticulo() != null ? i.getArticulo().getId() : null,
                        i.getArticuloNombre(),
                        i.getArticulo() != null && i.getArticulo().getSucursal() != null
                                ? i.getArticulo().getSucursal().getNombre() : null,
                        i.getCantidad(),
                        i.getSubtotal())).toList());
    }

    @Transactional
    public ApartadoDto eliminarItem(Long apartadoId, Long itemId) {
        Apartado apartado = bloquear(apartadoId, null);
        if (apartado.getEstado() != EstadoApartado.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden quitar articulos mientras el apartado esta pendiente");
        }
        if (apartado.getItems().size() <= 1) {
            throw new IllegalStateException("No puedes quitar el unico articulo; cancela el apartado en su lugar");
        }
        apartado.getItems().removeIf(i -> i.getId().equals(itemId));
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal descuentoTotal = BigDecimal.ZERO;
        for (ApartadoItem item : apartado.getItems()) {
            subtotal = subtotal.add(item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad())));
            descuentoTotal = descuentoTotal.add(item.getDescuento());
        }
        apartado.setSubtotal(subtotal);
        apartado.setDescuento(descuentoTotal);
        apartado.setTotal(subtotal.subtract(descuentoTotal));
        return toDto(apartadoRepository.save(apartado), true);
    }

    // ---------------------------------------------------------------- job de vencimiento
    @Transactional
    public void vencerPendientes() {
        List<Apartado> vencidos = apartadoRepository.findByEstadoAndExpiraEnBefore(EstadoApartado.ACTIVO, LocalDateTime.now());
        for (Apartado candidato : vencidos) {
            // Bloquear y re-verificar: si un cajero lo completo o cancelo justo ahora, ya no se
            // vence (si no, el stock se devolveria aunque el cliente ya se lo llevo).
            Apartado apartado = bloquear(candidato.getId(), null);
            if (apartado.getEstado() != EstadoApartado.ACTIVO || apartado.getExpiraEn() == null
                    || !apartado.getExpiraEn().isBefore(LocalDateTime.now())) {
                continue;
            }
            restaurarStock(apartado, "Apartado #" + apartado.getId() + " vencido", apartado.getConfirmadoPor());
            apartado.setEstado(EstadoApartado.VENCIDO);
            apartadoRepository.save(apartado);

            for (Usuario staff : staffParaNotificar(apartado.getCentro().getId(), null)) {
                mailService.send(staff.getEmail(), "Apartado #" + apartado.getId() + " vencio",
                        "El apartado de " + apartado.getClienteNombre() + " vencio sin que lo recogieran. El stock ya se restituyo.");
            }
            if (apartado.getClienteEmail() != null && !apartado.getClienteEmail().isBlank()) {
                mailService.send(apartado.getClienteEmail(), "Tu apartado #" + apartado.getId() + " vencio",
                        "El tiempo para recoger tu apartado ya paso, asi que se libero. Si aun lo quieres, puedes solicitarlo de nuevo.");
            }
        }
    }

    // ---------------------------------------------------------------- helpers
    private void restaurarStock(Apartado apartado, String razon, Usuario responsable) {
        Map<Long, ArticuloInventario> articulos = bloquearArticulos(apartado.getItems());
        for (ApartadoItem item : apartado.getItems()) {
            if (item.getArticulo() == null) continue;
            ArticuloInventario articulo = articulos.get(item.getArticulo().getId());
            int anterior = articulo.getStock();
            int nuevo = anterior + item.getCantidad();
            articulo.setStock(nuevo);
            articuloRepository.save(articulo);

            MovimientoInventario mov = new MovimientoInventario();
            mov.setArticulo(articulo);
            mov.setTipo(TipoMovimientoInventario.ENTRADA);
            mov.setStockAnterior(anterior);
            mov.setStockNuevo(nuevo);
            mov.setRazon(razon);
            mov.setRegistradoPor(responsable);
            movimientoInventarioRepository.save(mov);
        }
    }

    private void validarDescuentoApartado(Centro centro, BigDecimal descuentoMonto, BigDecimal bruto) {
        if (descuentoMonto == null || descuentoMonto.compareTo(BigDecimal.ZERO) == 0) return;
        if (descuentoMonto.compareTo(BigDecimal.ZERO) < 0 || descuentoMonto.compareTo(bruto) > 0) {
            throw new IllegalArgumentException("El descuento no es valido");
        }
        BigDecimal montoMax = centro.getMontoMaximoDescuentoApartado();
        BigDecimal pctMax = centro.getPorcentajeMaximoDescuentoApartado();
        if (montoMax == null && pctMax == null) {
            throw new IllegalStateException("Este centro no tiene configurado un limite de descuento para apartados");
        }
        boolean dentroDeMonto = montoMax != null && descuentoMonto.compareTo(montoMax) <= 0;
        boolean dentroDePorcentaje = pctMax != null && bruto.compareTo(BigDecimal.ZERO) > 0
                && descuentoMonto.compareTo(bruto.multiply(pctMax).divide(BigDecimal.valueOf(100))) <= 0;
        if (!dentroDeMonto && !dentroDePorcentaje) {
            throw new IllegalArgumentException("El descuento excede el limite permitido para apartados de este centro");
        }
    }

    private List<Usuario> staffParaNotificar(Long centroId, Usuario excluirActor) {
        return usuarioRepository.findActivosConSeccion(centroId, Seccion.TIENDA).stream()
                .filter(u -> excluirActor == null || !u.getId().equals(excluirActor.getId()))
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .toList();
    }

    private String detalleItems(Apartado apartado) {
        StringBuilder sb = new StringBuilder();
        for (ApartadoItem item : apartado.getItems()) {
            sb.append("- ").append(item.getCantidad()).append(" x ").append(item.getArticuloNombre())
                    .append(" ($").append(item.getSubtotal()).append(")\n");
        }
        sb.append("Total: $").append(apartado.getTotal());
        return sb.toString();
    }

    public ApartadoDto toDto(Apartado a, boolean incluirDisponible) {
        List<ApartadoItemDto> items = a.getItems().stream().map(i -> {
            Integer disponible = null;
            if (incluirDisponible && a.getEstado() == EstadoApartado.PENDIENTE) {
                int pendienteOtros = apartadoRepository.sumCantidadPendienteAnteriorPorArticulo(i.getArticulo().getId(), a.getId());
                disponible = i.getArticulo().getStock() - pendienteOtros;
            }
            return new ApartadoItemDto(i.getId(), i.getArticulo().getId(), i.getArticuloNombre(), i.getPrecioUnitario(),
                    i.getCantidad(), i.getDescuento(), i.getSubtotal(), disponible);
        }).toList();
        return new ApartadoDto(
                a.getId(), a.getClienteNombre(), a.getClienteTelefono(), a.getClienteEmail(), a.getNotas(),
                a.getEstado().name(), a.getSubtotal(), a.getDescuento(), a.getTotal(), a.getHorasVigencia(),
                a.getSolicitadoEn(), a.getConfirmadoEn(), a.getExpiraEn(),
                a.getConfirmadoPor() != null ? a.getConfirmadoPor().getNombre() : null,
                a.getCompletadoPor() != null ? a.getCompletadoPor().getNombre() : null,
                a.getCanceladoPor() != null ? a.getCanceladoPor().getNombre() : null,
                a.getCanceladoEn(), a.getMotivoCancelacion(), a.getVentaId(), items
        );
    }
}
