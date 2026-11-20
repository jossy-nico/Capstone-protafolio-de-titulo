
package com.example.venta.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.venta.dto.VentaRequest;
import com.example.venta.entity.DetalleVenta;
import com.example.venta.entity.EstadoPedido;
import com.example.venta.entity.Venta;
import com.example.venta.service.DetalleVentaService;
import com.example.venta.service.VentaService;
import com.example.venta.dto.SeguimientoPedidoResponse;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

        private final VentaService ventaService;
        private final DetalleVentaService detalleVentaService;

        public VentaController(
                        VentaService ventaService,
                        DetalleVentaService detalleVentaService) {

                this.ventaService = ventaService;
                this.detalleVentaService = detalleVentaService;
        }

        // ==========================================
        // CREAR VENTA
        // RUTA ADMINISTRATIVA ACTUAL
        // ==========================================

        @PostMapping("/crear")
        public ResponseEntity<Venta> crearVenta(
                        @RequestBody VentaRequest request) {

                Venta venta = ventaService.crearVenta(request);

                return ResponseEntity.ok(venta);
        }

        // ==========================================
        // HU-32: MIS COMPRAS
        // IDENTIDAD OBTENIDA DEL JWT
        // ==========================================

        @GetMapping("/mis-compras")
        public ResponseEntity<?> misCompras(
                        @AuthenticationPrincipal Jwt jwt) {

                if (jwt == null || jwt.getSubject() == null) {
                        return ResponseEntity
                                        .status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of(
                                                        "error",
                                                        "Debes iniciar sesión para consultar tus compras."));
                }

                final Long clienteId;

                try {
                        clienteId = Long.valueOf(jwt.getSubject());
                } catch (NumberFormatException ex) {

                        return ResponseEntity
                                        .status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of(
                                                        "error",
                                                        "La identidad del token no es válida."));
                }

                // El clienteId se obtiene del JWT validado.
                // Nunca se recibe desde el navegador o Android.

                List<Venta> compras = ventaService.obtenerVentasPorCliente(clienteId);

                return ResponseEntity.ok(compras);
        }

        // ==========================================
        // HU-32: DETALLES DE MI COMPRA
        // CLIENTE REGISTRADO CON JWT
        // ==========================================
        //
        // No modifica el endpoint ADMIN existente.
        //
        // Solo devuelve detalles cuando:
        // 1. Existe un JWT válido.
        // 2. El rol del JWT es CLIENTE.
        // 3. La venta pertenece al usuario del JWT.
        //
        // Las compras invitadas no se consultan
        // mediante este endpoint.
        // ==========================================

        @GetMapping("/mis-compras/{idVenta}/detalles")
        public ResponseEntity<?> obtenerDetallesMiCompra(
                        @PathVariable("idVenta") Long idVenta,
                        @AuthenticationPrincipal Jwt jwt) {

                // 1. Exigir identidad autenticada.

                if (jwt == null || jwt.getSubject() == null) {

                        return ResponseEntity
                                        .status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of(
                                                        "error",
                                                        "Debes iniciar sesión para consultar la compra."));
                }

                // 2. Comprobar el rol.

                if (!"CLIENTE".equals(jwt.getClaimAsString("rol"))) {

                        return ResponseEntity
                                        .status(HttpStatus.FORBIDDEN)
                                        .body(Map.of(
                                                        "error",
                                                        "La operación requiere una cuenta de cliente."));
                }

                // 3. Obtener la identidad desde el JWT.

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
                                                        "La identidad del token no es válida."));
                }

                // 4. Validar el ID solicitado.

                if (idVenta == null || idVenta <= 0L) {

                        return ResponseEntity.badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        "El identificador de la venta no es válido."));
                }

                // 5. Consultar la venta existente.

                var ventaOptional = ventaService.obtenerVentaPorId(idVenta);

                if (ventaOptional.isEmpty()) {

                        return ResponseEntity
                                        .status(HttpStatus.NOT_FOUND)
                                        .body(Map.of(
                                                        "error",
                                                        "La compra no fue encontrada."));
                }

                Venta venta = ventaOptional.get();

                // 6. Comprobar propiedad de la venta.
                //
                // Nunca confiar únicamente en idVenta.
                // El clienteId registrado debe coincidir
                // con el usuario autenticado.

                if (venta.getClienteId() == null
                                || !usuarioId.equals(venta.getClienteId())) {

                        // 404 evita revelar si otra persona
                        // tiene una compra con ese ID.

                        return ResponseEntity
                                        .status(HttpStatus.NOT_FOUND)
                                        .body(Map.of(
                                                        "error",
                                                        "La compra no fue encontrada."));
                }

                // 7. Entregar exclusivamente los detalles
                // de la venta cuya titularidad se comprobó.

                List<DetalleVenta> detalles = ventaService.obtenerDetallesPorVenta(idVenta);

                return ResponseEntity.ok(detalles);
        }

        // ==========================================
        // HU-32: SEGUIMIENTO DE COMPRADORES INVITADOS
        // ==========================================
        //
        // No requiere JWT.
        // El comprador debe presentar su código privado.
        //
        // Seguridad:
        // - No permite modificar el pedido.
        // - No devuelve información personal.
        // - No devuelve el código privado.
        // - No permite consultar por ID de venta.
        // ==========================================

        // =========================================================
        // HU-33: CONSULTA DE PEDIDOS PARA COMPRADORES INVITADOS
        // =========================================================
        //
        // Requiere:
        // - Número de orden de Webpay.
        // - Código privado de seguimiento.
        //
        // No requiere iniciar sesión.
        // Solo devuelve datos logísticos del pedido.
        // =========================================================

        @PostMapping("/invitados/seguimiento")
        public ResponseEntity<?> obtenerSeguimientoInvitado(
                        @RequestBody(required = false) Map<String, String> request) {

                final String mensajeError = "No se encontró un seguimiento válido para los datos ingresados.";

                // 1. Validar solicitud.
                if (request == null) {
                        return ResponseEntity.badRequest().body(
                                        Map.of("error", "Debes ingresar los datos del pedido."));
                }

                // 2. Obtener número de orden y código privado.
                String numeroOrden = request.get("numeroOrden");
                String codigoSeguimiento = request.get("codigoSeguimiento");

                // 3. Validar campos obligatorios.
                if (numeroOrden == null || numeroOrden.isBlank()
                                || codigoSeguimiento == null
                                || codigoSeguimiento.isBlank()) {

                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "error",
                                                        "Debes ingresar el número de orden y el código de seguimiento."));
                }

                // 4. Normalizar los datos.
                numeroOrden = numeroOrden.trim();
                codigoSeguimiento = codigoSeguimiento.trim();

                // 5. Consultar seguimiento.
                try {

                        SeguimientoPedidoResponse seguimiento = ventaService.obtenerSeguimientoInvitado(
                                        numeroOrden,
                                        codigoSeguimiento);

                        return ResponseEntity.ok(seguimiento);

                } catch (IllegalArgumentException ex) {

                        // No revelar cuál de los dos datos es incorrecto.
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                                        Map.of("error", mensajeError));
                }
        }

        // ==========================================
        // HU-32: SEGUIMIENTO DE MI PEDIDO
        // CLIENTE AUTENTICADO CON JWT
        // ==========================================

        @GetMapping("/mis-compras/{idVenta}/seguimiento")
        public ResponseEntity<?> obtenerSeguimientoMiCompra(
                        @PathVariable("idVenta") Long idVenta,
                        @AuthenticationPrincipal Jwt jwt) {

                // 1. Verificar autenticación.

                if (jwt == null || jwt.getSubject() == null) {

                        return ResponseEntity
                                        .status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of(
                                                        "error",
                                                        "Debes iniciar sesión para consultar el seguimiento."));
                }

                // 2. Verificar rol CLIENTE.

                if (!"CLIENTE".equals(jwt.getClaimAsString("rol"))) {

                        return ResponseEntity
                                        .status(HttpStatus.FORBIDDEN)
                                        .body(Map.of(
                                                        "error",
                                                        "Esta consulta requiere una cuenta de cliente."));
                }

                // 3. Obtener identidad desde JWT.

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
                                                        "El identificador del usuario no es válido."));
                }

                // 4. Validar identificador de la compra.

                if (idVenta == null || idVenta <= 0L) {

                        return ResponseEntity.badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        "El identificador de la compra no es válido."));
                }

                // 5. Consultar venta.

                var ventaOptional = ventaService.obtenerVentaPorId(idVenta);

                if (ventaOptional.isEmpty()) {

                        return ResponseEntity
                                        .status(HttpStatus.NOT_FOUND)
                                        .body(Map.of(
                                                        "error",
                                                        "El pedido no fue encontrado."));
                }

                Venta venta = ventaOptional.get();

                // 6. Comprobar propiedad.
                //
                // Se asume que venta.clienteId
                // almacena el ID de usuario del JWT.
                // Esa relación debe verificarse.

                if (venta.getClienteId() == null
                                || !usuarioId.equals(venta.getClienteId())) {

                        return ResponseEntity
                                        .status(HttpStatus.NOT_FOUND)
                                        .body(Map.of(
                                                        "error",
                                                        "El pedido no fue encontrado."));
                }

                // 7. Obtener datos logísticos.
                //
                // La autorización ya fue validada.

                SeguimientoPedidoResponse seguimiento = ventaService.obtenerSeguimiento(idVenta);

                return ResponseEntity.ok(seguimiento);
        }

        // ==========================================
        // OBTENER VENTA POR ID
        // ACCESO ADMINISTRATIVO
        // ==========================================

        @GetMapping("/{id}")
        public ResponseEntity<Venta> obtenerVenta(
                        @PathVariable("id") Long id) {

                return ventaService.obtenerVentaPorId(id)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        }

        // ==========================================
        // OBTENER VENTAS POR CLIENTE
        // ACCESO ADMINISTRATIVO
        // ==========================================

        @GetMapping("/cliente/{clienteId}")
        public ResponseEntity<List<Venta>> obtenerVentasPorCliente(
                        @PathVariable("clienteId") Long clienteId) {

                return ResponseEntity.ok(
                                ventaService.obtenerVentasPorCliente(clienteId));
        }

        // ==========================================
        // LISTAR TODAS LAS VENTAS
        // ACCESO ADMINISTRATIVO
        // ==========================================

        @GetMapping
        public ResponseEntity<List<Venta>> listarTodas() {

                return ResponseEntity.ok(
                                ventaService.listarTodas());
        }

        // ==========================================
        // OBTENER DETALLES DE LA VENTA
        // ACCESO ADMINISTRATIVO
        // ==========================================

        @GetMapping("/{idVenta}/detalles")
        public ResponseEntity<List<DetalleVenta>> obtenerDetalles(
                        @PathVariable("idVenta") Long idVenta) {

                return ResponseEntity.ok(
                                ventaService.obtenerDetallesPorVenta(idVenta));
        }

        // ==========================================
        // ACTUALIZAR VENTA
        // ACCESO ADMINISTRATIVO
        // ==========================================

        @PutMapping("/{idVenta}")
        public ResponseEntity<Venta> actualizarVenta(
                        @PathVariable("idVenta") Long idVenta,
                        @RequestBody VentaRequest request) {

                Venta ventaActualizada = ventaService.actualizarVenta(idVenta, request);

                return ResponseEntity.ok(ventaActualizada);
        }

        // ==========================================
        // HU-32: ACTUALIZAR ESTADO DEL PEDIDO
        // ACCESO ADMINISTRATIVO
        // ==========================================

        @PatchMapping("/{idVenta}/estado")
        public ResponseEntity<?> actualizarEstadoPedido(
                        @PathVariable("idVenta") Long idVenta,
                        @RequestBody Map<String, String> request) {

                try {

                        if (request == null) {
                                return ResponseEntity.badRequest()
                                                .body(Map.of(
                                                                "error",
                                                                "Los datos del pedido son obligatorios."));
                        }

                        String estadoRecibido = request.get("estado");

                        if (estadoRecibido == null
                                        || estadoRecibido.isBlank()) {

                                return ResponseEntity.badRequest()
                                                .body(Map.of(
                                                                "error",
                                                                "El estado del pedido es obligatorio."));
                        }

                        EstadoPedido nuevoEstado;

                        try {

                                nuevoEstado = EstadoPedido.valueOf(
                                                estadoRecibido.trim().toUpperCase());

                        } catch (IllegalArgumentException ex) {

                                return ResponseEntity.badRequest()
                                                .body(Map.of(
                                                                "error",
                                                                "El estado del pedido no es válido."));
                        }

                        Venta ventaActualizada = ventaService.actualizarEstadoPedido(
                                        idVenta,
                                        nuevoEstado);

                        return ResponseEntity.ok(ventaActualizada);

                } catch (IllegalArgumentException | IllegalStateException ex) {

                        return ResponseEntity.badRequest()
                                        .body(Map.of("error", ex.getMessage()));
                }
        }

        // ==========================================
        // HU-32: REGISTRAR DESPACHO
        // ACCESO ADMINISTRATIVO
        // ==========================================

        @PatchMapping("/{idVenta}/despacho")
        public ResponseEntity<?> registrarDespacho(
                        @PathVariable("idVenta") Long idVenta,
                        @RequestBody Map<String, String> request) {

                try {

                        if (request == null) {
                                return ResponseEntity.badRequest()
                                                .body(Map.of(
                                                                "error",
                                                                "Los datos del despacho son obligatorios."));
                        }

                        String transportista = request.get("transportista");

                        String numeroSeguimiento = request.get("numeroSeguimientoTransporte");

                        String urlSeguimiento = request.get("urlSeguimientoTransporte");

                        Venta ventaActualizada = ventaService.registrarDespacho(
                                        idVenta,
                                        transportista,
                                        numeroSeguimiento,
                                        urlSeguimiento);

                        return ResponseEntity.ok(ventaActualizada);

                } catch (IllegalArgumentException | IllegalStateException ex) {

                        return ResponseEntity.badRequest()
                                        .body(Map.of("error", ex.getMessage()));
                }
        }

        // ==========================================
        // ELIMINAR VENTA
        // ACCESO ADMINISTRATIVO ACTUAL
        // ==========================================

        @DeleteMapping("/{idVenta}")
        public ResponseEntity<Void> eliminarVenta(
                        @PathVariable("idVenta") Long idVenta) {

                ventaService.eliminarVenta(idVenta);

                return ResponseEntity.noContent().build();
        }

        // ==========================================
        // VERIFICAR PRODUCTO COMPRADO
        // ACCESO ADMINISTRATIVO ACTUAL
        // ==========================================

        @GetMapping("/{ventaId}/cliente/{clienteId}/producto/{productoId}/existe")
        public ResponseEntity<Boolean> clienteComproProductoEnVenta(
                        @PathVariable("ventaId") Long ventaId,
                        @PathVariable("clienteId") Long clienteId,
                        @PathVariable("productoId") Long productoId) {

                boolean compro = detalleVentaService.clienteComproProductoEnVenta(
                                clienteId,
                                ventaId,
                                productoId);

                return ResponseEntity.ok(compro);
        }
}
