package com.example.carrito_compra.service;

import java.util.List;

import com.example.carrito_compra.entity.CarritoCompra;

public interface CarritoCompraService {
    CarritoCompra agregarProducto(Long usuarioId, Integer productoId, Integer cantidad);
    List<CarritoCompra> obtenerCarritoPorUsuario(Long usuarioId);
    CarritoCompra actualizarCantidad(Long usuarioId, Integer productoId, Integer cantidad);
    void eliminarProducto(Long usuarioId, Integer productoId);
    void vaciarCarrito(Long usuarioId);

}

