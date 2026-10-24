package com.nexora.sport.dto;

import com.nexora.sport.model.Domicilio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Domicilio estructurado que capturan Centro y Sucursal (todos obligatorios salvo el num. interior). */
public record DomicilioDto(
        @NotBlank(message = "La calle es obligatoria") @Size(max = 150) String calle,
        @NotBlank(message = "El numero exterior es obligatorio") @Size(max = 20) String numeroExterior,
        @Size(max = 20) String numeroInterior,
        @NotBlank(message = "La colonia es obligatoria") @Size(max = 150) String colonia,
        @NotBlank(message = "El municipio es obligatorio") @Size(max = 150) String municipio,
        @NotBlank(message = "El estado es obligatorio") @Size(max = 100) String estado,
        @NotBlank(message = "El pais es obligatorio") @Size(max = 80) String pais,
        @NotBlank(message = "El codigo postal es obligatorio")
        @Pattern(regexp = "\\d{5}", message = "El codigo postal debe tener 5 digitos") String codigoPostal
) {
    public static DomicilioDto de(Domicilio d) {
        if (d == null || d.estaVacio()) return null;
        return new DomicilioDto(d.getCalle(), d.getNumeroExterior(), d.getNumeroInterior(), d.getColonia(),
                d.getMunicipio(), d.getEstado(), d.getPais(), d.getCodigoPostal());
    }

    public Domicilio aEntidad() {
        Domicilio d = new Domicilio();
        d.setCalle(limpio(calle));
        d.setNumeroExterior(limpio(numeroExterior));
        d.setNumeroInterior(limpio(numeroInterior));
        d.setColonia(limpio(colonia));
        d.setMunicipio(limpio(municipio));
        d.setEstado(limpio(estado));
        d.setPais(limpio(pais));
        d.setCodigoPostal(limpio(codigoPostal));
        return d;
    }

    private static String limpio(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
