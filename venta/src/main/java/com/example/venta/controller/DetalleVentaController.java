package com.example.venta.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.venta.dto.DetalleVentaRequest;
import com.example.venta.entity.DetalleVenta;
import com.example.venta.service.DetalleVentaService;

@RestController
@RequestMapping("/api/detalle-ventas")
@CrossOrigin(origins = "*")
public class DetalleVentaController {

    private final DetalleVentaService detalleVentaService;

    public DetalleVentaController(DetalleVentaService detalleVentaService) {
        this.detalleVentaService = detalleVentaService;
    }
    // Obtener todos los detalles de una venta
    @GetMapping("/venta/{idVenta}")
    public ResponseEntity<List<DetalleVenta>> obtenerDetallesPorVenta(@PathVariable("idVenta") Long idVenta) {
        return ResponseEntity.ok(detalleVentaService.obtenerDetallesPorVenta(idVenta));
    }

    // Obtener un detalle específico
    @GetMapping("/{idVenta}/{productoId}")
    public ResponseEntity<DetalleVenta> obtenerDetalle(
            @PathVariable("idVenta") Long idVenta,
            @PathVariable("productoId") Long productoId
    ) {
        DetalleVenta detalle = detalleVentaService.obtenerDetallePorId(idVenta, productoId);
        return ResponseEntity.ok(detalle);
    }

    // Crear un detalle manualmente (opcional)
    @PostMapping
    public ResponseEntity<DetalleVenta> crearDetalle(@RequestBody DetalleVentaRequest request) {

        DetalleVenta detalle = detalleVentaService.crearDetalle(request);
        return ResponseEntity.ok(detalle);
    }

    // Actualizar un detalle existente
    @PutMapping("/{idVenta}/{productoId}")
    public ResponseEntity<DetalleVenta> actualizarDetalle(
            @PathVariable("idVenta") Long idVenta,
            @PathVariable("productoId") Long productoId,
            @RequestBody DetalleVentaRequest request
    ) {
        DetalleVenta detalleActualizado =
                detalleVentaService.actualizarDetalle(idVenta, productoId, request);

        return ResponseEntity.ok(detalleActualizado);
    }

    // Eliminar detalle
    @DeleteMapping("/{idVenta}/{productoId}")
    public ResponseEntity<Void> eliminarDetalle(
            @PathVariable("idVenta") Long idVenta,
            @PathVariable("productoId") Long productoId
    ) {
        detalleVentaService.eliminarDetalle(idVenta, productoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cliente/{clienteId}/producto/{productoId}/compro")
    public ResponseEntity<Boolean> clienteComproProducto(
        @PathVariable("clienteId") Long clienteId,
        @PathVariable("productoId") Long productoId
    ){
        boolean compro = detalleVentaService.clienteComproProducto(clienteId, productoId);

        return ResponseEntity.ok(compro);
    }
}
