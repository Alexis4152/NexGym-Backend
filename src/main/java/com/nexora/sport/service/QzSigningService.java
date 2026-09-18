package com.nexora.sport.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Firma las solicitudes de conexion de QZ Tray (la app que conecta el navegador con la
 * impresora termica). Si las llaves no estan configuradas, la impresion sigue funcionando
 * pero QZ Tray le pedira confirmacion manual al usuario en cada venta.
 */
@Service
public class QzSigningService {

    private static final Logger log = LoggerFactory.getLogger(QzSigningService.class);

    @Value("${app.qz.certificate-path:qz-keys/digital-certificate.txt}")
    private String certificatePath;

    @Value("${app.qz.private-key-path:qz-keys/private-key.pem}")
    private String privateKeyPath;

    private String certificate;
    private PrivateKey privateKey;

    @PostConstruct
    void load() {
        try {
            Path certFile = Path.of(certificatePath);
            if (Files.exists(certFile)) {
                certificate = Files.readString(certFile, StandardCharsets.UTF_8);
            }
            Path keyFile = Path.of(privateKeyPath);
            if (Files.exists(keyFile)) {
                String pem = Files.readString(keyFile, StandardCharsets.UTF_8)
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s", "");
                byte[] decoded = Base64.getDecoder().decode(pem);
                privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
            }
        } catch (Exception e) {
            log.warn("No se pudieron cargar las llaves de QZ Tray ({}): {}", privateKeyPath, e.getMessage());
        }
    }

    public String getCertificate() {
        return certificate;
    }

    public String sign(String data) {
        if (privateKey == null) {
            throw new IllegalStateException("No hay una llave de firma configurada para QZ Tray");
        }
        try {
            Signature signature = Signature.getInstance("SHA512withRSA");
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar la solicitud de QZ Tray", e);
        }
    }
}
