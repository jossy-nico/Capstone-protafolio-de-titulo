package com.example.venta.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.venta.client.ProductoClient;
import com.example.venta.dto.DetalleVentaRequest;
import com.example.venta.dto.ProductoResponse;
import com.example.venta.dto.VentaRequest;
import com.example.venta.entity.DetalleVenta;
import com.example.venta.entity.DetalleVentaId;
import com.example.venta.entity.Venta;
import com.example.venta.repository.DetalleVentaRepository;
import com.example.venta.repository.PagoWebpayRepository;
import com.example.venta.repository.VentaRepository;
import com.example.venta.service.VentaService;
import com.example.venta.entity.EstadoPedido;
import com.example.venta.dto.SeguimientoPedidoResponse;
import com.example.venta.entity.PagoWebpay;

@Service
@Transactional
public class VentaServiceImpl implements VentaService {

        private final VentaRepository ventaRepository;
        private final DetalleVentaRepository detalleVentaRepository;
        private final ProductoClient productoClient;
        private final PagoWebpayRepository pagoWebpayRepository;

        // =========================================================
        // HU-32: CÓDIGOS PRIVADOS PARA COMPRADORES INVITADOS
        // =========================================================

        private static final SecureRandom SEGUIMIENTO_RANDOM = new SecureRandom();

        private String generarCodigoSeguimientoInvitado() {

                byte[] bytes = new byte[24];

                SEGUIMIENTO_RANDOM.nextBytes(bytes);

                return "NC-" + Base64.getUrlEncoder()
                                .withoutPadding()
                                .encodeToString(bytes);
        }

        public VentaServiceImpl(
                        VentaRepository ventaRepository,
                        DetalleVentaRepository detalleVentaRepository,
                        ProductoClient productoClient,
                        PagoWebpayRepository pagoWebpayRepository) {

                this.ventaRepository = ventaRepository;
                this.detalleVentaRepository = detalleVentaRepository;
                this.productoClient = productoClient;
                this.pagoWebpayRepository = pagoWebpayRepository;

        }

        // =========================================================
        // CALCULAR TOTAL
        // El precio SIEMPRE se obtiene desde producto-api.
        // Nunca se confía en el precio enviado por Android/React.
        // =========================================================

        @Override
        public double calcularTotal(VentaRequest request) {

                validarRequestYDetalles(request);

                double total = 0;

                for (DetalleVentaRequest detalle : request.getDetalles()) {

                        ProductoResponse producto = productoClient.obtenerProducto(
                                        detalle.getProductoId());

                        validarProducto(producto, detalle.getProductoId());

                        double precioVenta = obtenerPrecioVenta(
                                        producto,
                                        detalle.getCantidad());

                        total += precioVenta;
                }

                return total;
        }

        // =========================================================
        // CREAR VENTA
        // =========================================================

