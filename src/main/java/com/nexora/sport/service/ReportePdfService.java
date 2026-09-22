package com.nexora.sport.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.nexora.sport.dto.reportes.*;
import com.nexora.sport.model.Centro;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * PDF de reporte general del periodo, mismo estilo visual que TicketPdfService (fuentes,
 * colores, tablas) para no inventar una segunda identidad grafica. Respeta el filtro de
 * Centro/periodo aplicado en el resto del modulo.
 */
@Service
public class ReportePdfService {

    private static final NumberFormat MONEY_FMT = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color GRAY_TEXT = new Color(90, 90, 90);
    private static final Color GRAY_LINE = new Color(190, 190, 190);
    private static final Color GRAY_FILL = new Color(238, 238, 238);

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font FONT_SUBTLE = FontFactory.getFont(FontFactory.HELVETICA, 9, GRAY_TEXT);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font FONT_BODY_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

    private final CentroRepository centroRepository;
    private final ReporteTiendaService tiendaService;
    private final ReporteFinancieroService financieroService;
    private final ReporteMembresiaService membresiaService;
    private final ReporteAlumnoService alumnoService;
    private final TenantScope tenantScope;

    public ReportePdfService(CentroRepository centroRepository, ReporteTiendaService tiendaService,
                              ReporteFinancieroService financieroService, ReporteMembresiaService membresiaService,
                              ReporteAlumnoService alumnoService, TenantScope tenantScope) {
        this.centroRepository = centroRepository;
        this.tiendaService = tiendaService;
        this.financieroService = financieroService;
        this.membresiaService = membresiaService;
        this.alumnoService = alumnoService;
        this.tenantScope = tenantScope;
    }

    @Transactional(readOnly = true)
    public byte[] generar(Usuario actor, LocalDate from, LocalDate to) {
        Centro centro = centroRepository.getReferenceById(tenantScope.scopeId(actor));
        FinancieroResumenDto financiero = financieroService.resumen(actor, from, to);
        VentaResumenDto tienda = tiendaService.resumen(actor, from, to);
        CarteraResumenDto cartera = financieroService.cartera(actor);
        AlumnosResumenDto alumnos = alumnoService.resumen(actor, from, to);
        MembresiaVentasResumenDto membresias = membresiaService.ventas(actor, from, to);

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Reporte general", FONT_TITLE));
            document.add(new Paragraph(centro.getNombre(), FONT_BODY_BOLD));
            document.add(new Paragraph("Periodo: " + from.format(DATE_FMT) + " a " + to.format(DATE_FMT), FONT_SUBTLE));
            document.add(new Paragraph("Generado: " + LocalDate.now().format(DATE_FMT), FONT_SUBTLE));
            document.add(separador());

            document.add(new Paragraph("Finanzas", FONT_SECTION));
            document.add(filaEtiquetaValor("Ingresos", financiero.ingresos()));
            document.add(filaEtiquetaValor("Egresos", financiero.egresos()));
            document.add(filaEtiquetaValor("Resultado", financiero.resultado()));
            document.add(new Paragraph(" "));
            tablaEtiquetaValor(document, "Ingresos por concepto", financiero.ingresosPorConcepto());
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Tienda", FONT_SECTION));
            document.add(new Paragraph("Total vendido: " + money(tienda.totalVentas()) + "   ·   Ventas: " + tienda.numeroVentas()
                    + "   ·   Ticket promedio: " + money(tienda.ticketPromedio()), FONT_BODY));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Membresias", FONT_SECTION));
            document.add(new Paragraph("Nuevas contrataciones: " + membresias.nuevasMembresias()
                    + "   ·   Total contratado: " + money(membresias.totalContratado())
                    + "   ·   Total cobrado en el periodo: " + money(membresias.totalCobrado()), FONT_BODY));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Cartera (saldo pendiente a la fecha de generacion)", FONT_SECTION));
            document.add(new Paragraph("Por cobrar: " + money(cartera.totalPorCobrar())
                    + "   ·   Alumnos con saldo: " + cartera.alumnosConSaldo()
                    + "   ·   Membresias con saldo: " + cartera.membresiasConSaldo(), FONT_BODY));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Alumnos", FONT_SECTION));
            document.add(new Paragraph("Activos: " + alumnos.activos() + "   ·   Nuevos en el periodo: " + alumnos.nuevos()
                    + "   ·   Bajas en el periodo: " + alumnos.bajas(), FONT_BODY));

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF del reporte", e);
        }
    }

    private void tablaEtiquetaValor(Document document, String titulo, List<EtiquetaValorDto> filas) throws DocumentException {
        if (filas.isEmpty()) return;
        document.add(new Paragraph(titulo, FONT_BODY_BOLD));
        PdfPTable tabla = new PdfPTable(new float[]{4, 1, 2});
        tabla.setWidthPercentage(100);
        for (String header : new String[]{"Concepto", "Cant.", "Total"}) {
            PdfPCell cell = new PdfPCell(new Phrase(header, FONT_BODY_BOLD));
            cell.setBackgroundColor(GRAY_FILL);
            cell.setPadding(4);
            tabla.addCell(cell);
        }
        for (EtiquetaValorDto fila : filas) {
            tabla.addCell(celda(fila.etiqueta()));
            tabla.addCell(celda(String.valueOf(fila.cantidad())));
            tabla.addCell(celda(money(fila.total())));
        }
        document.add(tabla);
    }

    private Paragraph filaEtiquetaValor(String etiqueta, BigDecimal monto) {
        return new Paragraph(etiqueta + ": " + money(monto), FONT_BODY);
    }

    private PdfPCell celda(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FONT_BODY));
        cell.setPadding(4);
        return cell;
    }

    private Paragraph separador() {
        Paragraph p = new Paragraph(" ");
        p.add(new Chunk(new LineSeparator(0.5f, 100, GRAY_LINE, Element.ALIGN_CENTER, -2)));
        return p;
    }

    private String money(BigDecimal monto) {
        return MONEY_FMT.format(monto != null ? monto : BigDecimal.ZERO);
    }
}
