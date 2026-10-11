package com.example.carrito_compra.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.carrito_compra.entity.CarritoCompra;
import com.example.carrito_compra.entity.CarritoCompraId;

@Repository
public interface CarritoCompraRepository extends JpaRepository<CarritoCompra, CarritoCompraId>{
    //obtener todos los items del carrito de un usuario
    List<CarritoCompra> findByIdUsuarioId(Long usuarioId);

    //obtener un carrito de un carrito en específico
    CarritoCompra findByIdUsuarioIdAndIdProductoId(Long usuarioId, Integer productoId);

    //eliminar un producto del carrito de un usuario
    void deleteByIdUsuarioIdAndIdProductoId(Long usuarioId, Integer productoId);

    boolean existsByIdUsuarioIdAndIdProductoId(Long usuarioId, Integer productoId);

}
