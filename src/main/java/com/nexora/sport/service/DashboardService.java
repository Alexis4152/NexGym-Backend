package com.nexora.sport.service;

import com.nexora.sport.dto.DashboardResponse;
import com.nexora.sport.model.EstadoAlumno;
import com.nexora.sport.model.EstadoMembresia;
import com.nexora.sport.model.TipoMovimiento;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.AlumnoRepository;
import com.nexora.sport.repository.MembresiaRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class DashboardService {

    private final AlumnoRepository alumnoRepository;
    private final MembresiaRepository membresiaRepository;
    private final CajaService cajaService;
    private final InventarioService inventarioService;
    private final AsistenciaService asistenciaService;
    private final MembresiaService membresiaService;
    private final TenantScope tenantScope;

    public DashboardService(AlumnoRepository alumnoRepository, MembresiaRepository membresiaRepository,
                             CajaService cajaService, InventarioService inventarioService,
                             AsistenciaService asistenciaService, MembresiaService membresiaService,
                             TenantScope tenantScope) {
        this.alumnoRepository = alumnoRepository;
        this.membresiaRepository = membresiaRepository;
        this.cajaService = cajaService;
        this.inventarioService = inventarioService;
        this.asistenciaService = asistenciaService;
        this.membresiaService = membresiaService;
        this.tenantScope = tenantScope;
    }

    public DashboardResponse obtener(Usuario actor) {
        Long centroId = tenantScope.scopeId(actor);
        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate hoy = LocalDate.now();

        BigDecimal ingresos = cajaService.sumMonto(centroId, TipoMovimiento.INGRESO, inicioMes, hoy);
        BigDecimal egresos = cajaService.sumMonto(centroId, TipoMovimiento.EGRESO, inicioMes, hoy);

        return new DashboardResponse(
                ingresos, egresos, ingresos.subtract(egresos),
                alumnoRepository.countByCentroIdAndEstadoAndDeletedAtIsNull(centroId, EstadoAlumno.ACTIVO),
                membresiaRepository.countByCentroIdAndEstado(centroId, EstadoMembresia.VENCIDA),
                membresiaRepository.findProximasAVencer(centroId, hoy, hoy.plusDays(7)).size(),
                asistenciaService.contarHoy(centroId),
                inventarioService.stockBajo(actor),
                membresiaService.proximasAVencer(actor, 7)
        );
    }
}
