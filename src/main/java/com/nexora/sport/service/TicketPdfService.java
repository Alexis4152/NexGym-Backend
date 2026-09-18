package com.nexora.sport.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.nexora.sport.model.Centro;
import com.nexora.sport.model.MetodoPago;
import com.nexora.sport.model.Venta;
import com.nexora.sport.model.VentaItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Genera el PDF del ticket de una venta, a partir de lo que ya quedo guardado en la BD. */
@Service
public class TicketPdfService {

    private static final NumberFormat MONEY_FMT = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color GRAY_TEXT = new Color(90, 90, 90);
    private static final Color GRAY_LINE = new Color(190, 190, 190);
    private static final Color GRAY_FILL = new Color(238, 238, 238);

    private static final Font FONT_STORE_NAME = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
    private static final Font FONT_SUBTLE = FontFactory.getFont(FontFactory.HELVETICA, 9, GRAY_TEXT);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font FONT_BODY_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font FONT_TOTAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    public byte[] generar(Venta venta) {
        Document document = new Document(PageSize.A5, 36, 36, 24, 24);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            Centro centro = venta.getCentro();
            agregarLogo(document, centro);
            agregarParrafoCentrado(document, centro.getNombre(), FONT_STORE_NAME);
            if (centro.getDireccion() != null && !centro.getDireccion().isBlank()) {
                agregarParrafoCentrado(document, centro.getDireccion(), FONT_SUBTLE);
            }
            if (centro.getTelefono() != null && !centro.getTelefono().isBlank()) {
                agregarParrafoCentrado(document, "Tel. " + centro.getTelefono(), FONT_SUBTLE);
            }
            document.add(separador());

            document.add(new Paragraph("Le atendio: " + venta.getUsuario().getNombre(), FONT_BODY));
            document.add(new Paragraph("Fecha: " + venta.getCreatedAt().format(DATE_FMT), FONT_BODY));
            document.add(new Paragraph("Folio: #" + venta.getId(), FONT_BODY));
            if (venta.getClienteNombre() != null && !venta.getClienteNombre().isBlank()) {
                document.add(new Paragraph("Cliente: " + venta.getClienteNombre(), FONT_BODY));
            }
            document.add(new Paragraph("Metodo de pago: " + metodoPagoEs(venta.getMetodoPago()), FONT_BODY));
            document.add(separador());

            PdfPTable tabla = new PdfPTable(new float[]{4, 1, 2, 2});
            tabla.setWidthPercentage(100);
            for (String header : new String[]{"Producto", "Cant.", "P.Unit.", "Subtotal"}) {
                PdfPCell cell = new PdfPCell(new Phrase(header, FONT_BODY_BOLD));
                cell.setBackgroundColor(GRAY_FILL);
                cell.setPadding(4);
                tabla.addCell(cell);
            }
            for (VentaItem item : venta.getItems()) {
                tabla.addCell(celda(item.getArticuloNombre()));
                tabla.addCell(celda(String.valueOf(item.getCantidad())));
                tabla.addCell(celda(money(item.getPrecioUnitario())));
                tabla.addCell(celda(money(item.getSubtotal())));
            }
            document.add(tabla);
            document.add(new Paragraph(" "));

            document.add(filaTotal("Subtotal", venta.getSubtotal(), FONT_BODY));
            if (venta.getDescuento() != null && venta.getDescuento().compareTo(BigDecimal.ZERO) > 0) {
                document.add(filaTotal("Descuento", venta.getDescuento().negate(), FONT_BODY));
            }
            if (venta.getImpuesto() != null && venta.getImpuesto().compareTo(BigDecimal.ZERO) > 0) {
                document.add(filaTotal("Impuestos", venta.getImpuesto(), FONT_BODY));
            }
            document.add(filaTotal("TOTAL", venta.getTotal(), FONT_TOTAL));
            if (venta.getMontoRecibido() != null) {
                document.add(filaTotal("Recibido", venta.getMontoRecibido(), FONT_BODY));
                document.add(filaTotal("Cambio", venta.getCambio(), FONT_BODY));
            }
            document.add(separador());

            agregarParrafoCentrado(document, "NexoraSport", FONT_SUBTLE);
            agregarParrafoCentrado(document, "¡Gracias por su compra, vuelva pronto!", FONT_SUBTLE);

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF del ticket", e);
        }
    }

    private void agregarLogo(Document document, Centro centro) {
        if (centro.getLogoUrl() == null || centro.getLogoUrl().isBlank()) return;
        try {
            String relativo = centro.getLogoUrl().replaceFirst("^/uploads/", "");
            Path ruta = Path.of(uploadsDir, relativo).toAbsolutePath().normalize();
            if (!Files.exists(ruta)) return;
            Image logo = Image.getInstance(ruta.toString());
            logo.scaleToFit(70, 70);
            logo.setAlignment(Image.ALIGN_CENTER);
            document.add(logo);
        } catch (Exception ignored) {
            // El logo es decorativo; si falla no debe tumbar la generacion del ticket
        }
    }

    private void agregarParrafoCentrado(Document document, String texto, Font font) throws DocumentException {
        Paragraph p = new Paragraph(texto, font);
        p.setAlignment(Element.ALIGN_CENTER);
        document.add(p);
    }

    private Paragraph separador() {
        Paragraph p = new Paragraph(" ");
        LineSeparator linea = new LineSeparator(0.5f, 100, GRAY_LINE, Element.ALIGN_CENTER, -2);
        p.add(new Chunk(linea));
        return p;
    }

    private PdfPCell celda(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FONT_BODY));
        cell.setPadding(4);
        return cell;
    }

    private Paragraph filaTotal(String etiqueta, BigDecimal monto, Font font) {
        Paragraph p = new Paragraph(etiqueta + ": " + money(monto), font);
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }

    private String money(BigDecimal monto) {
        return MONEY_FMT.format(monto != null ? monto : BigDecimal.ZERO);
    }

    private String metodoPagoEs(MetodoPago metodo) {
        return switch (metodo) {
            case EFECTIVO -> "Efectivo";
            case TARJETA -> "Tarjeta";
            case TRANSFERENCIA -> "Transferencia";
            case OTRO -> "Otro";
        };
    }
}
