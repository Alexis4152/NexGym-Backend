package com.nexora.sport.dto;

import java.util.List;

/** Resultado de buscar un C.P. en el catalogo de SEPOMEX. Un C.P. casi siempre cae en un solo
 * municipio/estado, pero hay excepciones: por eso se regresan como listas. */
public record CodigoPostalDto(
        String codigoPostal,
        List<String> estados,
        List<String> municipios,
        List<Colonia> colonias
) {
    public record Colonia(String nombre, String tipo, String municipio, String estado) {}
}
