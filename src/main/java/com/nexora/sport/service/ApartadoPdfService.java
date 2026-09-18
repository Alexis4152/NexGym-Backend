package com.nexora.sport.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.nexora.sport.model.Centro;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Folleto promocional (PDF con QR) y QR suelto para invitar a los clientes a apartar en linea. */
@Service
public class ApartadoPdfService {

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    public byte[] generarFolleto(Centro centro, String url) {
        if (!centro.isApartadosActivo() || centro.getSlugPublico() == null) {
            throw new IllegalStateException("Activa los apartados y el enlace publico del centro antes de generar el folleto");
        }
        try {
            Document document = new Document(PageSize.LETTER, 48, 48, 48, 48);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            agregarLogo(document, centro);

            Paragraph nombre = new Paragraph(centro.getNombre(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22));
            nombre.setAlignment(Element.ALIGN_CENTER);
            nombre.setSpacingBefore(20);
            document.add(nombre);

            Paragraph subtitulo = new Paragraph("Consulta mi tienda en linea para realizar apartados",
                    FontFactory.getFont(FontFactory.HELVETICA, 14));
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingBefore(10);
            subtitulo.setSpacingAfter(30);
            document.add(subtitulo);

            Image qr = Image.getInstance(generarQrPng(url, 260));
            qr.setAlignment(Image.ALIGN_CENTER);
            document.add(qr);

            Paragraph link = new Paragraph(url, FontFactory.getFont(FontFactory.HELVETICA, 11));
            link.setAlignment(Element.ALIGN_CENTER);
            link.setSpacingBefore(15);
            document.add(link);

            document.close();
            return out.toByteArray();
        } catch (DocumentException | WriterException | IOException e) {
            throw new IllegalStateException("No se pudo generar el folleto", e);
        }
    }

    public byte[] generarQr(String url) {
        try {
            return generarQrPng(url, 400);
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("No se pudo generar el codigo QR", e);
        }
    }

    private byte[] generarQrPng(String url, int size) throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix matrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, size, size, hints);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    private void agregarLogo(Document document, Centro centro) {
        if (centro.getLogoUrl() == null || centro.getLogoUrl().isBlank()) return;
        try {
            String relativo = centro.getLogoUrl().replaceFirst("^/uploads/", "");
            Path ruta = Path.of(uploadsDir, relativo).toAbsolutePath().normalize();
            if (!Files.exists(ruta)) return;
            Image logo = Image.getInstance(ruta.toString());
            logo.scaleToFit(90, 90);
            logo.setAlignment(Image.ALIGN_CENTER);
            document.add(logo);
        } catch (Exception ignored) {
            // El logo es decorativo; si falla no debe tumbar la generacion del folleto
        }
    }
}
