package com.nexora.sport.dto.publico;

/** emailContacto/horasApartadoDefault: para el bloque "Informacion y contacto" y el texto de
 * plazo de la pantalla de exito de la tienda publica (homologado con DemoPV). */
public record PublicCentroDto(String nombre, String logoUrl, String colorPrimario, String telefono, String direccion,
                               String emailContacto, int horasApartadoDefault, boolean apartadosActivo) {}
