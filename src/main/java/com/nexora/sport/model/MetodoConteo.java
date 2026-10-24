package com.nexora.sport.model;

/**
 * Como se verifico el efectivo de una caja al cerrar un corte (o al moverlo con la caja
 * cerrada). Viaja con la entrega al siguiente turno: si quien recibe cuenta otra cantidad,
 * esto dice a quien le toca revisar la diferencia. Homologado con DemoPV#CashCountMethod.
 */
public enum MetodoConteo {
    /** Lo conto el propio cajero al cerrar su turno. */
    CAJERO,
    /** Lo conto en persona un administrador (cierre forzado presencial o movimiento con caja cerrada). */
    ADMIN_PRESENCIAL,
    /** Lo reporto el cajero a distancia (cierre forzado): queda a su nombre, con evidencia opcional. */
    REPORTADO_REMOTO,
    /** Nadie conto (cierre forzado sin conteo o cierre automatico): se entrega el esperado. */
    NO_CONTADO
}
