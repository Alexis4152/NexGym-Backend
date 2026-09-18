package com.nexora.sport.service;

import com.nexora.sport.model.Centro;
import com.nexora.sport.model.MetodoPago;
import com.nexora.sport.model.Venta;
import com.nexora.sport.model.VentaItem;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Construye el comando ESC/POS (impresora termica de 58mm) del ticket de una venta, como hex string. */
@Service
public class EscPosTicketService {

    private static final int WIDTH = 32;
    private static final NumberFormat MONEY_FMT = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public String build(Venta venta) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        Centro centro = venta.getCentro();

        ctl(buf, 0x1B, 0x40); // init
        ctl(buf, 0x1B, 0x61, 0x01); // centrado
        ctl(buf, 0x1B, 0x45, 0x01); // negritas on
        text(buf, wrap(centro.getNombre()) + "\n");
        ctl(buf, 0x1B, 0x45, 0x00); // negritas off
        if (centro.getDireccion() != null && !centro.getDireccion().isBlank()) {
            text(buf, wrap(centro.getDireccion()) + "\n");
        }
        text(buf, line() + "\n");

        ctl(buf, 0x1B, 0x61, 0x00); // alinear izquierda
        text(buf, "Folio: #" + venta.getId() + "\n");
        text(buf, "Fecha: " + venta.getCreatedAt().format(DATE_FMT) + "\n");
        text(buf, "Atendio: " + wrap(venta.getUsuario().getNombre()) + "\n");
        if (venta.getClienteNombre() != null && !venta.getClienteNombre().isBlank()) {
            text(buf, "Cliente: " + wrap(venta.getClienteNombre()) + "\n");
        }
        text(buf, line() + "\n");

        for (VentaItem item : venta.getItems()) {
            text(buf, wrap(item.getArticuloNombre()) + "\n");
            String izq = item.getCantidad() + " x " + money(item.getPrecioUnitario());
            text(buf, padBetween(izq, money(item.getSubtotal())) + "\n");
        }
        text(buf, line() + "\n");

        text(buf, padBetween("Subtotal", money(venta.getSubtotal())) + "\n");
        if (venta.getDescuento() != null && venta.getDescuento().compareTo(BigDecimal.ZERO) > 0) {
            text(buf, padBetween("Descuento", "-" + money(venta.getDescuento())) + "\n");
        }
        if (venta.getImpuesto() != null && venta.getImpuesto().compareTo(BigDecimal.ZERO) > 0) {
            text(buf, padBetween("Impuestos", money(venta.getImpuesto())) + "\n");
        }
        ctl(buf, 0x1B, 0x45, 0x01);
        text(buf, padBetween("TOTAL", money(venta.getTotal())) + "\n");
        ctl(buf, 0x1B, 0x45, 0x00);
        if (venta.getMontoRecibido() != null) {
            text(buf, padBetween("Recibido", money(venta.getMontoRecibido())) + "\n");
            text(buf, padBetween("Cambio", money(venta.getCambio())) + "\n");
        }
        text(buf, line() + "\n");

        ctl(buf, 0x1B, 0x61, 0x01);
        text(buf, "Gracias por su compra\n");
        text(buf, "\n\n\n");
        ctl(buf, 0x1D, 0x56, 0x01); // corte parcial

        if (venta.getMetodoPago() == MetodoPago.EFECTIVO) {
            ctl(buf, 0x1B, 0x70, 0x00, 0x19, 0xFA); // abrir cajon de dinero
        }

        return toHex(buf.toByteArray());
    }

    private void ctl(ByteArrayOutputStream buf, int... bytes) {
        for (int b : bytes) buf.write(b);
    }

    private void text(ByteArrayOutputStream buf, String s) {
        buf.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    private String line() {
        return "-".repeat(WIDTH);
    }

    private String padBetween(String izq, String der) {
        int espacio = WIDTH - izq.length() - der.length();
        if (espacio < 1) return izq + " " + der;
        return izq + " ".repeat(espacio) + der;
    }

    private String wrap(String texto) {
        if (texto == null) return "";
        return texto.length() > WIDTH ? texto.substring(0, WIDTH) : texto;
    }

    private String money(BigDecimal monto) {
        return MONEY_FMT.format(monto != null ? monto : BigDecimal.ZERO);
    }
}
