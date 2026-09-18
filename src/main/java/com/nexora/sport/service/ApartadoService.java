package com.nexora.sport.service;

import com.nexora.sport.dto.*;
import com.nexora.sport.dto.publico.PublicApartadoResponseDto;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.*;
import com.nexora.sport.security.TenantScope;
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

    public ApartadoService(ApartadoRepository apartadoRepository, ArticuloInventarioRepository articuloRepository,
                            MovimientoInventarioRepository movimientoInventarioRepository, CentroRepository centroRepository,
                            UsuarioRepository usuarioRepository, VentaService ventaService, MailService mailService,
                            TenantScope tenantScope) {
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

        return new PublicApartadoResponseDto(apartado.getId(), apartado.getEstado().name(), apartado.getTotal(),
                centro.getHorasApartadoDefault(), apartado.getSolicitadoEn());
    }

    // ---------------------------------------------------------------- admin
    @Transactional(readOnly = true)
    public PageResponse<ApartadoDto> listar(Usuario actor, EstadoApartado estado, Pageable pageable) {
        Long centroId = tenantScope.scopeId(actor);
        var page = estado != null
                ? apartadoRepository.findByCentroIdAndEstadoOrderBySolicitadoEnDesc(centroId, estado, pageable)
                : apartadoRepository.findByCentroIdOrderBySolicitadoEnDesc(centroId, pageable);
        return PageResponse.of(page, a -> toDto(a, false));
    }

    public Apartado buscar(Long id) {
        return apartadoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Apartado no encontrado"));
    }

    @Transactional(readOnly = true)
    public ApartadoDto obtener(Long id) {
        return toDto(buscar(id), true);
    }

    @Transactional
    public ApartadoDto confirmar(Long id, Usuario actor, ApartadoConfirmRequest request) {
        Apartado apartado = buscar(id);
        if (apartado.getEstado() != EstadoApartado.PENDIENTE) {
            throw new IllegalStateException("Este apartado ya fue procesado");
        }
        Map<Long, BigDecimal> descuentosPorArticulo = new HashMap<>();
        if (request.descuentos() != null) {
            request.descuentos().forEach(d -> descuentosPorArticulo.put(d.articuloId(), d.descuento()));
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal descuentoTotal = BigDecimal.ZERO;
        for (ApartadoItem item : apartado.getItems()) {
            ArticuloInventario articulo = item.getArticulo();
            if (articulo.getStock() < item.getCantidad()) {
                throw new IllegalStateException("Stock insuficiente para \"" + item.getArticuloNombre()
                        + "\" (disponible: " + articulo.getStock() + ")");
            }
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
        Apartado apartado = buscar(id);
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
        Apartado apartado = buscar(id);
        if (apartado.getEstado() != EstadoApartado.PENDIENTE && apartado.getEstado() != EstadoApartado.ACTIVO) {
            throw new IllegalStateException("Este apartado ya no se puede cancelar");
        }
        if (apartado.getEstado() == EstadoApartado.ACTIVO) {
            restaurarStock(apartado, "Apartado #" + apartado.getId() + " cancelado", actor);
        }
        apartado.setEstado(EstadoApartado.CANCELADO);
        apartado.setCanceladoPor(actor);
        apartado.setCanceladoEn(LocalDateTime.now());
        apartado.setMotivoCancelacion(request.motivo());
        apartado = apartadoRepository.save(apartado);

        for (Usuario staff : staffParaNotificar(apartado.getCentro().getId(), actor)) {
            mailService.send(staff.getEmail(), "Apartado #" + apartado.getId() + " cancelado",
                    actor.getNombre() + " cancelo el apartado de " + apartado.getClienteNombre()
                            + (request.motivo() != null ? ("\nMotivo: " + request.motivo()) : ""));
        }
        if (apartado.getClienteEmail() != null && !apartado.getClienteEmail().isBlank()) {
            mailService.send(apartado.getClienteEmail(), "Tu apartado #" + apartado.getId() + " fue cancelado",
                    "Tu reserva fue cancelada." + (request.motivo() != null ? ("\nMotivo: " + request.motivo()) : ""));
        }
        return toDto(apartado, false);
    }

    @Transactional
    public ApartadoDto eliminarItem(Long apartadoId, Long itemId) {
        Apartado apartado = buscar(apartadoId);
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
        for (Apartado apartado : vencidos) {
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
        for (ApartadoItem item : apartado.getItems()) {
            ArticuloInventario articulo = item.getArticulo();
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
                int pendienteOtros = apartadoRepository.sumCantidadPendientePorArticulo(i.getArticulo().getId(), a.getId());
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
