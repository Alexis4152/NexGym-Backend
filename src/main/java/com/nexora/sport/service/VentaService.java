package com.nexora.sport.service;

import com.nexora.sport.dto.*;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.*;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Punto de venta: registra ventas de mostrador, descuenta inventario y las liga a Caja. */
@Service
public class VentaService {

    private final VentaRepository ventaRepository;
    private final CorteCajaRepository corteRepository;
    private final ArticuloInventarioRepository articuloRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final CentroRepository centroRepository;
    private final CajaService cajaService;
    private final MailService mailService;
    private final TicketPdfService ticketPdfService;
    private final EscPosTicketService escPosTicketService;
    private final TenantScope tenantScope;

    public VentaService(VentaRepository ventaRepository, CorteCajaRepository corteRepository,
                         ArticuloInventarioRepository articuloRepository, MovimientoInventarioRepository movimientoInventarioRepository,
                         CentroRepository centroRepository, CajaService cajaService, MailService mailService,
                         TicketPdfService ticketPdfService, EscPosTicketService escPosTicketService, TenantScope tenantScope) {
        this.ventaRepository = ventaRepository;
        this.corteRepository = corteRepository;
        this.articuloRepository = articuloRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.centroRepository = centroRepository;
        this.cajaService = cajaService;
        this.mailService = mailService;
        this.ticketPdfService = ticketPdfService;
        this.escPosTicketService = escPosTicketService;
        this.tenantScope = tenantScope;
    }