        @Override
        public Venta crearVenta(VentaRequest request) {

                if (request == null) {
                        throw new IllegalArgumentException(
                                        "La solicitud de venta no puede ser nula.");
                }

                boolean clienteRegistrado = request.getClienteId() != null;

                boolean datosInvitadoCompletos = request.getNombreInvitado() != null &&
                                !request.getNombreInvitado().isBlank() &&

                                request.getApellidoInvitado() != null &&
                                !request.getApellidoInvitado().isBlank() &&

                                request.getCorreoInvitado() != null &&
                                !request.getCorreoInvitado().isBlank() &&

                                request.getTelefonoInvitado() != null &&
                                !request.getTelefonoInvitado().isBlank() &&

                                request.getDireccionInvitado() != null &&
                                !request.getDireccionInvitado().isBlank();

                if (!clienteRegistrado &&
                                !datosInvitadoCompletos) {

                        throw new IllegalArgumentException(
                                        "La venta debe tener un cliente registrado "
                                                        + "o los datos completos del invitado.");
                }

                validarRequestYDetalles(request);

                List<DetalleVentaRequest> descontados = new ArrayList<>();

                try {

                        // -------------------------------------------------
                        // 1. OBTENER PRECIOS REALES Y DESCONTAR STOCK
                        // -------------------------------------------------

                        for (DetalleVentaRequest detalle : request.getDetalles()) {

                                ProductoResponse producto = productoClient.obtenerProducto(
                                                detalle.getProductoId());

                                validarProducto(
                                                producto,
                                                detalle.getProductoId());

                                /*
                                 * IMPORTANTE:
                                 * Sobrescribimos cualquier precio enviado
                                 * por Android/React.
                                 */
                                detalle.setPrecioUnitario(
                                                producto.getPrecioKilo());

                                productoClient.descontarStock(
                                                detalle.getProductoId().intValue(),
                                                detalle.getCantidad());

                                descontados.add(detalle);
                        }

                        // -------------------------------------------------
                        // 2. CALCULAR TOTAL CON PRECIOS DEL BACKEND
                        // -------------------------------------------------

                        double total = 0;

                        for (DetalleVentaRequest detalle : request.getDetalles()) {

                                ProductoResponse producto = productoClient.obtenerProducto(
                                                detalle.getProductoId());

                                total += obtenerPrecioVenta(
                                                producto,
                                                detalle.getCantidad());
                        }

                        // -------------------------------------------------
                        // 3. CREAR VENTA
                        // -------------------------------------------------

                        Venta venta = new Venta();

                        // ==========================================
                        // SEGUIMIENTO DE PEDIDOS - HU-32
                        // ==========================================

                        // Estado inicial mientras no exista
                        // confirmación del pago mediante Webpay.
                        venta.setEstadoPedido(EstadoPedido.PENDIENTE_PAGO);

                        // ==========================================
                        // CLIENTE
                        // ==========================================

                        venta.setClienteId(
                                        request.getClienteId());

                        if (request.getClienteId() == null) {

                                venta.setNombreInvitado(
                                                request.getNombreInvitado().trim());

                                venta.setApellidoInvitado(
                                                request.getApellidoInvitado().trim());

                                venta.setCorreoInvitado(
                                                request.getCorreoInvitado().trim());

                                venta.setTelefonoInvitado(
                                                request.getTelefonoInvitado().trim());

                                venta.setDireccionInvitado(
                                                request.getDireccionInvitado().trim());
                        }

                        venta.setFechaVenta(
                                        LocalDateTime.now());

                        venta.setTotalVenta(total);

                        Venta ventaGuardada = ventaRepository.save(venta);

                        // -------------------------------------------------
                        // 4. GUARDAR DETALLES
                        // -------------------------------------------------

                        for (DetalleVentaRequest detalle : request.getDetalles()) {

                                DetalleVentaId id = new DetalleVentaId(
                                                ventaGuardada.getIdVenta(),
                                                detalle.getProductoId());

                                DetalleVenta detalleVenta = new DetalleVenta();

                                detalleVenta.setId(id);

                                detalleVenta.setCantidad(
                                                detalle.getCantidad());

                                /*
                                 * Aquí queda guardado el precio REAL
                                 * obtenido desde producto-api.
                                 */
                                detalleVenta.setCantidad(
                                                detalle.getCantidad());

                                detalleVenta.setPrecioUnitario(
                                                detalle.getPrecioUnitario());

                                ProductoResponse producto = productoClient.obtenerProducto(
                                                detalle.getProductoId());

                                double subtotal = obtenerPrecioVenta(
                                                producto,
                                                detalle.getCantidad());

                                detalleVenta.setSubtotal(subtotal);

                                detalleVentaRepository.save(
                                                detalleVenta);
                        }

                        return ventaGuardada;

                } catch (Exception e) {

                        // -------------------------------------------------
                        // DEVOLVER STOCK SI ALGO FALLA
                        // -------------------------------------------------

                        for (DetalleVentaRequest detalle : descontados) {

                                try {

                                        productoClient.sumarStock(
                                                        detalle.getProductoId().intValue(),
                                                        detalle.getCantidad());

                                } catch (Exception ex) {

                                        ex.printStackTrace();
                                }
                        }

                        throw e;
                }
        }

        // =========================================================
        // OBTENER VENTA
        // =========================================================

        @Override
        public Optional<Venta> obtenerVentaPorId(
                        Long idVenta) {

                return ventaRepository.findById(idVenta);
        }

        // =========================================================
        // VENTAS POR CLIENTE
        // =========================================================

