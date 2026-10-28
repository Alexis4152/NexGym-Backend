package com.nexora.sport.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Dias habiles para los plazos legales de ARCO. Igual que DemoPV, solo excluye sabados y
 * domingos (sin calendario de festivos): un festivo contado como habil da, a lo mucho, un
 * dia de margen de menos.
 */
public final class DiasHabiles {

    private DiasHabiles() {}

    public static LocalDate sumar(LocalDate inicio, int dias) {
        LocalDate fecha = inicio;
        int sumados = 0;
        while (sumados < dias) {
            fecha = fecha.plusDays(1);
            if (fecha.getDayOfWeek() != DayOfWeek.SATURDAY && fecha.getDayOfWeek() != DayOfWeek.SUNDAY) {
                sumados++;
            }
        }
        return fecha;
    }
}
