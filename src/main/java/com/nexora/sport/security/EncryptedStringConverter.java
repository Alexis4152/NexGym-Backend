package com.nexora.sport.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * Aplica {@link CredentialEncryptionService} en el limite del ORM: un campo anotado con
 * {@code @Convert(converter = EncryptedStringConverter.class)} queda cifrado en la columna
 * y se descifra solo al leer la entidad. @Component para que Hibernate lo tome del
 * contenedor de Spring (y reciba el service por inyeccion) en vez de instanciarlo el.
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final CredentialEncryptionService encryptionService;

    public EncryptedStringConverter(CredentialEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return (attribute == null || attribute.isBlank()) ? attribute : encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return (dbData == null || dbData.isBlank()) ? dbData : encryptionService.decrypt(dbData);
    }
}
