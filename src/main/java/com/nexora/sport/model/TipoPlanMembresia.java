package com.nexora.sport.model;

/**
 * Forma en que se consume un {@link MembresiaPlan}. A proposito NO hay un valor
 * distinto por cada granularidad de periodo (semanal/quincenal/mensual/trimestral...):
 * eso se configura con duracionCantidad + duracionUnidad. Este enum solo distingue la
 * FORMA del plan:
 * <ul>
 *   <li>PERIODO: acceso por un rango de fechas (duracionCantidad+duracionUnidad obligatorios).</li>
 *   <li>POR_CLASES: bolsa de clases a consumir (numeroClasesIncluidas obligatorio); la
 *       duracion es opcional (vigencia adicional, ver Membresia#fechaFin).</li>
 *   <li>PASE: caso particular de POR_CLASES con numeroClasesIncluidas fijo en 1 (una
 *       clase individual), separado solo para poder etiquetarlo distinto en la UI.</li>
 *   <li>PERSONALIZADO: sin acoplamiento de campos obligatorios, para configuraciones que
 *       no encajen en los esquemas anteriores.</li>
 * </ul>
 */
public enum TipoPlanMembresia {
    PERIODO,
    POR_CLASES,
    PASE,
    PERSONALIZADO
}
