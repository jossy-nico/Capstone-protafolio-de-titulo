package com.example.venta.service;

import java.util.List;

import com.example.venta.dto.DetalleVentaRequest;
import com.example.venta.entity.DetalleVenta;

public interface DetalleVentaService {
    DetalleVenta guardar(DetalleVenta detalle);

    List<DetalleVenta> obtenerDetallesPorVenta(Long idVenta);
    DetalleVenta obtenerDetallePorId(Long idVenta, Long productoId);
    DetalleVenta crearDetalle(DetalleVentaRequest request);
    DetalleVenta actualizarDetalle(Long idVenta, Long productoId, DetalleVentaRequest request);
    void eliminarDetalle(Long idVenta, Long productoId);
    boolean clienteComproProducto(Long clienteId, Long productoId);

    boolean clienteComproProductoEnVenta(
        Long clienteId,
        Long ventaId,
        Long productoId
);
}
