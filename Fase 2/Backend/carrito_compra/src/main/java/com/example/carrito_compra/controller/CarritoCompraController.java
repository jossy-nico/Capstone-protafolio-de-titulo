package com.example.carrito_compra.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.carrito_compra.entity.CarritoCompra;
import com.example.carrito_compra.service.CarritoCompraService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/carrito")
public class CarritoCompraController {

    private final CarritoCompraService carritoCompraService;

    public CarritoCompraController(CarritoCompraService carritoCompraService) {
        this.carritoCompraService = carritoCompraService;
    }

    // obtener todos los productos del carrito
    @GetMapping("/{usuarioId}")
    public ResponseEntity<List<CarritoCompra>> obtenerCarrito(@PathVariable("usuarioId") Long usuarioId) {
        List<CarritoCompra> carrito = carritoCompraService.obtenerCarritoPorUsuario(usuarioId);
        return ResponseEntity.ok(carrito);
    }

    // agregar carrito
    @PostMapping("/agregar")
    public ResponseEntity<CarritoCompra> agregarProducto(@RequestParam(name = "usuarioId") Long usuarioId,
            @RequestParam(name = "productoId") Integer productoId, @RequestParam(name = "cantidad") Integer cantidad) {

        CarritoCompra item = carritoCompraService.agregarProducto(usuarioId, productoId, cantidad);
        return ResponseEntity.ok(item);
    }

    // Actualizar la cantidad de un producto en el carrito
    @PutMapping("/actualizar")
    public ResponseEntity<CarritoCompra> actualizarCantidad(@RequestParam(name = "usuarioId") Long usuarioId,
            @RequestParam(name = "productoId") Integer productoId, @RequestParam(name = "cantidad") Integer cantidad) {

        CarritoCompra itemActualizado = carritoCompraService.actualizarCantidad(usuarioId, productoId, cantidad);
        return ResponseEntity.ok(itemActualizado);
    }

    // Eliminar un productos del carrito de un usuario
    @DeleteMapping("/eliminar")
    public ResponseEntity<Void> eliminarProducto(@RequestParam(name = "usuarioId") Long usuarioId,
            @RequestParam(name = "productoId") Integer productoId) {

        carritoCompraService.eliminarProducto(usuarioId, productoId);
        return ResponseEntity.noContent().build();
    }

    // vaciar carrito de un usuario
    @DeleteMapping("/vaciar/{usuarioId}")
    public ResponseEntity<Void> vaciarCarrito(@PathVariable("usuarioId") Long usuarioId) {

        carritoCompraService.vaciarCarrito(usuarioId);
        return ResponseEntity.noContent().build();
    }

}
