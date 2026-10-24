package com.nexora.sport.service;

import com.nexora.sport.dto.CajaRegistradoraEstadoDto;
import com.nexora.sport.dto.CajaRegistradoraRequest;
import com.nexora.sport.dto.CajeroDto;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.*;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Cajas registradoras por sucursal (homologado con DemoPV#CashRegisterService): su estado (en
 * uso, o cerrada con su entrega pendiente), alta y edicion.
 *
 * Toda sucursal tiene al menos una caja: si al consultar una sucursal activa no tiene ninguna,
 * se crea "Caja 1" (asi los centros que ya existian antes de las cajas siguen funcionando sin
 * configurar nada). Un centro sin sucursales tiene una caja sin sucursal.
 *
 * Alcance: un cajero/encargado solo ve las cajas de su(s) sucursal(es); Dueño/SUPER_ADMIN todas.
 */
@Service
public class CajaRegistradoraService {

    private final CajaRegistradoraRepository cajaRepository;
    private final CorteCajaRepository corteRepository;
    private final MovimientoEfectivoRepository movimientoRepository;
    private final SucursalRepository sucursalRepository;
    private final CentroRepository centroRepository;
    private final UsuarioRepository usuarioRepository;
    private final CalculadoraCorte calculadora;
    private final MovimientoEfectivoService movimientoService;
    private final TenantScope tenantScope;

    public CajaRegistradoraService(CajaRegistradoraRepository cajaRepository, CorteCajaRepository corteRepository,
                                   MovimientoEfectivoRepository movimientoRepository, SucursalRepository sucursalRepository,
                                   CentroRepository centroRepository, UsuarioRepository usuarioRepository,
                                   CalculadoraCorte calculadora, MovimientoEfectivoService movimientoService,
                                   TenantScope tenantScope) {
        this.cajaRepository = cajaRepository;
        this.corteRepository = corteRepository;
        this.movimientoRepository = movimientoRepository;
        this.sucursalRepository = sucursalRepository;
        this.centroRepository = centroRepository;
        this.usuarioRepository = usuarioRepository;
        this.calculadora = calculadora;
        this.movimientoService = movimientoService;
        this.tenantScope = tenantScope;
    }

    /** Cajas visibles para el actor, con su estado. Crea "Caja 1" donde haga falta. */
    @Transactional
    public List<CajaRegistradoraEstadoDto> listarEstado(Usuario actor) {
        Long centroId = tenantScope.scopeId(actor);
        asegurarCajaPorSucursal(centroId, actor);
        Set<Long> permitidas = tenantScope.sucursalesPermitidas(actor);
        return cajaRepository.findByCentroIdOrderByIdAsc(centroId).stream()
                .filter(c -> c.getSucursal() == null || permitidas == null || permitidas.contains(c.getSucursal().getId()))
                .map(c -> toEstado(c, movimientoService.esAdminDeCaja(c, actor)))
                .toList();
    }

    private void asegurarCajaPorSucursal(Long centroId, Usuario actor) {
        Set<Long> permitidas = tenantScope.sucursalesPermitidas(actor);
        List<Sucursal> sucursales = permitidas == null
                ? sucursalRepository.findByCentroIdAndActivoTrueOrderByNombre(centroId)
                : sucursalRepository.findByCentroIdAndIdInAndActivoTrueOrderByNombre(centroId, permitidas);
        if (sucursales.isEmpty() && !sucursalRepository.existsByCentroIdAndActivoTrue(centroId)) {
            if (!cajaRepository.existsByCentroIdAndSucursalIsNull(centroId)) {
                cajaRepository.save(nuevaCaja(centroId, null, "Caja 1"));
            }
            return;
        }
        for (Sucursal s : sucursales) {
            if (!cajaRepository.existsBySucursalId(s.getId())) {
                cajaRepository.save(nuevaCaja(centroId, s, "Caja 1"));
            }
        }
    }

    private CajaRegistradora nuevaCaja(Long centroId, Sucursal sucursal, String nombre) {
        CajaRegistradora c = new CajaRegistradora();
        c.setCentro(centroRepository.getReferenceById(centroId));
        c.setSucursal(sucursal);
        c.setNombre(nombre);
        return c;
    }

    @Transactional
    public CajaRegistradoraEstadoDto crear(CajaRegistradoraRequest req, Usuario actor) {
        Long centroId = tenantScope.scopeId(actor);
        Sucursal sucursal = null;
        if (req.sucursalId() != null) {
            sucursal = sucursalRepository.findById(req.sucursalId())
                    .filter(s -> s.getCentro().getId().equals(centroId))
                    .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
        } else if (sucursalRepository.existsByCentroIdAndActivoTrue(centroId)) {
            throw new IllegalStateException("Elige la sucursal de la caja");
        }
        CajaRegistradora caja = nuevaCaja(centroId, sucursal, req.nombre().trim());
        if (!movimientoService.esAdminDeCaja(caja, actor)) {
            throw new IllegalStateException("Solo un administrador de esa sucursal puede crear cajas");
        }
        asegurarNombreUnico(centroId, caja.getSucursal(), caja.getNombre(), null);
        return toEstado(cajaRepository.save(caja), true);
    }

