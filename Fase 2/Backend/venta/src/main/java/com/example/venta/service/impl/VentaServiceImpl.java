package com.example.venta.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import com.example.venta.repository.VentaRepository;
import com.example.venta.service.VentaService;

@Service
@Transactional
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final ProductoClient productoClient;

    public VentaServiceImpl(
            VentaRepository ventaRepository,
            DetalleVentaRepository detalleVentaRepository,
            ProductoClient productoClient) {

        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.productoClient = productoClient;
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
}