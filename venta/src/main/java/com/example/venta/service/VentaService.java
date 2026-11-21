
package com.example.venta.service;

import java.util.List;
import java.util.Optional;

import com.example.venta.dto.VentaRequest;
import com.example.venta.entity.DetalleVenta;
import com.example.venta.entity.EstadoPedido;
import com.example.venta.entity.Venta;
import com.example.venta.dto.SeguimientoPedidoResponse;

public interface VentaService {

        // ==========================================
        // GESTIÓN DE VENTAS
        // ==========================================

        Venta crearVenta(VentaRequest request);

        Optional<Venta> obtenerVentaPorId(Long idVenta);

        List<Venta> obtenerVentasPorCliente(Long clienteId);

        List<DetalleVenta> obtenerDetallesPorVenta(Long idVenta);

        List<Venta> listarTodas();

        Venta actualizarVenta(Long idVenta, VentaRequest request);

        void eliminarVenta(Long idVenta);

        double calcularTotal(VentaRequest request);

        // ==========================================
        // HU-32: SEGUIMIENTO DE PEDIDOS
        // ==========================================

        // Confirmar pago autorizado por Webpay.
        Venta marcarVentaPagada(Long idVenta);

        // Actualizar las etapas permitidas del pedido.
        Venta actualizarEstadoPedido(
                        Long idVenta,
                        EstadoPedido nuevoEstado);

        // Registrar el despacho y cambiar a ENVIADO
        // en una misma operación.
        Venta registrarDespacho(
                        Long idVenta,
                        String transportista,
                        String numeroSeguimientoTransporte,
                        String urlSeguimientoTransporte);

        // ==========================================
        // HU-32: CONSULTAR SEGUIMIENTO DEL DESPACHO
        // ==========================================
        // Obtiene información logística de una venta.
        // La autorización del comprador se verificará
        // en el controlador antes de llamar al servicio.

        SeguimientoPedidoResponse obtenerSeguimiento(
                        Long idVenta);

        // HU-33: Seguimiento de compras invitadas.
        SeguimientoPedidoResponse obtenerSeguimientoInvitado(
                        String numeroOrden,
                        String codigoSeguimiento);

}
