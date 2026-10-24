package com.nexora.sport.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Domicilio estructurado (Mexico) de un Centro o Sucursal. Columnas nullable: los registros
 * anteriores solo tenian el texto libre `direccion`, que se sigue llenando a partir de
 * estos campos (lo leen tickets, tienda en linea, etc. sin cambios). */
@Embeddable
@Getter
@Setter
public class Domicilio {

    @Column(name = "dom_calle", length = 150)
    private String calle;

    @Column(name = "dom_numero_exterior", length = 20)
    private String numeroExterior;

    @Column(name = "dom_numero_interior", length = 20)
    private String numeroInterior;

    @Column(name = "dom_colonia", length = 150)
    private String colonia;

    @Column(name = "dom_municipio", length = 150)
    private String municipio;

    @Column(name = "dom_estado", length = 100)
    private String estado;

    @Column(name = "dom_pais", length = 80)
    private String pais;

    @Column(name = "dom_codigo_postal", length = 5)
    private String codigoPostal;

    /** "Calle 123 Int. 4, Col. Centro, C.P. 40200, Taxco de Alarcon, Guerrero, Mexico". */
    public String formatear() {
        List<String> partes = new ArrayList<>();
        StringBuilder linea = new StringBuilder();
        if (tiene(calle)) linea.append(calle.trim());
        if (tiene(numeroExterior)) linea.append(linea.isEmpty() ? "" : " ").append(numeroExterior.trim());
        if (tiene(numeroInterior)) linea.append(linea.isEmpty() ? "" : " ").append("Int. ").append(numeroInterior.trim());
        if (!linea.isEmpty()) partes.add(linea.toString());
        if (tiene(colonia)) partes.add("Col. " + colonia.trim());
        if (tiene(codigoPostal)) partes.add("C.P. " + codigoPostal.trim());
        if (tiene(municipio)) partes.add(municipio.trim());
        if (tiene(estado)) partes.add(estado.trim());
        if (tiene(pais)) partes.add(pais.trim());
        return String.join(", ", partes);
    }

    /** Version recortada para la columna de texto libre `direccion` (250/300 caracteres). */
    public String formatear(int max) {
        String s = formatear();
        return s.length() > max ? s.substring(0, max) : s;
    }

    /** Copia independiente (un @Embeddable no debe compartirse entre dos entidades). */
    public Domicilio copia() {
        Domicilio d = new Domicilio();
        d.calle = calle;
        d.numeroExterior = numeroExterior;
        d.numeroInterior = numeroInterior;
        d.colonia = colonia;
        d.municipio = municipio;
        d.estado = estado;
        d.pais = pais;
        d.codigoPostal = codigoPostal;
        return d;
    }

    public boolean estaVacio() {
        return !tiene(calle) && !tiene(numeroExterior) && !tiene(colonia) && !tiene(codigoPostal);
    }

    private static boolean tiene(String s) {
        return s != null && !s.isBlank();
    }
}
