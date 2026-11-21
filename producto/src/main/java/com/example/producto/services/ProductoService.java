package com.example.producto.services;

import java.util.List;
import java.util.Optional;

import com.example.producto.entity.Producto;

public interface ProductoService {
    List<Producto> findAll();
    Optional<Producto> findById(Integer idProducto);
    Producto create(Producto p);
    Producto update(Integer idProducto, Producto p);
    void deleteById(Integer idProducto);
    List<Producto> findByCategoria(Integer idCategoria);

    //inventario
    void descuentoStock(Integer idProducto, Integer gramos);
    void sumarStock(Integer idProducto, Integer gramos);
}
