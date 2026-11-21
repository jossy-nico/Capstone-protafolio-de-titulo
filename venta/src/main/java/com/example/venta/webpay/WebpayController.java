package com.example.venta.webpay;

import org.springframework.http.HttpHeaders;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

import com.example.venta.entity.Venta;
import com.example.venta.entity.DetalleVenta;
import com.example.venta.service.ComprobantePdfService;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.venta.dto.VentaRequest;
import com.example.venta.entity.PagoWebpay;
import com.example.venta.repository.PagoWebpayRepository;
import com.example.venta.service.VentaService;

import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCommitResponse;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCreateResponse;

@RestController
@RequestMapping("/api/webpay")
@CrossOrigin(origins = "*")
public class WebpayController {

        private final WebpayService webpayService;
        private final VentaService ventaService;
        private final PagoWebpayRepository pagoWebpayRepository;
        private final ComprobantePdfService comprobantePdfService;

        public WebpayController(
                        WebpayService webpayService,
                        VentaService ventaService,
                        PagoWebpayRepository pagoWebpayRepository,
                        ComprobantePdfService comprobantePdfService) {

                this.webpayService = webpayService;
                this.ventaService = ventaService;
                this.pagoWebpayRepository = pagoWebpayRepository;
                this.comprobantePdfService = comprobantePdfService;
        }

        /*
         * ============================================================
         * FLUJO REAL DE COMPRA
         * ============================================================
         */
        @PostMapping("/iniciar")
        public ResponseEntity<Map<String, Object>> iniciarCompra(
                        @RequestBody VentaRequest request) {

                try {

                        double total = ventaService.calcularTotal(request);

                        if (total <= 0) {
                                throw new IllegalArgumentException(
                                                "El total de la compra debe ser mayor a cero.");
                        }

                        String buyOrder = "ORD-" + System.currentTimeMillis();
                        String sessionId = UUID.randomUUID().toString();

                        String returnUrl = "http://10.0.2.2:8383/api/webpay/retorno";

                        WebpayPlusTransactionCreateResponse response = webpayService.iniciarCompra(
                                        request,
                                        buyOrder,
                                        sessionId,
                                        returnUrl);

                        Map<String, Object> resultado = new LinkedHashMap<>();

                        resultado.put("buyOrder", buyOrder);
                        resultado.put("sessionId", sessionId);
                        resultado.put("monto", total);
                        resultado.put("token", response.getToken());
                        resultado.put("url", response.getUrl());

                        return ResponseEntity.ok(resultado);

                } catch (Exception e) {

                        throw new RuntimeException(
                                        "Error al iniciar la compra con Webpay: "
                                                        + e.getMessage(),
                                        e);
                }
        }

        /*
         * ============================================================
         * ENDPOINT TEMPORAL DE PRUEBA
         * ============================================================
         */
        @PostMapping("/crear")
        public ResponseEntity<Map<String, Object>> crearTransaccion(
                        @RequestParam(name = "monto") double monto) {

                try {

                        if (monto <= 0) {
                                throw new IllegalArgumentException(
                                                "El monto debe ser mayor a cero.");
                        }

                        String buyOrder = "ORD-" + System.currentTimeMillis();
                        String sessionId = UUID.randomUUID().toString();

                        String returnUrl = "http://localhost:8383/api/webpay/retorno";

                        WebpayPlusTransactionCreateResponse response = webpayService.crearTransaccion(
                                        buyOrder,
                                        sessionId,
                                        monto,
                                        returnUrl);

                        Map<String, Object> resultado = new LinkedHashMap<>();

                        resultado.put("buyOrder", buyOrder);
                        resultado.put("sessionId", sessionId);
                        resultado.put("monto", monto);
                        resultado.put("token", response.getToken());
                        resultado.put("url", response.getUrl());

                        return ResponseEntity.ok(resultado);

                } catch (Exception e) {

                        throw new RuntimeException(
                                        "Error al crear la transacción Webpay: "
                                                        + e.getMessage(),
                                        e);
                }
        }

