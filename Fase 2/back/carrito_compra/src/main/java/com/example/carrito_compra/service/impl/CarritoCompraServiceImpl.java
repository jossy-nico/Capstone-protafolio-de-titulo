package com.example.carrito_compra.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.carrito_compra.entity.CarritoCompra;
import com.example.carrito_compra.entity.CarritoCompraId;
import com.example.carrito_compra.repository.CarritoCompraRepository;
import com.example.carrito_compra.service.CarritoCompraService;

import jakarta.transaction.Transactional;


@Service
@Transactional
public class CarritoCompraServiceImpl implements CarritoCompraService{
    
    private final CarritoCompraRepository carritoCompraRepository;

    public CarritoCompraServiceImpl(CarritoCompraRepository carritoCompraRepository){
        this.carritoCompraRepository = carritoCompraRepository;
    }
    @Override
    public CarritoCompra agregarProducto(Long usuarioId, Integer productoId, Integer cantidad){
        if (usuarioId == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo");
        }
        if (productoId == null) {
            throw new IllegalArgumentException("El producto no puede ser nulo");
        }
        if (cantidad == null || cantidad <=0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
        }
        CarritoCompraId id = new CarritoCompraId(usuarioId, productoId);

        //si ya el producto en el carrito de eseusuario, sumamos la cantidad
        return carritoCompraRepository.findById(id)
        .map(existente ->{
            existente.setCantidad(existente.getCantidad()+ cantidad);
            return carritoCompraRepository.save(existente);
        })
        .orElseGet(() -> {
            CarritoCompra nuevoCarrito = new CarritoCompra();
            nuevoCarrito.setId(id);
            nuevoCarrito.setCantidad(cantidad);
            return carritoCompraRepository.save(nuevoCarrito);
        });
    }

    @Override
    @Transactional
    public List<CarritoCompra> obtenerCarritoPorUsuario(Long usuarioId){
        if (usuarioId == null) {
            throw new IllegalArgumentException("El usuarioId no puede ser nulo.");
        }
        return carritoCompraRepository.findByIdUsuarioId(usuarioId);
    }

    @Override
    public CarritoCompra actualizarCantidad(Long usuarioId, Integer productoId, Integer cantidad){
        if (usuarioId == null) {
            throw new IllegalArgumentException("El usuarioId no puede ser nulo.");
        }
        if (productoId == null) {
            throw new IllegalArgumentException("El productoId no puede ser nulo.");
        }
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
        }

        CarritoCompraId id = new CarritoCompraId(usuarioId, productoId);

        CarritoCompra existente = carritoCompraRepository.findById(id)
        .orElseThrow(()-> new IllegalArgumentException("No exise ese producto en el carrito del usuairo"));

        existente.setCantidad(cantidad);

        return carritoCompraRepository.save(existente);
    }

    @Override
    public void eliminarProducto(Long usuarioId, Integer productoId){
        if (usuarioId == null) {
            throw new IllegalArgumentException("El usuarioId no puede ser nulo.");
        }
        if (productoId == null) {
            throw new IllegalArgumentException("El productoId no puede ser nulo.");
        }

        CarritoCompraId id = new CarritoCompraId(usuarioId, productoId);

        if (!carritoCompraRepository.existsById(id)) {
            throw new IllegalArgumentException(
                    "No se puede eliminar: el producto no está en el carrito del usuario.");
        }
        carritoCompraRepository.deleteById(id);
    }

    @Override
    public void vaciarCarrito(Long usuarioId){
        if (usuarioId == null) {
            throw new IllegalArgumentException("El usuarioId no puede ser nulo.");
        }

        List<CarritoCompra> items = carritoCompraRepository.findByIdUsuarioId(usuarioId);

        if (items.isEmpty()) {
            return;
        }
        carritoCompraRepository.deleteAll(items);
    }
}
