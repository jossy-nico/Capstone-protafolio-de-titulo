package com.example.venta.webpay;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.venta.dto.VentaRequest;
import com.example.venta.entity.CompraPendiente;
import com.example.venta.entity.PagoWebpay;
import com.example.venta.entity.Venta;
import com.example.venta.repository.CompraPendienteRepository;
import com.example.venta.repository.PagoWebpayRepository;
import com.example.venta.service.VentaService;
import com.fasterxml.jackson.databind.ObjectMapper;

import cl.transbank.common.IntegrationType;
import cl.transbank.webpay.common.WebpayOptions;
import cl.transbank.webpay.webpayplus.WebpayPlus;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCommitResponse;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCreateResponse;

@Service
public class WebpayService {

    private static final String COMMERCE_CODE = "597055555532";

    private static final String API_KEY = System.getenv("WEBPAY_API_KEY");

    private final WebpayPlus.Transaction transaction;

    private final PagoWebpayRepository pagoWebpayRepository;
    private final CompraPendienteRepository compraPendienteRepository;
    private final VentaService ventaService;
    private final ObjectMapper objectMapper;

    public WebpayService(
            PagoWebpayRepository pagoWebpayRepository,
            CompraPendienteRepository compraPendienteRepository,
            VentaService ventaService,
            ObjectMapper objectMapper) {

        this.pagoWebpayRepository = pagoWebpayRepository;
        this.compraPendienteRepository = compraPendienteRepository;
        this.ventaService = ventaService;
        this.objectMapper = objectMapper;

        WebpayOptions options = new WebpayOptions(
                COMMERCE_CODE,
                API_KEY,
                IntegrationType.TEST);

        this.transaction = new WebpayPlus.Transaction(options);
    }

    /*
     * ============================================================
     * TRANSACCIÓN MANUAL DE PRUEBA
     * ============================================================
     */
    public WebpayPlusTransactionCreateResponse crearTransaccion(
            String buyOrder,
            String sessionId,
            double amount,
            String returnUrl) throws Exception {

        WebpayPlusTransactionCreateResponse response = transaction.create(
                buyOrder,
                sessionId,
                amount,
                returnUrl);

        guardarPagoPendiente(
                response.getToken(),
                buyOrder,
                sessionId,
                amount);

        return response;
    }

    /*
     * ============================================================
     * INICIAR COMPRA REAL
     * ============================================================
     *
     * Todavía NO crea Venta.
     * Todavía NO descuenta stock.
     */
    public WebpayPlusTransactionCreateResponse iniciarCompra(
            VentaRequest request,
            String buyOrder,
            String sessionId,
            String returnUrl) throws Exception {

        double total = ventaService.calcularTotal(request);

        if (total <= 0) {
            throw new IllegalArgumentException(
                    "El total de la compra debe ser mayor a cero.");
        }

        WebpayPlusTransactionCreateResponse response = transaction.create(
                buyOrder,
                sessionId,
                total,
                returnUrl);

        guardarPagoPendiente(
                response.getToken(),
                buyOrder,
                sessionId,
                total);

        String ventaRequestJson = objectMapper.writeValueAsString(request);

        CompraPendiente compraPendiente = new CompraPendiente();

        compraPendiente.setTokenWs(
                response.getToken());

        compraPendiente.setBuyOrder(
                buyOrder);

        compraPendiente.setVentaRequestJson(
                ventaRequestJson);

        compraPendiente.setEstado(
                "PENDIENTE");

        compraPendiente.setFechaCreacion(
                LocalDateTime.now());

        compraPendienteRepository.save(
                compraPendiente);

        return response;
    }