        /*
         * ============================================================
         * CONSULTAR ESTADO DEL PAGO
         * ============================================================
         *
         * Android consulta este endpoint utilizando el token recibido
         * al iniciar la compra.
         *
         * Cuando Webpay ya procesó correctamente la transacción,
         * PagoWebpay contiene el ventaId generado.
         */
        @GetMapping("/estado/{token}")
        public ResponseEntity<Map<String, Object>> obtenerEstadoPago(
                        @PathVariable("token") String token) {

                PagoWebpay pago = pagoWebpayRepository
                                .findByTokenWs(token)
                                .orElseThrow(() -> new RuntimeException(
                                                "No se encontró el pago Webpay para el token recibido."));

                Map<String, Object> resultado = new LinkedHashMap<>();

                resultado.put("estado", pago.getEstado());
                resultado.put("ventaId", pago.getVentaId());
                resultado.put("buyOrder", pago.getBuyOrder());
                resultado.put("monto", pago.getMonto());

                return ResponseEntity.ok(resultado);
        }

        /*
         * ==========================================
         * RETORNO WEBPAY - NORMAL Y CANCELADO
         * ==========================================
         *
         * Nunca confirmar un pago sin token_ws.
         * No crear ventas en retornos cancelados.
         * No modificar el flujo de PDF ni JWT.
         * ==========================================
         */
        @RequestMapping(value = "/retorno", method = {
                        RequestMethod.GET,
                        RequestMethod.POST
        }, produces = MediaType.TEXT_HTML_VALUE)
        public ResponseEntity<String> retornoWebpay(
                        @RequestParam(name = "token_ws", required = false) String token,
                        @RequestParam(name = "TBK_TOKEN", required = false) String tokenAbortado,
                        @RequestParam(name = "TBK_ID_SESION", required = false) String sesionAbortada,
                        @RequestParam(name = "TBK_ORDEN_COMPRA", required = false) String ordenAbortada) {

                // ======================================
                // 1. RETORNO SIN TOKEN DE CONFIRMACIÓN
                // ======================================

                if (token == null || token.isBlank()) {

                        boolean retornoInterrumpido = (tokenAbortado != null
                                        && !tokenAbortado.isBlank())
                                        || (sesionAbortada != null
                                                        && !sesionAbortada.isBlank())
                                        || (ordenAbortada != null
                                                        && !ordenAbortada.isBlank());

                        String titulo = retornoInterrumpido
                                        ? "Pago cancelado o interrumpido"
                                        : "No fue posible verificar el pago";

                        String mensaje = retornoInterrumpido
                                        ? "La operación no fue confirmada. "
                                                        + "Puedes volver a la aplicación "
                                                        + "e iniciar un nuevo pago."
                                        : "Webpay no entregó un token válido "
                                                        + "para confirmar la transacción.";

                        System.out.println(
                                        "[WEBPAY] Retorno sin token_ws. "
                                                        + "Interrumpido: "
                                                        + retornoInterrumpido);

                        String html = """
                                        <!DOCTYPE html>
                                        <html lang="es">
                                        <head>
                                            <meta charset="UTF-8">
                                            <meta name="viewport"
                                                  content="width=device-width,
                                                  initial-scale=1.0">
                                            <title>Resultado Webpay</title>
                                        </head>
                                        <body style="
                                            font-family: Arial, sans-serif;
                                            text-align: center;
                                            padding: 40px;
                                        ">
                                            <h2>%s</h2>
                                            <p>%s</p>
                                            <p>
                                                No se ha confirmado una compra
                                                en Natur Cosecha.
                                            </p>
                                        </body>
                                        </html>
                                        """.formatted(titulo, mensaje);

                        return ResponseEntity.ok()
                                        .contentType(MediaType.TEXT_HTML)
                                        .body(html);
                }

                // ======================================
                // 2. RETORNO NORMAL CON TOKEN WEBPAY
                // ======================================

                try {

                        WebpayPlusTransactionCommitResponse response = webpayService.confirmarTransaccion(
                                        token.trim());

                        boolean autorizado = response.getResponseCode() == 0
                                        && "AUTHORIZED".equals(
                                                        response.getStatus());

                        String titulo = autorizado
                                        ? "Pago realizado correctamente"
                                        : "Pago no autorizado";

                        String mensaje = autorizado
                                        ? "La transacción fue autorizada. "
                                                        + "Puedes volver a Natur Cosecha "
                                                        + "para consultar tu comprobante."
                                        : "Webpay no autorizó la transacción. "
                                                        + "No se debe considerar pagada "
                                                        + "esta compra.";

                        String html = """
                                        <!DOCTYPE html>
                                        <html lang="es">
                                        <head>
                                            <meta charset="UTF-8">
                                            <meta name="viewport"
                                                  content="width=device-width,
                                                  initial-scale=1.0">
                                            <title>Resultado Webpay</title>
                                        </head>
                                        <body style="
                                            font-family: Arial, sans-serif;
                                            text-align: center;
                                            padding: 40px;
                                        ">
                                            <h2>%s</h2>
                                            <p>%s</p>
                                        </body>
                                        </html>
                                        """.formatted(titulo, mensaje);

                        return ResponseEntity.ok()
                                        .contentType(MediaType.TEXT_HTML)
                                        .body(html);

                } catch (Exception e) {

                        // No marcar como rechazado un pago cuyo
                        // resultado no pudo verificarse.
                        // Tampoco repetir automáticamente el commit.

                        System.err.println(
                                        "[WEBPAY] Error al confirmar retorno: "
                                                        + e.getClass().getSimpleName());

                        String html = """
                                        <!DOCTYPE html>
                                        <html lang="es">
                                        <head>
                                            <meta charset="UTF-8">
                                            <meta name="viewport"
                                                  content="width=device-width,
                                                  initial-scale=1.0">
                                            <title>Verificación pendiente</title>
                                        </head>
                                        <body style="
                                            font-family: Arial, sans-serif;
                                            text-align: center;
                                            padding: 40px;
                                        ">
                                            <h2>
                                                No fue posible verificar
                                                el resultado del pago
                                            </h2>
                                            <p>
                                                Vuelve a Natur Cosecha
                                                y consulta el estado de
                                                la transacción antes
                                                de intentar pagar nuevamente.
                                            </p>
                                        </body>
                                        </html>
                                        """;

                        return ResponseEntity
                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .contentType(MediaType.TEXT_HTML)
                                        .body(html);
                }
        }