    @Transactional(readOnly = true)
    public String ticketEscPos(Long id) {
        return escPosTicketService.build(buscar(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<VentaDto> listar(Usuario actor, Pageable pageable) {
        return PageResponse.of(ventaRepository.findByCentroIdOrderByCreatedAtDesc(tenantScope.scopeId(actor), pageable), this::toDto);
    }

    public Venta buscar(Long id) {
        return ventaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada"));
    }

    @Transactional(readOnly = true)
    public VentaDto obtener(Long id) {
        return toDto(buscar(id));
    }

    @Transactional
    public VentaDto crear(Usuario actor, VentaRequest request) {
        Long centroId = tenantScope.scopeId(actor);
        CorteCaja corte = corteRepository.findFirstByUsuarioIdAndEstado(actor.getId(), EstadoCorteCaja.ABIERTO)
                .orElseThrow(() -> new IllegalStateException("Debes abrir un corte de caja antes de registrar ventas"));

        Venta venta = new Venta();
        venta.setCentro(centroRepository.getReferenceById(centroId));
        venta.setUsuario(actor);
        venta.setCorteCaja(corte);
        venta.setClienteNombre(request.clienteNombre());
        venta.setClienteEmail(request.clienteEmail());
        venta.setMetodoPago(MetodoPago.valueOf(request.metodoPago()));
        venta.setTipoTicket(TipoTicket.valueOf(request.tipoTicket()));
        venta.setNotas(request.notas());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (VentaRequest.ItemRequest ir : request.items()) {
            ArticuloInventario articulo = articuloRepository.findById(ir.articuloId())
                    .orElseThrow(() -> new ResourceNotFoundException("Articulo no encontrado"));
            if (!articulo.isActivo()) {
                throw new IllegalStateException("Articulo inactivo: " + articulo.getNombre());
            }
            if (!articulo.isVendible()) {
                throw new IllegalStateException("Articulo no marcado como vendible: " + articulo.getNombre());
            }
            if (articulo.getStock() < ir.cantidad()) {
                throw new IllegalStateException("Stock insuficiente para " + articulo.getNombre() + " (disponible: " + articulo.getStock() + ")");
            }
            BigDecimal precio = articulo.getPrecioVenta() != null ? articulo.getPrecioVenta() : BigDecimal.ZERO;
            BigDecimal descuentoLinea = ir.descuento() != null ? ir.descuento() : BigDecimal.ZERO;
            BigDecimal bruto = precio.multiply(BigDecimal.valueOf(ir.cantidad()));
            if (descuentoLinea.compareTo(BigDecimal.ZERO) < 0 || descuentoLinea.compareTo(bruto) > 0) {
                throw new IllegalArgumentException("El descuento de " + articulo.getNombre() + " no es valido");
            }
            BigDecimal subtotalLinea = bruto.subtract(descuentoLinea);

            VentaItem item = new VentaItem();
            item.setVenta(venta);
            item.setArticulo(articulo);
            item.setArticuloNombre(articulo.getNombre());
            item.setPrecioUnitario(precio);
            item.setCantidad(ir.cantidad());
            item.setDescuento(descuentoLinea);
            item.setSubtotal(subtotalLinea);
            venta.getItems().add(item);
            subtotal = subtotal.add(subtotalLinea);
        }

        BigDecimal descuentoGlobal = request.descuento() != null ? request.descuento() : BigDecimal.ZERO;
        BigDecimal impuesto = request.impuesto() != null ? request.impuesto() : BigDecimal.ZERO;
        BigDecimal total = subtotal.subtract(descuentoGlobal).add(impuesto);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El total de la venta no puede ser negativo");
        }
        venta.setSubtotal(subtotal);
        venta.setDescuento(descuentoGlobal);
        venta.setImpuesto(impuesto);
        venta.setTotal(total);

        if (venta.getMetodoPago() == MetodoPago.EFECTIVO) {
            BigDecimal recibido = request.montoRecibido();
            if (recibido == null || recibido.compareTo(total) < 0) {
                throw new IllegalArgumentException("El monto recibido debe cubrir el total de la venta");
            }
            venta.setMontoRecibido(recibido);
            venta.setCambio(recibido.subtract(total));
        }

        venta = ventaRepository.save(venta);

        for (VentaItem item : venta.getItems()) {
            ArticuloInventario articulo = item.getArticulo();
            int anterior = articulo.getStock();
            int nuevo = anterior - item.getCantidad();
            articulo.setStock(nuevo);
            articuloRepository.save(articulo);

            MovimientoInventario mov = new MovimientoInventario();
            mov.setArticulo(articulo);
            mov.setTipo(TipoMovimientoInventario.SALIDA);
            mov.setStockAnterior(anterior);
            mov.setStockNuevo(nuevo);
            mov.setRazon("Venta #" + venta.getId());
            mov.setRegistradoPor(actor);
            movimientoInventarioRepository.save(mov);
        }

        cajaService.registrarIngresoDeVenta(centroId, venta, actor);

        if ((venta.getTipoTicket() == TipoTicket.DIGITAL || venta.getTipoTicket() == TipoTicket.AMBOS)
                && venta.getClienteEmail() != null && !venta.getClienteEmail().isBlank()) {
            byte[] pdf = ticketPdfService.generar(venta);
            mailService.enviarConAdjunto(venta.getClienteEmail(), "Tu ticket de compra #" + venta.getId(),
                    "Gracias por tu compra. Adjuntamos tu ticket en PDF.", "ticket-" + venta.getId() + ".pdf", pdf);
        }

        return toDto(venta);
    }

    /** Completa un apartado ya confirmado: genera una Venta real a partir de sus lineas, SIN volver a descontar stock (ya se descarto al confirmar). */
    @Transactional
    public VentaDto completarDesdeApartado(Apartado apartado, String metodoPago, String tipoTicket,
                                            BigDecimal montoRecibido, String clienteEmailOverride, Usuario actor) {
        Long centroId = apartado.getCentro().getId();
        CorteCaja corte = corteRepository.findFirstByUsuarioIdAndEstado(actor.getId(), EstadoCorteCaja.ABIERTO)
                .orElseThrow(() -> new IllegalStateException("Debes abrir un corte de caja antes de completar un apartado"));

        String emailEfectivo = (clienteEmailOverride != null && !clienteEmailOverride.isBlank())
                ? clienteEmailOverride : apartado.getClienteEmail();

        Venta venta = new Venta();
        venta.setCentro(centroRepository.getReferenceById(centroId));
        venta.setUsuario(actor);
        venta.setCorteCaja(corte);
        venta.setClienteNombre(apartado.getClienteNombre());
        venta.setClienteEmail(emailEfectivo);
        venta.setMetodoPago(MetodoPago.valueOf(metodoPago));
        venta.setTipoTicket(TipoTicket.valueOf(tipoTicket));
        venta.setNotas("Generada desde el apartado #" + apartado.getId());

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal descuentoTotal = BigDecimal.ZERO;
        for (ApartadoItem ai : apartado.getItems()) {
            VentaItem item = new VentaItem();
            item.setVenta(venta);
            item.setArticulo(ai.getArticulo());
            item.setArticuloNombre(ai.getArticuloNombre());
            item.setPrecioUnitario(ai.getPrecioUnitario());
            item.setCantidad(ai.getCantidad());
            item.setDescuento(ai.getDescuento());
            item.setSubtotal(ai.getSubtotal());
            venta.getItems().add(item);
            subtotal = subtotal.add(ai.getPrecioUnitario().multiply(BigDecimal.valueOf(ai.getCantidad())));
            descuentoTotal = descuentoTotal.add(ai.getDescuento());
        }
        BigDecimal total = subtotal.subtract(descuentoTotal);
        venta.setSubtotal(subtotal);
        venta.setDescuento(descuentoTotal);
        venta.setImpuesto(BigDecimal.ZERO);
        venta.setTotal(total);

        if (venta.getMetodoPago() == MetodoPago.EFECTIVO) {
            if (montoRecibido == null || montoRecibido.compareTo(total) < 0) {
                throw new IllegalArgumentException("El monto recibido debe cubrir el total del apartado");
            }
            venta.setMontoRecibido(montoRecibido);
            venta.setCambio(montoRecibido.subtract(total));
        }

        venta = ventaRepository.save(venta);

        cajaService.registrarIngresoDeVenta(centroId, venta, actor);

        if ((venta.getTipoTicket() == TipoTicket.DIGITAL || venta.getTipoTicket() == TipoTicket.AMBOS)
                && venta.getClienteEmail() != null && !venta.getClienteEmail().isBlank()) {
            byte[] pdf = ticketPdfService.generar(venta);
            mailService.enviarConAdjunto(venta.getClienteEmail(), "Tu ticket de compra #" + venta.getId(),
                    "Gracias por tu compra. Adjuntamos tu ticket en PDF.", "ticket-" + venta.getId() + ".pdf", pdf);
        }

        return toDto(venta);
    }

    @Transactional
    public VentaDto cancelar(Long id, Usuario actor) {
        if (!tenantScope.isAdminOSuperior(actor)) {
            throw new IllegalStateException("Solo Dueno o Administrador puede cancelar una venta");
        }
        Venta venta = buscar(id);
        if (venta.getEstado() != EstadoVenta.COMPLETADA) {
            throw new IllegalStateException("Esta venta ya esta cancelada");
        }
        for (VentaItem item : venta.getItems()) {
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
            mov.setRazon("Cancelacion de venta #" + venta.getId());
            mov.setRegistradoPor(actor);
            movimientoInventarioRepository.save(mov);
        }
        venta.setEstado(EstadoVenta.CANCELADA);
        venta.setCanceladaPor(actor);
        venta.setCanceladaEn(java.time.LocalDateTime.now());
        return toDto(ventaRepository.save(venta));
    }

    public VentaDto toDto(Venta v) {
        List<VentaItemDto> items = v.getItems().stream().map(i -> new VentaItemDto(
                i.getId(), i.getArticulo().getId(), i.getArticuloNombre(), i.getPrecioUnitario(),
                i.getCantidad(), i.getDescuento(), i.getSubtotal()
        )).toList();
        return new VentaDto(
                v.getId(), v.getCorteCaja().getId(), v.getUsuario().getNombre(), v.getClienteNombre(), v.getClienteEmail(),
                v.getSubtotal(), v.getDescuento(), v.getImpuesto(), v.getTotal(), v.getMontoRecibido(), v.getCambio(),
                v.getMetodoPago().name(), v.getTipoTicket().name(), v.getEstado().name(), v.getNotas(), v.getCreatedAt(), items
        );
    }
}