    /*
     * ============================================================
     * CONFIRMAR TRANSACCIÓN WEBPAY
     * ============================================================
     *
     * Si Webpay autoriza:
     *
     * 1. Recuperamos CompraPendiente.
     * 2. Reconstruimos VentaRequest.
     * 3. Creamos Venta.
     * 4. crearVenta() descuenta stock.
     * 5. Guardamos ventaId en PagoWebpay.
     * 6. Guardamos ventaId en CompraPendiente.
     * 7. Marcamos CompraPendiente como PROCESADA.
     */
    public WebpayPlusTransactionCommitResponse confirmarTransaccion(
            String token) throws Exception {

        PagoWebpay pago = pagoWebpayRepository
                .findByTokenWs(token)
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró el pago Webpay para el token recibido."));

        /*
         * IDEMPOTENCIA LOCAL
         *
         * Si ya existe una venta asociada, no intentamos
         * confirmar nuevamente ni crear otra venta.
         *
         * En el flujo normal Webpay no debería enviar dos veces
         * el commit, pero esta comprobación protege nuestra venta.
         */
        if (pago.getVentaId() != null) {
            throw new IllegalStateException(
                    "La transacción ya fue procesada. Venta asociada: "
                            + pago.getVentaId());
        }

        WebpayPlusTransactionCommitResponse response = transaction.commit(token);

        /*
         * Guardamos SIEMPRE el resultado entregado por Webpay.
         */
        pago.setEstado(
                response.getStatus());

        pago.setResponseCode(
                (int) response.getResponseCode());

        pago.setAuthorizationCode(
                response.getAuthorizationCode());

        pago.setPaymentTypeCode(
                response.getPaymentTypeCode());

        pago.setInstallmentsNumber(
                (int) response.getInstallmentsNumber());

        pago.setFechaConfirmacion(
                LocalDateTime.now());

        if (response.getCardDetail() != null) {

            pago.setCardLast4(
                    response.getCardDetail()
                            .getCardNumber());
        }

        pagoWebpayRepository.save(pago);

        boolean autorizado = response.getResponseCode() == 0
                && "AUTHORIZED".equals(
                        response.getStatus());

        /*
         * Si Webpay rechazó/canceló:
         *
         * - NO Venta
         * - NO descuento stock
         */
        if (!autorizado) {

            compraPendienteRepository
                    .findByTokenWs(token)
                    .ifPresent(compra -> {

                        compra.setEstado(
                                "RECHAZADA");

                        compra.setFechaProcesamiento(
                                LocalDateTime.now());

                        compraPendienteRepository.save(
                                compra);
                    });

            return response;
        }

        /*
         * ========================================================
         * WEBPAY AUTORIZADO
         * ========================================================
         */

        CompraPendiente compraPendiente = compraPendienteRepository
                .findByTokenWs(token)
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró la compra pendiente asociada al pago."));

        /*
         * Segunda protección contra creación duplicada.
         */
        if (compraPendiente.getVentaId() != null) {

            pago.setVentaId(
                    compraPendiente.getVentaId());

            pagoWebpayRepository.save(pago);

            return response;
        }

        /*
         * Convertimos nuevamente el JSON guardado
         * a nuestro VentaRequest.
         */
        VentaRequest ventaRequest = objectMapper.readValue(
                compraPendiente.getVentaRequestJson(),
                VentaRequest.class);

        /*
         * AQUÍ recién nace la venta.
         *
         * crearVenta() actualmente:
         *
         * - valida
         * - descuenta stock
         * - calcula total
         * - crea Venta
         * - crea DetalleVenta
         * - compensa stock si algo falla
         */
        Venta venta = ventaService.crearVenta(
                ventaRequest);

        /*
         * Asociamos Webpay con Venta.
         */
        pago.setVentaId(
                venta.getIdVenta());

        pagoWebpayRepository.save(
                pago);

        /*
         * Cerramos CompraPendiente.
         */
        compraPendiente.setVentaId(
                venta.getIdVenta());

        compraPendiente.setEstado(
                "PROCESADA");

        compraPendiente.setFechaProcesamiento(
                LocalDateTime.now());

        compraPendienteRepository.save(
                compraPendiente);

        return response;
    }

    /*
     * ============================================================
     * GUARDAR PAGO PENDIENTE
     * ============================================================
     */
    private void guardarPagoPendiente(
            String token,
            String buyOrder,
            String sessionId,
            double amount) {

        PagoWebpay pago = new PagoWebpay();

        pago.setTokenWs(
                token);

        pago.setBuyOrder(
                buyOrder);

        pago.setSessionId(
                sessionId);

        pago.setMonto(
                amount);

        pago.setEstado(
                "PENDIENTE");

        pago.setFechaCreacion(
                LocalDateTime.now());

        pagoWebpayRepository.save(
                pago);
    }
}