        // ==========================================
        // RECUPERAR COMPRA AUTORIZADA POR WEBPAY
        // ==========================================

        @PostMapping("/compra/recuperar")
        public ResponseEntity<?> recuperarCompra(
                        @RequestBody Map<String, String> request) {

                String token = request == null
                                ? null
                                : request.get("token");

                if (token == null || token.isBlank()) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        "El token de la compra es obligatorio."));
                }

                PagoWebpay pago = pagoWebpayRepository
                                .findByTokenWs(token.trim())
                                .orElse(null);

                if (pago == null) {
                        return ResponseEntity
                                        .status(HttpStatus.NOT_FOUND)
                                        .body(Map.of(
                                                        "error",
                                                        "No se encontró la transacción."));
                }

                if (!"AUTHORIZED".equals(pago.getEstado())
                                || pago.getResponseCode() == null
                                || pago.getResponseCode() != 0
                                || pago.getVentaId() == null) {

                        return ResponseEntity
                                        .status(HttpStatus.CONFLICT)
                                        .body(Map.of(
                                                        "error",
                                                        "La compra todavía no está confirmada."));
                }

                // Acceso temporal mediante el token Webpay.
                // No utilizarlo como código de seguimiento permanente.

                LocalDateTime fechaConfirmacion = pago.getFechaConfirmacion();

                if (fechaConfirmacion == null
                                || LocalDateTime.now().isAfter(
                                                fechaConfirmacion.plusMinutes(15))) {

                        return ResponseEntity
                                        .status(HttpStatus.GONE)
                                        .body(Map.of(
                                                        "error",
                                                        "Finalizó el período de recuperación inmediata."));
                }

                Venta venta = ventaService
                                .obtenerVentaPorId(pago.getVentaId())
                                .orElse(null);

                if (venta == null) {
                        return ResponseEntity
                                        .status(HttpStatus.NOT_FOUND)
                                        .body(Map.of(
                                                        "error",
                                                        "La venta asociada no fue encontrada."));
                }

                List<DetalleVenta> detalles = ventaService.obtenerDetallesPorVenta(
                                venta.getIdVenta());

                Map<String, Object> resultado = new LinkedHashMap<>();

                // ==========================================
                // RESPUESTA DE COMPRA AUTORIZADA
                // ==========================================

                resultado.put("ventaId", venta.getIdVenta());
                resultado.put("buyOrder", pago.getBuyOrder());
                resultado.put("estadoPago", pago.getEstado());
                resultado.put("estadoPedido", venta.getEstadoPedido());
                resultado.put("total", venta.getTotalVenta());
                resultado.put("detalles", detalles);

                // ==========================================
                // HU-33: SEGUIMIENTO PARA INVITADOS
                // ==========================================
                //
                // Solo se entrega al comprador que presenta
                // el token Webpay dentro de la ventana de
                // recuperación autorizada de 15 minutos.
                //
                // No se entrega a clientes registrados.
                // ==========================================

                if (venta.getClienteId() == null) {

                        String codigo = venta.getCodigoSeguimiento();

                        if (codigo == null || codigo.isBlank()) {

                                return ResponseEntity
                                                .status(HttpStatus.CONFLICT)
                                                .body(Map.of(
                                                                "error",
                                                                "La compra fue confirmada, "
                                                                                + "pero su seguimiento todavía "
                                                                                + "no está disponible."));
                        }

                        resultado.put("codigoSeguimiento", codigo);
                }

                return ResponseEntity.ok(resultado);

        }

        // ==========================================
        // WEBPAY - PDF DE COMPRA AUTORIZADA
        // ==========================================

        @PostMapping(value = "/compra/comprobante/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
        public ResponseEntity<?> descargarPdfCompra(
                        @RequestBody(required = false) Map<String, String> request) {

                String token = request == null
                                ? null
                                : request.get("token");

                if (token == null || token.isBlank()) {
                        return ResponseEntity.badRequest()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(Map.of(
                                                        "error",
                                                        "El token de compra es obligatorio."));
                }

                PagoWebpay pago = pagoWebpayRepository
                                .findByTokenWs(token.trim())
                                .orElse(null);

                if (pago == null) {
                        return ResponseEntity
                                        .status(HttpStatus.NOT_FOUND)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(Map.of(
                                                        "error",
                                                        "No se encontró la transacción."));
                }

                boolean autorizado = "AUTHORIZED".equals(pago.getEstado())
                                && pago.getResponseCode() != null
                                && pago.getResponseCode() == 0
                                && pago.getVentaId() != null;

                if (!autorizado) {
                        return ResponseEntity
                                        .status(HttpStatus.CONFLICT)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(Map.of(
                                                        "error",
                                                        "La compra no está confirmada."));
                }

                // Acceso temporal: misma ventana
                // de recuperación inmediata.

                LocalDateTime fecha = pago.getFechaConfirmacion();

                if (fecha == null ||
                                LocalDateTime.now().isAfter(
                                                fecha.plusMinutes(15))) {

                        return ResponseEntity
                                        .status(HttpStatus.GONE)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(Map.of(
                                                        "error",
                                                        "Finalizó el período de acceso inmediato."));
                }

                Long ventaId = pago.getVentaId();

                // Reutilizamos el generador PDF existente.
                byte[] pdf = comprobantePdfService
                                .generarPdf(ventaId);

                return ResponseEntity.ok()
                                .header(
                                                HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"comprobante-venta-"
                                                                + ventaId + ".pdf\"")
                                .contentType(MediaType.APPLICATION_PDF)
                                .body(pdf);
        }

}