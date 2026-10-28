package com.nexora.sport.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.nexora.sport.model.SolicitudArco;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Constancia en PDF de una solicitud ARCO: comprobante de que se recibio y de lo que se
 * respondio/ejecuto. Refleja el estado actual, asi sirve para el correo de respuesta y para
 * el de ejecucion. Documento de PLATAFORMA (NexoraSport), nunca de un centro.
 */
@Service
public class ArcoConstanciaPdfService {

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DIA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color GRAY_TEXT = new Color(90, 90, 90);
    private static final Color GRAY_LINE = new Color(190, 190, 190);

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font FONT_SUBTLE = FontFactory.getFont(FontFactory.HELVETICA, 9, GRAY_TEXT);
    private static final Font FONT_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 10);

    public byte[] generar(SolicitudArco s) {
        Document document = new Document(PageSize.LETTER, 54, 54, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(parrafo("NexoraSport", FONT_SUBTLE, 2));
            document.add(parrafo("Constancia de solicitud de derechos ARCO", FONT_TITLE, 4));
            document.add(parrafo("Folio #" + s.getId(), FONT_SUBTLE, 12));
            separador(document);

            campo(document, "Solicitante", s.getSolicitanteNombre()
                    + (s.getSolicitanteEmail() != null ? " (" + s.getSolicitanteEmail() + ")" : ""));
            if (s.isRepresentante() && s.getRepresentadoNombre() != null) {
                campo(document, "A nombre de", s.getRepresentadoNombre());
            }
            campo(document, "Derecho ejercido", s.getTipo().etiqueta());
            if (s.getRecibidaEn() != null) campo(document, "Recibida formalmente", s.getRecibidaEn().format(FECHA_FMT));
            if (s.getFechaLimite() != null) {
                campo(document, "Plazo legal de respuesta", s.getFechaLimite().format(DIA_FMT)
                        + (s.isExtendida() ? " (extendido una vez)" : ""));
            }
            if (s.getIdentidadVerificadaEn() != null) {
                campo(document, "Identidad verificada", s.getIdentidadVerificadaEn().format(FECHA_FMT));
            }

            if (s.getResultado() != null) {
                separador(document);
                campo(document, "Resultado", s.getResultado().name());
                if (s.getFundamento() != null && !s.getFundamento().isBlank()) campo(document, "Motivo/fundamento", s.getFundamento());
                if (s.getRespuesta() != null) campo(document, "Respuesta", s.getRespuesta());
                if (s.getRespondidaEn() != null) campo(document, "Respondida el", s.getRespondidaEn().format(FECHA_FMT));
            }

            if (s.getEjecutadaEn() != null) {
                separador(document);
                campo(document, "Ejecutada el", s.getEjecutadaEn().format(FECHA_FMT));
                if (s.getResumenEjecucion() != null) campo(document, "Qué se hizo", s.getResumenEjecucion());
            }

            separador(document);
            Paragraph pie = parrafo("Constancia informativa generada automáticamente por el sistema; "
                    + "no sustituye ningún otro medio probatorio.", FONT_SUBTLE, 0);
            pie.setSpacingBefore(10);
            document.add(pie);
            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar la constancia ARCO en PDF", e);
        }
        return out.toByteArray();
    }

    private void campo(Document document, String etiqueta, String valor) throws DocumentException {
        Paragraph p = new Paragraph();
        p.add(new Chunk(etiqueta + ": ", FONT_LABEL));
        p.add(new Chunk(valor, FONT_BODY));
        p.setSpacingAfter(6);
        document.add(p);
    }

    private void separador(Document document) throws DocumentException {
        document.add(new Chunk(new LineSeparator(0.5f, 100, GRAY_LINE, Element.ALIGN_CENTER, -2)));
        Paragraph espacio = new Paragraph(" ");
        espacio.setSpacingAfter(8);
        document.add(espacio);
    }

    private Paragraph parrafo(String texto, Font font, float espacioDespues) {
        Paragraph p = new Paragraph(texto, font);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(espacioDespues);
        return p;
    }
}
