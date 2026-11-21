
package com.example.venta.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.venta.dto.ComprobanteCompraResponse;
import com.example.venta.entity.Venta;
import com.example.venta.repository.VentaRepository;
import com.example.venta.service.ComprobanteCompraService;
import com.example.venta.service.ComprobantePdfService;

@RestController
@RequestMapping("/api/ventas")
public class ComprobanteCompraController {

        private final ComprobanteCompraService comprobanteCompraService;
        private final ComprobantePdfService comprobantePdfService;
        private final VentaRepository ventaRepository;

        public ComprobanteCompraController(
                        ComprobanteCompraService comprobanteCompraService,
                        ComprobantePdfService comprobantePdfService,
                        VentaRepository ventaRepository) {

                this.comprobanteCompraService = comprobanteCompraService;
                this.comprobantePdfService = comprobantePdfService;
                this.ventaRepository = ventaRepository;
        }

        // ==========================================
        // COMPROBANTE ADMINISTRATIVO EXISTENTE
        // ==========================================

        @GetMapping("/{ventaId}/comprobante")
        public ResponseEntity<ComprobanteCompraResponse> obtenerComprobante(
                        @PathVariable("ventaId") Long ventaId) {

                ComprobanteCompraResponse comprobante = comprobanteCompraService.obtenerComprobante(ventaId);

                return ResponseEntity.ok(comprobante);
        }

        // ==========================================
        // PDF ADMINISTRATIVO EXISTENTE
        // ==========================================
        //
        // Conserva su ruta original.
        // SecurityConfig exige JWT ADMIN.
        // ==========================================

        @GetMapping(value = "/{ventaId}/comprobante/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
        public ResponseEntity<byte[]> descargarComprobantePdf(
                        @PathVariable("ventaId") Long ventaId) {

                byte[] pdf = comprobantePdfService.generarPdf(ventaId);

                return ResponseEntity.ok()
                                .header(
                                                HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"comprobante-venta-"
                                                                + ventaId
                                                                + ".pdf\"")
                                .contentType(MediaType.APPLICATION_PDF)
                                .body(pdf);
        }

        // ==========================================
        // HU-32: PDF DE MI COMPRA CON JWT
        // ==========================================
        //
        // Exclusivo para CLIENTE registrado.
        //
        // Requisitos:
        // 1. JWT válido.
        // 2. Rol CLIENTE.
        // 3. Venta existente.
        // 4. Venta perteneciente al usuario del JWT.
        //
        // No permite consultar ventas de invitados
        // ni ventas pertenecientes a otras cuentas.
        // ==========================================

        @GetMapping("/mis-compras/{ventaId}/comprobante/pdf")
        public ResponseEntity<?> descargarPdfMiCompra(
                        @PathVariable("ventaId") Long ventaId,
                        @AuthenticationPrincipal Jwt jwt) {

                // ======================================
                // 1. VALIDAR AUTENTICACIÓN
                // ======================================

                if (jwt == null || jwt.getSubject() == null) {

                        return ResponseEntity
                                        .status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of(
                                                        "error",
                                                        "Debes iniciar sesión para descargar el comprobante."));
                }

                // ======================================
                // 2. VALIDAR ROL CLIENTE
                // ======================================

                if (!"CLIENTE".equals(
                                jwt.getClaimAsString("rol"))) {

                        return ResponseEntity
                                        .status(HttpStatus.FORBIDDEN)
                                        .body(Map.of(
                                                        "error",
                                                        "La operación requiere una cuenta de cliente."));
                }

                // ======================================
                // 3. OBTENER ID DESDE JWT
                // ======================================

                final Long usuarioId;

                try {

                        usuarioId = Long.valueOf(jwt.getSubject());

                        if (usuarioId <= 0L) {
                                throw new NumberFormatException();
                        }

                } catch (NumberFormatException ex) {

                        return ResponseEntity
                                        .status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of(
                                                        "error",
                                                        "La identidad del usuario no es válida."));
                }

                // ======================================
                // 4. VALIDAR ID DE LA VENTA
                // ======================================

                if (ventaId == null || ventaId <= 0L) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        "El identificador de la compra no es válido."));
                }

                // ======================================
                // 5. CONSULTAR VENTA EN POSTGRESQL
                // ======================================

                Optional<Venta> ventaOptional = ventaRepository.findById(ventaId);

                if (ventaOptional.isEmpty()) {

                        return ResponseEntity
                                        .status(HttpStatus.NOT_FOUND)
                                        .body(Map.of(
                                                        "error",
                                                        "La compra no fue encontrada."));
                }

                Venta venta = ventaOptional.get();

                // ======================================
                // 6. COMPROBAR PROPIEDAD DE LA VENTA
                // ======================================
                //
                // El clienteId debe corresponder al
                // mismo usuario identificado por el JWT.
                //
                // Una venta de invitado tendrá clienteId
                // null y no podrá usar esta ruta.
                // ======================================

                if (venta.getClienteId() == null ||
                                !usuarioId.equals(venta.getClienteId())) {

                        return ResponseEntity
                                        .status(HttpStatus.NOT_FOUND)
                                        .body(Map.of(
                                                        "error",
                                                        "La compra no fue encontrada."));
                }

                // ======================================
                // 7. GENERAR PDF EXISTENTE
                // ======================================
                //
                // No se crea otra venta.
                // No se modifica Webpay.
                // No se duplica la lógica de PDF.
                // ======================================

                byte[] pdf = comprobantePdfService.generarPdf(ventaId);

                // ======================================
                // 8. ENTREGAR COMPROBANTE
                // ======================================

                return ResponseEntity.ok()
                                .header(
                                                HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"comprobante-venta-"
                                                                + ventaId
                                                                + ".pdf\"")
                                .contentType(MediaType.APPLICATION_PDF)
                                .body(pdf);
        }
}
