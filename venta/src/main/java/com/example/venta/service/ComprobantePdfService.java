package com.example.venta.service;

import com.example.venta.dto.ComprobanteCompraResponse;
import com.example.venta.dto.DetalleComprobanteResponse;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class ComprobantePdfService {

    private final ComprobanteCompraService comprobanteCompraService;

    public ComprobantePdfService(
            ComprobanteCompraService comprobanteCompraService) {
        this.comprobanteCompraService = comprobanteCompraService;
    }

    public byte[] generarPdf(Long ventaId) {

        ComprobanteCompraResponse comprobante = comprobanteCompraService.obtenerComprobante(ventaId);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, outputStream);

            document.open();

            // ==============================
            // TÍTULO
            // ==============================

            Font tituloFont = new Font(
                    Font.HELVETICA,
                    18,
                    Font.BOLD);

            Paragraph titulo = new Paragraph(
                    "NATUR COSECHA",
                    tituloFont);

            titulo.setAlignment(Element.ALIGN_CENTER);

            document.add(titulo);

            Font subtituloFont = new Font(
                    Font.HELVETICA,
                    14,
                    Font.BOLD);

            Paragraph subtitulo = new Paragraph(
                    "COMPROBANTE DE COMPRA",
                    subtituloFont);

            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingAfter(20);

            document.add(subtitulo);

            // ==============================
            // INFORMACIÓN DE LA COMPRA
            // ==============================

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                    "dd-MM-yyyy HH:mm");

            document.add(new Paragraph(
                    "Venta N°: "
                            + comprobante.getVentaId()));

            document.add(new Paragraph(
                    "Orden Webpay: "
                            + comprobante.getBuyOrder()));

            if (comprobante.getFechaVenta() != null) {
                document.add(new Paragraph(
                        "Fecha: "
                                + comprobante
                                        .getFechaVenta()
                                        .format(formatter)));
            }

            document.add(new Paragraph(
                    "Estado del pago: "
                            + comprobante.getEstadoPago()));

            document.add(Chunk.NEWLINE);

            // ==============================
            // CLIENTE
            // ==============================

            Font seccionFont = new Font(
                    Font.HELVETICA,
                    12,
                    Font.BOLD);

            document.add(new Paragraph(
                    "DATOS DEL CLIENTE",
                    seccionFont));

            document.add(new Paragraph(
                    "Nombre: "
                            + valorSeguro(
                                    comprobante.getNombreCliente())
                            + " "
                            + valorSeguro(
                                    comprobante.getApellidoCliente())));

            document.add(new Paragraph(
                    "Correo: "
                            + valorSeguro(
                                    comprobante.getCorreoCliente())));

            if (comprobante.getTelefonoCliente() != null) {
                document.add(new Paragraph(
                        "Teléfono: "
                                + comprobante.getTelefonoCliente()));
            }

            if (comprobante.getDireccionCliente() != null) {
                document.add(new Paragraph(
                        "Dirección: "
                                + comprobante.getDireccionCliente()));
            }

            document.add(Chunk.NEWLINE);

            // ==============================
            // DETALLE DE PRODUCTOS
            // ==============================

            document.add(new Paragraph(
                    "DETALLE DE LA COMPRA",
                    seccionFont));

            document.add(Chunk.NEWLINE);

            PdfPTable tabla = new PdfPTable(4);

            tabla.setWidthPercentage(100);

            tabla.setWidths(
                    new float[] { 4f, 2f, 2f, 2f });

            agregarCabecera(tabla, "Producto");
            agregarCabecera(tabla, "Cantidad");
            agregarCabecera(tabla, "Precio kilo");
            agregarCabecera(tabla, "Subtotal");

            for (DetalleComprobanteResponse detalle : comprobante.getDetalles()) {

                tabla.addCell(
                        valorSeguro(
                                detalle.getNombreProducto()));

                tabla.addCell(
                        detalle.getCantidadGramos()
                                + " g");

                tabla.addCell(
                        formatearPeso(
                                detalle.getPrecioKilo()));

                tabla.addCell(
                        formatearPeso(
                                detalle.getSubtotal()));
            }

            document.add(tabla);

            document.add(Chunk.NEWLINE);

            // ==============================
            // TOTAL
            // ==============================

            Font totalFont = new Font(
                    Font.HELVETICA,
                    14,
                    Font.BOLD);

            Paragraph total = new Paragraph(
                    "TOTAL PAGADO: "
                            + formatearPeso(
                                    comprobante.getTotal()),
                    totalFont);

            total.setAlignment(Element.ALIGN_RIGHT);

            document.add(total);

            document.add(Chunk.NEWLINE);

            // ==============================
            // DATOS WEBPAY
            // ==============================

            document.add(new Paragraph(
                    "INFORMACIÓN DEL PAGO",
                    seccionFont));

            document.add(new Paragraph(
                    "Medio de pago: Webpay"));

            document.add(new Paragraph(
                    "Código de autorización: "
                            + valorSeguro(
                                    comprobante.getAuthorizationCode())));

            document.add(new Paragraph(
                    "Tipo de pago: "
                            + valorSeguro(
                                    comprobante.getPaymentTypeCode())));

            if (comprobante.getCardLast4() != null) {
                document.add(new Paragraph(
                        "Tarjeta terminada en: **** "
                                + comprobante.getCardLast4()));
            }

            if (comprobante.getFechaPago() != null) {
                document.add(new Paragraph(
                        "Fecha de pago: "
                                + comprobante
                                        .getFechaPago()
                                        .format(formatter)));
            }

            document.add(Chunk.NEWLINE);

            // ==============================
            // ACLARACIÓN
            // ==============================

            Font notaFont = new Font(
                    Font.HELVETICA,
                    9,
                    Font.ITALIC);

            Paragraph nota = new Paragraph(
                    "Documento generado como comprobante de compra. "
                            + "No corresponde a una boleta electrónica tributaria.",
                    notaFont);

            nota.setAlignment(Element.ALIGN_CENTER);

            document.add(nota);

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException(
                    "No fue posible generar el comprobante PDF.",
                    e);
        }
    }

    private void agregarCabecera(
            PdfPTable tabla,
            String texto) {

        Font font = new Font(
                Font.HELVETICA,
                10,
                Font.BOLD);

        PdfPCell celda = new PdfPCell(
                new Phrase(texto, font));

        celda.setPadding(7);

        tabla.addCell(celda);
    }

    private String valorSeguro(String valor) {

        return valor != null
                ? valor
                : "-";
    }

    private String formatearPeso(Double valor) {

        if (valor == null) {
            return "$0";
        }

        return String.format(
                "$%,.0f",
                valor)
                .replace(",", ".");
    }
}