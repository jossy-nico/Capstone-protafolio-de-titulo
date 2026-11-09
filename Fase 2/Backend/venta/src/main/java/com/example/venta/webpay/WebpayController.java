package com.example.venta.webpay;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

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

        public WebpayController(
                        WebpayService webpayService,
                        VentaService ventaService,
                        PagoWebpayRepository pagoWebpayRepository) {

                this.webpayService = webpayService;
                this.ventaService = ventaService;
                this.pagoWebpayRepository = pagoWebpayRepository;
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
         * ============================================================
         * RETORNO WEBPAY
         * ============================================================
         */
        @RequestMapping(value = "/retorno", method = {
                        RequestMethod.GET,
                        RequestMethod.POST
        }, produces = MediaType.TEXT_HTML_VALUE)
        public ResponseEntity<String> retornoWebpay(
                        @RequestParam(name = "token_ws") String token) {

                try {

                        WebpayPlusTransactionCommitResponse response = webpayService.confirmarTransaccion(token);

                        boolean autorizado = response.getResponseCode() == 0
                                        && "AUTHORIZED".equals(
                                                        response.getStatus());

                        String html = """
                                        <!DOCTYPE html>
                                        <html lang="es">

                                        <head>

                                            <meta charset="UTF-8">

                                            <meta name="viewport"
                                                  content="width=device-width,
                                                  initial-scale=1.0">

                                            <title>
                                                Resultado del pago
                                            </title>

                                        </head>

                                        <body style="
                                            font-family: Arial, sans-serif;
                                            text-align: center;
                                            padding: 50px;
                                        ">

                                            <h1>%s</h1>

                                            <h2>%s</h2>

                                            <p>
                                                <strong>Orden:</strong>
                                                %s
                                            </p>

                                            <p>
                                                <strong>Monto:</strong>
                                                $%.0f
                                            </p>

                                            <p>
                                                <strong>Estado Webpay:</strong>
                                                %s
                                            </p>

                                            <p>
                                                <strong>Código respuesta:</strong>
                                                %d
                                            </p>

                                        </body>

                                        </html>
                                        """.formatted(

                                        autorizado
                                                        ? "Pago realizado correctamente"
                                                        : "Pago rechazado",

                                        autorizado
                                                        ? "Transacción AUTORIZADA"
                                                        : "Transacción NO AUTORIZADA",

                                        response.getBuyOrder(),
                                        response.getAmount(),
                                        response.getStatus(),
                                        response.getResponseCode());

                        return ResponseEntity.ok()
                                        .contentType(MediaType.TEXT_HTML)
                                        .body(html);

                } catch (Exception e) {

                        String html = """
                                        <!DOCTYPE html>
                                        <html lang="es">

                                        <head>

                                            <meta charset="UTF-8">

                                            <title>
                                                Error Webpay
                                            </title>

                                        </head>

                                        <body style="
                                            font-family: Arial, sans-serif;
                                            text-align: center;
                                            padding: 50px;
                                        ">

                                            <h1>
                                                Error al confirmar el pago
                                            </h1>

                                            <p>%s</p>

                                        </body>

                                        </html>
                                        """.formatted(
                                        e.getMessage());

                        return ResponseEntity
                                        .internalServerError()
                                        .contentType(MediaType.TEXT_HTML)
                                        .body(html);
                }
        }
}