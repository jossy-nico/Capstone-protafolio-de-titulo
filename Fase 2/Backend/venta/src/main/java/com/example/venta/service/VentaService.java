package com.example.venta.service;

import java.util.List;
import java.util.Optional;

import com.example.venta.dto.VentaRequest;
import com.example.venta.entity.DetalleVenta;
import com.example.venta.entity.Venta;

public interface VentaService {
    Venta crearVenta(VentaRequest request);
    Optional<Venta> obtenerVentaPorId(Long idVenta);
    List<Venta> obtenerVentasPorCliente(Long clienteId);
    List<DetalleVenta> obtenerDetallesPorVenta(Long idVenta);
    List<Venta> listarTodas();
    Venta actualizarVenta(Long idVenta, VentaRequest request);
    void eliminarVenta(Long idVenta);
}