    /** Renombra o activa/desactiva una caja. Una caja en uso no se puede desactivar. */
    @Transactional
    public CajaRegistradoraEstadoDto actualizar(Long id, CajaRegistradoraRequest req, Usuario actor) {
        CajaRegistradora caja = buscar(id, actor);
        if (!movimientoService.esAdminDeCaja(caja, actor)) {
            throw new IllegalStateException("Solo un administrador de esa sucursal puede editar cajas");
        }
        String nombre = req.nombre().trim();
        asegurarNombreUnico(caja.getCentro().getId(), caja.getSucursal(), nombre, caja.getId());
        caja.setNombre(nombre);
        if (req.activo() != null && !req.activo() && caja.isActivo()) {
            if (corteRepository.findFirstByCajaRegistradoraIdAndEstado(caja.getId(), EstadoCorteCaja.ABIERTO).isPresent()) {
                throw new IllegalStateException("No se puede desactivar una caja en uso");
            }
        }
        if (req.activo() != null) caja.setActivo(req.activo());
        return toEstado(cajaRepository.save(caja), true);
    }

    public CajaRegistradora buscar(Long id, Usuario actor) {
        return cajaRepository.findById(id)
                .filter(c -> c.getCentro().getId().equals(tenantScope.scopeId(actor)))
                .orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada"));
    }

    private void asegurarNombreUnico(Long centroId, Sucursal sucursal, String nombre, Long excluirId) {
        boolean repetido = cajaRepository.findByCentroIdOrderByIdAsc(centroId).stream()
                .anyMatch(c -> !c.getId().equals(excluirId)
                        && java.util.Objects.equals(c.getSucursal() != null ? c.getSucursal().getId() : null,
                        sucursal != null ? sucursal.getId() : null)
                        && c.getNombre().equalsIgnoreCase(nombre));
        if (repetido) throw new IllegalStateException("Ya existe una caja con ese nombre en esa sucursal");
    }

    /** Personal activo del centro, para "quien retiro/puso el dinero" y "quien reporto el conteo". */
    @Transactional(readOnly = true)
    public List<CajeroDto> personas(Usuario actor) {
        List<CajeroDto> lista = new ArrayList<>(usuarioRepository
                .findByCentroIdAndDeletedAtIsNull(tenantScope.scopeId(actor), PageRequest.of(0, 500)).stream()
                .filter(Usuario::isActivo)
                .map(u -> new CajeroDto(u.getId(), u.getNombre()))
                .sorted((a, b) -> a.nombre().compareToIgnoreCase(b.nombre()))
                .toList());
        if (lista.stream().noneMatch(p -> p.id().equals(actor.getId()))) {
            lista.add(0, new CajeroDto(actor.getId(), actor.getNombre()));
        }
        return lista;
    }

    /** El efectivo esperado de una caja en uso solo lo ve un administrador. */
    private CajaRegistradoraEstadoDto toEstado(CajaRegistradora c, boolean admin) {
        Optional<CorteCaja> abierto = c.getId() == null ? Optional.empty()
                : corteRepository.findFirstByCajaRegistradoraIdAndEstado(c.getId(), EstadoCorteCaja.ABIERTO);
        Long sucursalId = c.getSucursal() != null ? c.getSucursal().getId() : null;
        String sucursalNombre = c.getSucursal() != null ? c.getSucursal().getNombre() : null;
        if (abierto.isPresent()) {
            CorteCaja corte = abierto.get();
            return new CajaRegistradoraEstadoDto(c.getId(), c.getNombre(), sucursalId, sucursalNombre, c.isActivo(),
                    new CajaRegistradoraEstadoDto.CorteAbierto(corte.getId(), corte.getUsuario().getId(),
                            corte.getUsuario().getNombre(), corte.getAbiertoEn(),
                            admin ? calculadora.efectivoEsperado(corte) : null),
                    null);
        }
        CorteCaja ultimo = c.getUltimoCorte();
        CajaRegistradoraEstadoDto.Pendiente pendiente = new CajaRegistradoraEstadoDto.Pendiente(
                c.getMontoPendiente(),
                c.getMetodoConteoPendiente() != null ? c.getMetodoConteoPendiente().name() : null,
                c.getContadoPendientePor() != null ? c.getContadoPendientePor().getNombre() : null,
                c.getContadoPendienteEn(),
                ultimo != null ? ultimo.getId() : null,
                ultimo != null ? ultimo.getUsuario().getNombre() : null,
                ultimo != null ? ultimo.getCerradoEn() : null,
                ultimo != null ? ultimo.getMontoEntregado() : null,
                ultimo != null && ultimo.getMetodoConteo() != null ? ultimo.getMetodoConteo().name() : null,
                c.getId() == null ? List.of()
                        : movimientoRepository.findByCajaRegistradoraIdAndCorteCajaIsNullAndRecibidoPorCorteIsNullOrderByCreatedAtAsc(c.getId())
                        .stream().map(movimientoService::toDto).toList());
        return new CajaRegistradoraEstadoDto(c.getId(), c.getNombre(), sucursalId, sucursalNombre, c.isActivo(), null, pendiente);
    }
}
