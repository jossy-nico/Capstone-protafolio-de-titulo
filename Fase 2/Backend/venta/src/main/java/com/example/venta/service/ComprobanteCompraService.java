package com.example.venta.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.venta.client.ProductoClient;
import com.example.venta.client.UsuarioClient;
import com.example.venta.dto.ComprobanteCompraResponse;
import com.example.venta.dto.DetalleComprobanteResponse;
import com.example.venta.dto.ProductoResponse;
import com.example.venta.dto.UsuarioResponse;
import com.example.venta.entity.DetalleVenta;
import com.example.venta.entity.PagoWebpay;
import com.example.venta.entity.Venta;
import com.example.venta.repository.DetalleVentaRepository;
import com.example.venta.repository.PagoWebpayRepository;
import com.example.venta.repository.VentaRepository;

@Service
public class ComprobanteCompraService {

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final PagoWebpayRepository pagoWebpayRepository;
    private final ProductoClient productoClient;
    private final UsuarioClient usuarioClient;

    public ComprobanteCompraService(
            VentaRepository ventaRepository,
            DetalleVentaRepository detalleVentaRepository,
            PagoWebpayRepository pagoWebpayRepository,
            ProductoClient productoClient,
            UsuarioClient usuarioClient) {

        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.pagoWebpayRepository = pagoWebpayRepository;
        this.productoClient = productoClient;
        this.usuarioClient = usuarioClient;
    }

    public ComprobanteCompraResponse obtenerComprobante(
            Long ventaId) {

        if (ventaId == null) {
            throw new IllegalArgumentException(
                    "ventaId no puede ser nulo.");
        }

        // =====================================================
        // 1. OBTENER VENTA
        // =====================================================

        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La venta no existe."));

        // =====================================================
        // 2. VERIFICAR PAGO WEBPAY
        // =====================================================

        PagoWebpay pago = pagoWebpayRepository
                .findByVentaId(ventaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La venta no tiene un pago Webpay asociado."));

        if (!"AUTHORIZED".equalsIgnoreCase(pago.getEstado())
                || pago.getResponseCode() == null
                || pago.getResponseCode() != 0) {

            throw new IllegalStateException(
                    "No se puede generar un comprobante "
                            + "para una venta no autorizada.");
        }

        // =====================================================
        // 3. CREAR COMPROBANTE
        // =====================================================

        ComprobanteCompraResponse comprobante = new ComprobanteCompraResponse();

        comprobante.setVentaId(
                venta.getIdVenta());

        comprobante.setFechaVenta(
                venta.getFechaVenta());

        comprobante.setTotal(
                venta.getTotalVenta());

        // =====================================================
        // 4. DATOS DEL CLIENTE
        // =====================================================

        if (venta.getClienteId() != null) {

            UsuarioResponse usuario = usuarioClient.obtenerUsuario(
                    venta.getClienteId());

            comprobante.setClienteId(
                    usuario.getIdUsuario());

            comprobante.setNombreCliente(
                    usuario.getNombre());

            comprobante.setApellidoCliente(
                    usuario.getApellido());

            comprobante.setCorreoCliente(
                    usuario.getCorreo());

            /*
             * usuario-api actualmente no devuelve
             * teléfono ni dirección.
             */
            comprobante.setTelefonoCliente(null);
            comprobante.setDireccionCliente(null);

        } else {

            // Compra como invitado
            comprobante.setClienteId(null);

            comprobante.setNombreCliente(
                    venta.getNombreInvitado());

            comprobante.setApellidoCliente(
                    venta.getApellidoInvitado());

            comprobante.setCorreoCliente(
                    venta.getCorreoInvitado());

            comprobante.setTelefonoCliente(
                    venta.getTelefonoInvitado());

            comprobante.setDireccionCliente(
                    venta.getDireccionInvitado());
        }

        // =====================================================
        // 5. DETALLES DE LA COMPRA
        // =====================================================

        List<DetalleVenta> detallesVenta = detalleVentaRepository
                .findByIdVentaId(ventaId);

        List<DetalleComprobanteResponse> detalles = new ArrayList<>();

        for (DetalleVenta detalleVenta : detallesVenta) {

            Long productoId = detalleVenta.getId()
                    .getProductoId();

            ProductoResponse producto = productoClient.obtenerProducto(
                    productoId);

            double subtotal;

            if (detalleVenta.getSubtotal() != null) {

                subtotal = detalleVenta.getSubtotal();

            } else {

                // Compatibilidad con ventas antiguas
                subtotal = Math.round(
                        (detalleVenta.getCantidad() / 1000.0)
                                * detalleVenta.getPrecioUnitario());
            }

            DetalleComprobanteResponse detalle = new DetalleComprobanteResponse(
                    productoId,
                    producto.getNomProducto(),
                    detalleVenta.getCantidad(),
                    detalleVenta.getPrecioUnitario(),
                    subtotal);

            detalles.add(detalle);
        }

        comprobante.setDetalles(detalles);

        // =====================================================
        // 6. DATOS WEBPAY
        // =====================================================

        comprobante.setBuyOrder(
                pago.getBuyOrder());

        comprobante.setEstadoPago(
                pago.getEstado());

        comprobante.setAuthorizationCode(
                pago.getAuthorizationCode());

        comprobante.setPaymentTypeCode(
                pago.getPaymentTypeCode());

        comprobante.setInstallmentsNumber(
                pago.getInstallmentsNumber());

        comprobante.setCardLast4(
                pago.getCardLast4());

        comprobante.setFechaPago(
                pago.getFechaConfirmacion());

        return comprobante;
    }
}