        @Override
        public List<Venta> obtenerVentasPorCliente(
                        Long clienteId) {

                return ventaRepository
                                .findByClienteId(clienteId);
        }

        // =========================================================
        // DETALLES DE UNA VENTA
        // =========================================================

        @Override
        public List<DetalleVenta> obtenerDetallesPorVenta(
                        Long idVenta) {

                return detalleVentaRepository
                                .findByIdVentaId(idVenta);
        }

        // =========================================================
        // LISTAR TODAS
        // =========================================================

        @Override
        public List<Venta> listarTodas() {

                return ventaRepository.findAll();
        }

        // =========================================================
        // ACTUALIZAR VENTA
        // =========================================================

        @Override
        public Venta actualizarVenta(
                        Long idVenta,
                        VentaRequest request) {

                if (idVenta == null) {
                        throw new IllegalArgumentException(
                                        "idVenta no puede ser nulo.");
                }

                if (request == null) {
                        throw new IllegalArgumentException(
                                        "El request no puede ser nulo.");
                }

                Venta venta = ventaRepository.findById(idVenta)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "La venta no existe."));

                // Actualización simple clienteId
                if (request.getClienteId() != null) {
                        venta.setClienteId(
                                        request.getClienteId());
                }

                /*
                 * Si no vienen detalles nuevos,
                 * no tocamos stock ni precios.
                 */
                if (request.getDetalles() == null ||
                                request.getDetalles().isEmpty()) {

                        return ventaRepository.save(venta);
                }

                List<DetalleVenta> detallesActuales = detalleVentaRepository
                                .findByIdVentaId(idVenta);

                // -------------------------------------------------
                // VALIDAR Y OBTENER PRECIOS REALES
                // -------------------------------------------------

                for (DetalleVentaRequest detalle : request.getDetalles()) {

                        validarDetalle(detalle);

                        ProductoResponse producto = productoClient.obtenerProducto(
                                        detalle.getProductoId());

                        validarProducto(
                                        producto,
                                        detalle.getProductoId());

                        /*
                         * Ignoramos precio recibido externamente.
                         */
                        detalle.setPrecioUnitario(
                                        producto.getPrecioKilo());
                }

                List<DetalleVenta> devueltos = new ArrayList<>();

                List<DetalleVentaRequest> descontadosNuevos = new ArrayList<>();

                try {

                        // -------------------------------------------------
                        // DEVOLVER STOCK ANTERIOR
                        // -------------------------------------------------

                        for (DetalleVenta detalle : detallesActuales) {

                                Long productoId = detalle.getId()
                                                .getProductoId();

                                if (productoId != null) {

                                        productoClient.sumarStock(
                                                        productoId.intValue(),
                                                        detalle.getCantidad());

                                        devueltos.add(detalle);
                                }
                        }

                        // -------------------------------------------------
                        // DESCONTAR NUEVO STOCK
                        // -------------------------------------------------

                        for (DetalleVentaRequest detalle : request.getDetalles()) {

                                productoClient.descontarStock(
                                                detalle.getProductoId().intValue(),
                                                detalle.getCantidad());

                                descontadosNuevos.add(detalle);
                        }

                        // -------------------------------------------------
                        // REEMPLAZAR DETALLES
                        // -------------------------------------------------

                        detalleVentaRepository
                                        .deleteAll(detallesActuales);

                        double totalNuevo = 0;

                        for (DetalleVentaRequest detalle : request.getDetalles()) {

                                DetalleVentaId id = new DetalleVentaId(
                                                idVenta,
                                                detalle.getProductoId());

                                DetalleVenta detalleNuevo = new DetalleVenta();

                                detalleNuevo.setId(id);

                                detalleNuevo.setCantidad(
                                                detalle.getCantidad());

                                detalleNuevo.setPrecioUnitario(
                                                detalle.getPrecioUnitario());

                                ProductoResponse producto = productoClient.obtenerProducto(
                                                detalle.getProductoId());

                                double subtotal = obtenerPrecioVenta(
                                                producto,
                                                detalle.getCantidad());

                                detalleNuevo.setSubtotal(subtotal);

                                detalleVentaRepository.save(
                                                detalleNuevo);

                                totalNuevo += subtotal;
                        }

                        venta.setTotalVenta(totalNuevo);

                        return ventaRepository.save(venta);

                } catch (Exception e) {

                        // -------------------------------------------------
                        // COMPENSACIÓN NUEVO STOCK
                        // -------------------------------------------------

                        for (DetalleVentaRequest detalle : descontadosNuevos) {

                                try {

                                        productoClient.sumarStock(
                                                        detalle.getProductoId().intValue(),
                                                        detalle.getCantidad());

                                } catch (Exception ignored) {
                                }
                        }

                        // -------------------------------------------------
                        // RESTAURAR STOCK ANTERIOR
                        // -------------------------------------------------

                        for (DetalleVenta detalle : devueltos) {

                                try {

                                        Long productoId = detalle.getId()
                                                        .getProductoId();

                                        if (productoId != null) {

                                                productoClient.descontarStock(
                                                                productoId.intValue(),
                                                                detalle.getCantidad());
                                        }

                                } catch (Exception ignored) {
                                }
                        }

                        throw e;
                }
        }

        // =========================================================
        // ELIMINAR VENTA
        // =========================================================

        @Override
        public void eliminarVenta(Long idVenta) {

                if (idVenta == null) {
                        throw new IllegalArgumentException(
                                        "idVenta no puede ser nulo.");
                }

                if (!ventaRepository.existsById(idVenta)) {
                        throw new IllegalArgumentException(
                                        "La venta no existe.");
                }

                List<DetalleVenta> detalles = detalleVentaRepository
                                .findByIdVentaId(idVenta);

                // Devolver stock
                for (DetalleVenta detalle : detalles) {

                        Long productoId = detalle.getId()
                                        .getProductoId();

                        if (productoId != null) {

                                productoClient.sumarStock(
                                                productoId.intValue(),
                                                detalle.getCantidad());
                        }
                }

                detalleVentaRepository
                                .deleteAll(detalles);

                ventaRepository
                                .deleteById(idVenta);
        }

        // =========================================================
        // VALIDACIONES PRIVADAS
        // =========================================================

        private void validarRequestYDetalles(
                        VentaRequest request) {

                if (request == null) {

                        throw new IllegalArgumentException(
                                        "La solicitud de venta no puede ser nula.");
                }

                if (request.getDetalles() == null ||
                                request.getDetalles().isEmpty()) {

                        throw new IllegalArgumentException(
                                        "La venta debe tener al menos un detalle.");
                }

                for (DetalleVentaRequest detalle : request.getDetalles()) {

                        validarDetalle(detalle);
                }
        }

        private void validarDetalle(
                        DetalleVentaRequest detalle) {

                if (detalle == null) {

                        throw new IllegalArgumentException(
                                        "El detalle de venta no puede ser nulo.");
                }

                if (detalle.getProductoId() == null) {

                        throw new IllegalArgumentException(
                                        "productoId no puede ser nulo.");
                }

                if (detalle.getCantidad() == null ||
                                detalle.getCantidad() <= 0) {

                        throw new IllegalArgumentException(
                                        "cantidad (gramos) debe ser mayor a 0.");
                }

                /*
                 * Ya NO validamos precioUnitario recibido.
                 *
                 * Puede venir modificado, nulo o incorrecto:
                 * el backend obtiene el precio real desde producto-api.
                 */
        }

        private void validarProducto(
                        ProductoResponse producto,
                        Long productoId) {

                if (producto == null) {

                        throw new IllegalArgumentException(
                                        "No se encontró el producto "
                                                        + productoId + ".");
                }

                if (producto.getPrecioKilo() == null ||
                                producto.getPrecioKilo() < 0) {

                        throw new IllegalArgumentException(
                                        "El producto "
                                                        + productoId
                                                        + " no tiene un precio válido.");
                }
        }

        private double obtenerPrecioVenta(
                        ProductoResponse producto,
                        Integer cantidadGramos) {

                if (cantidadGramos == 250) {

                        if (producto.getPrecio250g() == null ||
                                        producto.getPrecio250g() < 0) {

                                throw new IllegalArgumentException(
                                                "El producto no tiene un precio válido para 250 g.");
                        }

                        return producto.getPrecio250g();
                }

                if (producto.getPrecioKilo() == null ||
                                producto.getPrecioKilo() < 0) {

                        throw new IllegalArgumentException(
                                        "El producto no tiene un precio por kilo válido.");
                }

                /*
                 * Para otras cantidades:
                 * se calcula proporcionalmente y se redondea a pesos chilenos.
                 */
                return Math.round(
                                (cantidadGramos / 1000.0)
                                                * producto.getPrecioKilo());
        }

        // =========================================================
        // HU-32: CONFIRMAR PAGO Y GENERAR CÓDIGO DEL INVITADO
        // =========================================================

        @Override
        public Venta marcarVentaPagada(Long idVenta) {

                if (idVenta == null || idVenta <= 0L) {
                        throw new IllegalArgumentException(
                                        "El identificador de la venta no es válido.");
                }

                Venta venta = ventaRepository.findById(idVenta)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "No se encontró la venta con ID: " + idVenta));

                EstadoPedido estado = venta.getEstadoPedido();

                // No modificar ventas canceladas.
                if (estado == EstadoPedido.CANCELADO) {
                        throw new IllegalStateException(
                                        "No se puede confirmar el pago de una venta cancelada.");
                }

                // No retroceder un pedido que ya fue despachado
                // o se encuentra en preparación.
                if (estado == EstadoPedido.EN_PREPARACION
                                || estado == EstadoPedido.ENVIADO
                                || estado == EstadoPedido.ENTREGADO) {

                        throw new IllegalStateException(
                                        "La venta ya se encuentra en una etapa posterior al pago.");
                }

                // Si ya estaba pagada, conservar su estado,
                // fecha y código privado existente.
                // Permite completar compras invitadas antiguas
                // que aún no tengan código.
                if (estado == EstadoPedido.PAGADO) {

                        if (venta.getClienteId() == null
                                        && (venta.getCodigoSeguimiento() == null
                                                        || venta.getCodigoSeguimiento().isBlank())) {

                                venta.setCodigoSeguimiento(
                                                generarCodigoSeguimientoInvitado());

                                return ventaRepository.save(venta);
                        }

                        return venta;
                }

                // Solamente Webpay puede confirmar el pago.
                venta.setEstadoPedido(EstadoPedido.PAGADO);
                venta.setFechaPago(LocalDateTime.now());

                // Generar seguimiento exclusivo para invitados.
                if (venta.getClienteId() == null
                                && (venta.getCodigoSeguimiento() == null
                                                || venta.getCodigoSeguimiento().isBlank())) {

                        venta.setCodigoSeguimiento(
                                        generarCodigoSeguimientoInvitado());
                }

                return ventaRepository.save(venta);
        }

        // ==========================================
        // HU-32: ACTUALIZAR ESTADO DEL PEDIDO
        // ==========================================

        @Override
        public Venta actualizarEstadoPedido(
                        Long idVenta,
                        EstadoPedido nuevoEstado) {

                // 1. Validar datos recibidos.
                if (idVenta == null) {
                        throw new IllegalArgumentException(
                                        "El ID de la venta es obligatorio.");
                }

                if (nuevoEstado == null) {
                        throw new IllegalArgumentException(
                                        "El nuevo estado del pedido es obligatorio.");
                }

                // 2. Buscar la venta.
                Venta venta = ventaRepository.findById(idVenta)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "No se encontró la venta con ID: " + idVenta));

                EstadoPedido estadoActual = venta.getEstadoPedido();

                // 3. Evitar modificar ventas antiguas sin estado.
                if (estadoActual == null) {
                        throw new IllegalStateException(
                                        "La venta no tiene un estado inicial registrado.");
                }

                // 4. Si ya tiene ese estado, no modificar nada.
                if (estadoActual == nuevoEstado) {
                        return venta;
                }

                // 5. Bloquear estados finales.
                if (estadoActual == EstadoPedido.ENTREGADO
                                || estadoActual == EstadoPedido.CANCELADO) {
                        throw new IllegalStateException(
                                        "No se puede modificar un pedido finalizado o cancelado.");
                }

                // 6. Validar las transiciones permitidas.
                boolean transicionValida = switch (estadoActual) {

                        case PENDIENTE_PAGO ->
                                nuevoEstado == EstadoPedido.CANCELADO;

                        case PAGADO ->
                                nuevoEstado == EstadoPedido.EN_PREPARACION
                                                || nuevoEstado == EstadoPedido.CANCELADO;

                        case EN_PREPARACION ->
                                nuevoEstado == EstadoPedido.ENVIADO
                                                || nuevoEstado == EstadoPedido.CANCELADO;

                        case ENVIADO ->
                                nuevoEstado == EstadoPedido.ENTREGADO;

                        case ENTREGADO, CANCELADO -> false;
                };

                if (!transicionValida) {
                        throw new IllegalStateException(
                                        "Transición no permitida: "
                                                        + estadoActual
                                                        + " -> "
                                                        + nuevoEstado);
                }

                // 7. Registrar fecha del nuevo estado.
                LocalDateTime ahora = LocalDateTime.now();

                switch (nuevoEstado) {

                        case EN_PREPARACION ->
                                venta.setFechaPreparacion(ahora);

                        case ENVIADO ->
                                venta.setFechaEnvio(ahora);

                        case ENTREGADO ->
                                venta.setFechaEntrega(ahora);

                        case CANCELADO ->
                                venta.setFechaCancelacion(ahora);

                        case PENDIENTE_PAGO, PAGADO ->
                                throw new IllegalStateException(
                                                "El estado de pago solo puede ser "
                                                                + "gestionado mediante Webpay.");
                }

                // 8. Actualizar el estado.
                venta.setEstadoPedido(nuevoEstado);

                // 9. Guardar los cambios.
                return ventaRepository.save(venta);
        }

        // ==========================================
        // HU-32: REGISTRAR DESPACHO DEL PEDIDO
        // ==========================================

        @Override
        @Transactional
        public Venta registrarDespacho(
                        Long idVenta,
                        String transportista,
                        String numeroSeguimientoTransporte,
                        String urlSeguimientoTransporte) {

                // ==========================================
                // 1. VALIDAR IDENTIFICADOR DE LA VENTA
                // ==========================================

                if (idVenta == null || idVenta <= 0) {
                        throw new IllegalArgumentException(
                                        "El ID de la venta no es válido.");
                }

                // ==========================================
                // 2. VALIDAR TRANSPORTISTA
                // ==========================================

                if (transportista == null
                                || transportista.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Debe indicar el responsable del despacho.");
                }

                String transportistaLimpio = transportista.trim();

                if (transportistaLimpio.length() > 100) {
                        throw new IllegalArgumentException(
                                        "El transportista no puede superar 100 caracteres.");
                }

                String numeroLimpio = numeroSeguimientoTransporte == null
                                ? null
                                : numeroSeguimientoTransporte.trim();

                String urlLimpia = urlSeguimientoTransporte == null
                                ? null
                                : urlSeguimientoTransporte.trim();

                if (numeroLimpio != null && numeroLimpio.isBlank()) {
                        numeroLimpio = null;
                }

                if (urlLimpia != null && urlLimpia.isBlank()) {
                        urlLimpia = null;
                }

                // ==========================================
                // 3. VALIDAR LONGITUD DE LOS DATOS
                // ==========================================

                if (numeroLimpio != null
                                && numeroLimpio.length() > 150) {
                        throw new IllegalArgumentException(
                                        "El número de seguimiento supera 150 caracteres.");
                }

                if (urlLimpia != null
                                && urlLimpia.length() > 1000) {
                        throw new IllegalArgumentException(
                                        "La URL de seguimiento supera 1000 caracteres.");
                }

                // ==========================================
                // 4. VALIDAR TIPO DE DESPACHO
                // ==========================================

                boolean despachoPropio = "DESPACHO_PROPIO".equalsIgnoreCase(
                                transportistaLimpio);

                // Para transporte externo exigimos un número
                // de seguimiento. La URL es opcional porque
                // algunos transportistas no proporcionan un
                // enlace individual para cada pedido.

                if (!despachoPropio && numeroLimpio == null) {
                        throw new IllegalArgumentException(
                                        "El transporte externo requiere " +
                                                        "un número de seguimiento.");
                }

                // Validar que los enlaces externos utilicen HTTPS.
                if (urlLimpia != null) {
                        java.net.URI uri;

                        try {
                                uri = java.net.URI.create(urlLimpia);
                        } catch (IllegalArgumentException e) {
                                throw new IllegalArgumentException(
                                                "La URL de seguimiento no es válida.");
                        }

                        if (!"https".equalsIgnoreCase(uri.getScheme())
                                        || uri.getHost() == null
                                        || uri.getUserInfo() != null) {
                                throw new IllegalArgumentException(
                                                "La URL de seguimiento debe ser HTTPS válida.");
                        }
                }

                // ==========================================
                // 5. BUSCAR LA VENTA
                // ==========================================

                Venta venta = ventaRepository.findById(idVenta)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "No se encontró la venta con ID: " + idVenta));

                // ==========================================
                // 6. VALIDAR ESTADO ACTUAL
                // ==========================================

                if (venta.getEstadoPedido() != EstadoPedido.EN_PREPARACION) {
                        throw new IllegalStateException(
                                        "Solo se pueden despachar pedidos " +
                                                        "que estén EN_PREPARACION. Estado actual: " +
                                                        venta.getEstadoPedido());
                }

                // ==========================================
                // 7. REGISTRAR INFORMACIÓN DEL TRANSPORTE
                // ==========================================

                venta.setTransportista(transportistaLimpio);

                venta.setNumeroSeguimientoTransporte(
                                despachoPropio ? null : numeroLimpio);

                venta.setUrlSeguimientoTransporte(
                                despachoPropio ? null : urlLimpia);

                // ==========================================
                // 8. ACTUALIZAR ESTADO Y FECHA
                // ==========================================

                venta.setEstadoPedido(EstadoPedido.ENVIADO);
                venta.setFechaEnvio(LocalDateTime.now());

                // ==========================================
                // 9. GUARDAR CAMBIOS
                // ==========================================

                return ventaRepository.save(venta);
        }

        // =========================================================
        // HU-32: CONSULTAR SEGUIMIENTO DE COMPRA INVITADA
        // =========================================================
        //
        // Permite consultar el pedido mediante un código privado.
        // No requiere identificar al cliente mediante JWT.
        //
        // Reglas:
        // - El código debe tener el formato esperado.
        // - La venta debe pertenecer a un invitado.
        // - La venta debe tener el pago confirmado.
        // - No se entregan datos personales.
        // - No se modifica el estado del pedido.
        // =========================================================

        // =========================================================
        // HU-33: CONSULTA DE PEDIDOS PARA COMPRADORES INVITADOS
        // =========================================================
        //
        // Valida:
        // 1. Número de orden de Webpay.
        // 2. Código privado de seguimiento.
        // 3. Relación entre pago y venta.
        // 4. Pago autorizado.
        // 5. Compra realizada como invitado.
        //
        // La consulta es de solo lectura.
        // No modifica Webpay, ventas ni inventario.
        // =========================================================

        @Override
        @Transactional(readOnly = true)
        public SeguimientoPedidoResponse obtenerSeguimientoInvitado(
                        String numeroOrden,
                        String codigoSeguimiento) {

                final String mensajeError = "No se encontró un seguimiento válido para los datos ingresados.";

                // ==========================================
                // 1. VALIDAR DATOS
                // ==========================================

                if (numeroOrden == null
                                || numeroOrden.isBlank()
                                || numeroOrden.length() > 50
                                || codigoSeguimiento == null
                                || !codigoSeguimiento.matches(
                                                "NC-[A-Za-z0-9_-]{32}")) {

                        throw new IllegalArgumentException(mensajeError);
                }

                String ordenLimpia = numeroOrden.trim();

                // ==========================================
                // 2. BUSCAR PAGO POR NÚMERO DE ORDEN
                // ==========================================

                PagoWebpay pago = pagoWebpayRepository
                                .findByBuyOrder(ordenLimpia)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                mensajeError));

                // ==========================================
                // 3. VALIDAR PAGO AUTORIZADO
                // ==========================================

                if (!"AUTHORIZED".equalsIgnoreCase(
                                pago.getEstado())
                                || pago.getVentaId() == null) {

                        throw new IllegalArgumentException(mensajeError);
                }

                // ==========================================
                // 4. BUSCAR VENTA POR CÓDIGO PRIVADO
                // ==========================================

                Venta venta = ventaRepository
                                .findByCodigoSeguimiento(codigoSeguimiento)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                mensajeError));

                // ==========================================
                // 5. VERIFICAR RELACIÓN PAGO / VENTA
                // ==========================================

                if (!pago.getVentaId().equals(
                                venta.getIdVenta())) {

                        throw new IllegalArgumentException(mensajeError);
                }

                // ==========================================
                // 6. VALIDAR COMPRADOR INVITADO
                // ==========================================

                if (venta.getClienteId() != null) {

                        throw new IllegalArgumentException(mensajeError);
                }

                // ==========================================
                // 7. VALIDAR ESTADO DE COMPRA
                // ==========================================

                EstadoPedido estado = venta.getEstadoPedido();

                if (venta.getFechaPago() == null
                                || estado == null
                                || estado == EstadoPedido.PENDIENTE_PAGO) {

                        throw new IllegalArgumentException(mensajeError);
                }

                // ==========================================
                // 8. DEVOLVER SEGUIMIENTO
                // ==========================================
                //
                // Reutiliza el DTO existente.
                // No entrega nombre, correo, dirección
                // ni el código privado del comprador.
                // ==========================================

                return obtenerSeguimiento(
                                venta.getIdVenta());
        }

        // ==========================================
        // HU-32: SEGUIMIENTO DEL DESPACHO
        // ==========================================
        //
        // Devuelve únicamente información de
        // seguimiento del pedido.
        //
        // No modifica pagos, ventas ni inventario.
        //
        // No entrega el código privado del invitado.
        // ==========================================

        @Override
        @Transactional(readOnly = true)
        public SeguimientoPedidoResponse obtenerSeguimiento(
                        Long idVenta) {

                // 1. Validar identificador.

                if (idVenta == null || idVenta <= 0L) {
                        throw new IllegalArgumentException(
                                        "El identificador del pedido no es válido.");
                }

                // 2. Buscar pedido.

                Venta venta = ventaRepository.findById(idVenta)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "El pedido no fue encontrado."));

                // 3. Obtener estado actual.

                EstadoPedido estado = venta.getEstadoPedido();

                if (estado == null) {
                        throw new IllegalStateException(
                                        "El pedido no tiene un estado registrado.");
                }

                // 4. Preparar mensaje para Android y React.

                String mensajeEstado = switch (estado) {

                        case PENDIENTE_PAGO ->
                                "Estamos esperando la confirmación de tu pago.";

                        case PAGADO ->
                                "Tu pago fue confirmado. Pronto prepararemos tu pedido.";

                        case EN_PREPARACION ->
                                "Estamos preparando y embalando tus productos.";

                        case ENVIADO ->
                                "Tu pedido fue despachado y está en camino.";

                        case ENTREGADO ->
                                "Tu pedido fue entregado correctamente.";

                        case CANCELADO ->
                                "Tu pedido fue cancelado.";
                };

                // 5. Crear DTO de seguimiento.

                SeguimientoPedidoResponse respuesta = new SeguimientoPedidoResponse();

                respuesta.setVentaId(
                                venta.getIdVenta());

                respuesta.setEstadoActual(
                                estado.name());

                respuesta.setMensajeEstado(
                                mensajeEstado);

                // 6. Fechas del proceso.

                respuesta.setFechaPago(
                                venta.getFechaPago());

                respuesta.setFechaPreparacion(
                                venta.getFechaPreparacion());

                respuesta.setFechaEnvio(
                                venta.getFechaEnvio());

                respuesta.setFechaEntrega(
                                venta.getFechaEntrega());

                respuesta.setFechaCancelacion(
                                venta.getFechaCancelacion());

                // 7. Información del transporte.

                respuesta.setTransportista(
                                venta.getTransportista());

                respuesta.setNumeroSeguimientoTransporte(
                                venta.getNumeroSeguimientoTransporte());

                respuesta.setUrlSeguimientoTransporte(
                                venta.getUrlSeguimientoTransporte());

                // 8. Proteger el código privado del invitado.
                //
                // Este DTO también será utilizado por rutas
                // de consulta, por lo que nunca devolveremos
                // la credencial de seguimiento aquí.

                respuesta.setCodigoSeguimiento(null);

                return respuesta;
        }

}