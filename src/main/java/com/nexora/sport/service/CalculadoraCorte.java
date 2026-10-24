package com.nexora.sport.service;

import com.nexora.sport.dto.CorteCajaResumenDto;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.MovimientoEfectivoRepository;
import com.nexora.sport.repository.PagoMembresiaRepository;
import com.nexora.sport.repository.VentaRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Totales en vivo de un corte y su efectivo esperado (homologado con DemoPV#CashBalanceCalculator,
 * ampliado a membresias):
 *
 *   esperado = fondo inicial + ventas en efectivo + pagos de membresia en efectivo
 *              + entradas (aporte, fondo, pago de faltante, prestamo recibido)
 *              - gastos - retiros - reembolsos - prestamos a otras cajas
 *
 * Sirve igual para un corte abierto (resumen en vivo, validar salidas) que para uno cerrado.
 */
@Component
public class CalculadoraCorte {

    private final VentaRepository ventaRepository;
    private final PagoMembresiaRepository pagoMembresiaRepository;
    private final MovimientoEfectivoRepository movimientoRepository;

    public CalculadoraCorte(VentaRepository ventaRepository, PagoMembresiaRepository pagoMembresiaRepository,
                            MovimientoEfectivoRepository movimientoRepository) {
        this.ventaRepository = ventaRepository;
        this.pagoMembresiaRepository = pagoMembresiaRepository;
        this.movimientoRepository = movimientoRepository;
    }

    public CorteCajaResumenDto resumir(CorteCaja corte) {
        BigDecimal vEfectivo = BigDecimal.ZERO, vTarjeta = BigDecimal.ZERO, vTransferencia = BigDecimal.ZERO,
                vOtro = BigDecimal.ZERO, vTotal = BigDecimal.ZERO, totalCancelado = BigDecimal.ZERO;
        int transacciones = 0, canceladas = 0;
        for (Venta v : ventaRepository.findByCorteCajaId(corte.getId())) {
            if (v.getEstado() == EstadoVenta.CANCELADA) {
                canceladas++;
                totalCancelado = totalCancelado.add(v.getTotal());
                continue;
            }
            transacciones++;
            vTotal = vTotal.add(v.getTotal());
            switch (v.getMetodoPago()) {
                case EFECTIVO -> vEfectivo = vEfectivo.add(v.getTotal());
                case TARJETA -> vTarjeta = vTarjeta.add(v.getTotal());
                case TRANSFERENCIA -> vTransferencia = vTransferencia.add(v.getTotal());
                default -> vOtro = vOtro.add(v.getTotal());
            }
        }

        BigDecimal mEfectivo = BigDecimal.ZERO, mTarjeta = BigDecimal.ZERO, mTransferencia = BigDecimal.ZERO,
                mOtro = BigDecimal.ZERO;
        int pagos = 0;
        for (Object[] fila : pagoMembresiaRepository.sumarPorCorte(corte.getId())) {
            MetodoPago metodo = (MetodoPago) fila[0];
            pagos += ((Number) fila[1]).intValue();
            BigDecimal suma = (BigDecimal) fila[2];
            switch (metodo) {
                case EFECTIVO -> mEfectivo = mEfectivo.add(suma);
                case TARJETA -> mTarjeta = mTarjeta.add(suma);
                case TRANSFERENCIA -> mTransferencia = mTransferencia.add(suma);
                default -> mOtro = mOtro.add(suma);
            }
        }

        BigDecimal entradas = BigDecimal.ZERO, gastos = BigDecimal.ZERO, retiros = BigDecimal.ZERO,
                reembolsos = BigDecimal.ZERO, prestamosSalida = BigDecimal.ZERO;
        for (Object[] fila : movimientoRepository.sumarPorTipo(corte.getId())) {
            BigDecimal suma = (BigDecimal) fila[1];
            switch ((TipoMovimientoEfectivo) fila[0]) {
                case APORTE, FONDO, PAGO_FALTANTE, PRESTAMO_ENTRADA -> entradas = entradas.add(suma);
                case GASTO -> gastos = gastos.add(suma);
                case RETIRO -> retiros = retiros.add(suma);
                case REEMBOLSO -> reembolsos = reembolsos.add(suma);
                case PRESTAMO_SALIDA -> prestamosSalida = prestamosSalida.add(suma);
            }
        }

        BigDecimal inicial = corte.getMontoInicial() != null ? corte.getMontoInicial() : BigDecimal.ZERO;
        BigDecimal esperado = inicial.add(vEfectivo).add(mEfectivo).add(entradas)
                .subtract(gastos).subtract(retiros).subtract(reembolsos).subtract(prestamosSalida);
        boolean ciego = corte.getCentro() != null && corte.getCentro().isConteoCiego();

        return new CorteCajaResumenDto(inicial, vEfectivo, vTarjeta, vTransferencia, vOtro, vTotal, transacciones,
                canceladas, totalCancelado, mEfectivo, mTarjeta, mTransferencia, mOtro,
                mEfectivo.add(mTarjeta).add(mTransferencia).add(mOtro), pagos,
                entradas, gastos, retiros, reembolsos, prestamosSalida, esperado, ciego);
    }

    public BigDecimal efectivoEsperado(CorteCaja corte) {
        return resumir(corte).efectivoEsperado();
    }

    /** Version para conteo ciego: oculta el esperado y todo lo que permitiria despejarlo. */
    public static CorteCajaResumenDto ocultarParaConteoCiego(CorteCajaResumenDto r) {
        return new CorteCajaResumenDto(r.montoInicial(), null, r.ventasTarjeta(), r.ventasTransferencia(), r.ventasOtro(),
                null, r.totalTransacciones(), r.canceladas(), r.totalCancelado(), null, r.membresiasTarjeta(),
                r.membresiasTransferencia(), r.membresiasOtro(), null, r.pagosMembresia(), r.totalEntradas(),
                r.totalGastos(), r.totalRetiros(), r.totalReembolsos(), r.totalPrestamosSalida(), null, true);
    }
}
