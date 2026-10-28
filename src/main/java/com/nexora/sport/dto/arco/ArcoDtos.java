package com.nexora.sport.dto.arco;

import com.nexora.sport.model.MetodoIdentidadArco;
import com.nexora.sport.model.ResultadoArco;
import com.nexora.sport.model.TipoDerechoArco;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Payloads del modulo ARCO (ver SolicitudArcoService). */
public final class ArcoDtos {

    private ArcoDtos() {}

    /**
     * Alta de una solicitud. Publica (sin sesion): nombre y correo obligatorios, el correo
     * debe existir en el sistema. Desde la cuenta propia: nombre/correo se ignoran (se toman
     * de la sesion) y, si el tipo requiere identidad fuerte, se pide la contrasena.
     */
    public record CrearRequest(
            @Size(max = 150, message = "El nombre no puede tener más de 150 caracteres") String nombre,
            @Email(message = "Correo inválido") @Size(max = 150, message = "El correo no puede tener más de 150 caracteres") String email,
            @Pattern(regexp = "[0-9]{10}", message = "El teléfono debe tener 10 dígitos") String telefono,
            @NotNull(message = "Elige qué tipo de solicitud quieres hacer") TipoDerechoArco tipo,
            @Size(max = 2000, message = "El detalle no puede tener más de 2000 caracteres") String detalle,
            Boolean representante,
            @Size(max = 150, message = "El nombre no puede tener más de 150 caracteres") String representadoNombre,
            String password,
            @AssertTrue(message = "Debes aceptar el aviso de privacidad") boolean aceptaAvisoPrivacidad
    ) {}

    /** Registro en mostrador (SUPER_ADMIN): correo opcional, identidad ya verificada en persona. */
    public record MostradorRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 150, message = "El nombre no puede tener más de 150 caracteres") String nombre,
            @Email(message = "Correo inválido") @Size(max = 150, message = "El correo no puede tener más de 150 caracteres") String email,
            @Pattern(regexp = "[0-9]{10}", message = "El teléfono debe tener 10 dígitos") String telefono,
            @NotNull(message = "Elige qué tipo de solicitud quiere hacer") TipoDerechoArco tipo,
            @Size(max = 2000, message = "El detalle no puede tener más de 2000 caracteres") String detalle,
            Boolean representante,
            @Size(max = 150, message = "El nombre no puede tener más de 150 caracteres") String representadoNombre,
            @NotBlank(message = "Explica cómo verificaste su identidad") @Size(max = 500, message = "No puede tener más de 500 caracteres") String identidadNotas
    ) {}

    public record VerificarIdentidadRequest(
            @NotNull(message = "Elige cómo verificaste su identidad") MetodoIdentidadArco metodo,
            @NotBlank(message = "Explica cómo la verificaste") @Size(max = 500, message = "No puede tener más de 500 caracteres") String notas
    ) {}

    public record PedirInfoRequest(
            @NotBlank(message = "Explica qué información necesitas") @Size(max = 1000, message = "No puede tener más de 1000 caracteres") String infoSolicitada
    ) {}

    public record ResponderInfoRequest(
            @NotBlank(message = "Escribe tu respuesta") @Size(max = 2000, message = "No puede tener más de 2000 caracteres") String respuesta
    ) {}

    public record ExtenderRequest(
            @NotBlank(message = "Explica por qué necesitas más plazo") @Size(max = 500, message = "No puede tener más de 500 caracteres") String motivo
    ) {}

    /** El fundamento es obligatorio salvo PROCEDENTE (se valida en el servicio). */
    public record ResponderRequest(
            @NotNull(message = "Elige el resultado") ResultadoArco resultado,
            @Size(max = 2000, message = "No puede tener más de 2000 caracteres") String fundamento,
            @NotBlank(message = "Explica tu respuesta (queda como constancia)") @Size(max = 2000, message = "No puede tener más de 2000 caracteres") String respuesta
    ) {}

    public record EjecutarRequest(
            @NotBlank(message = "Describe qué hiciste (queda como constancia)") @Size(max = 2000, message = "No puede tener más de 2000 caracteres") String resumen
    ) {}
}
