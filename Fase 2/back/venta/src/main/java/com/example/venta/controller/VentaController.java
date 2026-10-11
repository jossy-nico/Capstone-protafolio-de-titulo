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

import com.example.venta.dto.VentaRequest;
import com.example.venta.entity.DetalleVenta;
import com.example.venta.entity.Venta;
import com.example.venta.service.DetalleVentaService;
import com.example.venta.service.VentaService;

@RestController
@RequestMapping("/api/ventas")
@CrossOrigin(origins = "*")
public class VentaController {

    private final VentaService ventaService;
    private final DetalleVentaService detalleVentaService;

    public VentaController(
            VentaService ventaService,
            DetalleVentaService detalleVentaService
    ) {
        this.ventaService = ventaService;
        this.detalleVentaService = detalleVentaService;
    }

    @PostMapping("/crear")
    public ResponseEntity<Venta> crearVenta(
            @RequestBody VentaRequest request
    ) {
        Venta venta = ventaService.crearVenta(request);
        return ResponseEntity.ok(venta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Venta> obtenerVenta(
            @PathVariable("id") Long id
    ) {
        return ventaService.obtenerVentaPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<Venta>> obtenerVentasPorCliente(
            @PathVariable("clienteId") Long clienteId
    ) {
        return ResponseEntity.ok(
                ventaService.obtenerVentasPorCliente(clienteId)
        );
    }

    @GetMapping
    public ResponseEntity<List<Venta>> listarTodas() {
        return ResponseEntity.ok(
                ventaService.listarTodas()
        );
    }

    @GetMapping("/{idVenta}/detalles")
    public ResponseEntity<List<DetalleVenta>> obtenerDetalles(
            @PathVariable("idVenta") Long idVenta
    ) {
        return ResponseEntity.ok(
                ventaService.obtenerDetallesPorVenta(idVenta)
        );
    }

    @PutMapping("/{idVenta}")
    public ResponseEntity<Venta> actualizarVenta(
            @PathVariable("idVenta") Long idVenta,
            @RequestBody VentaRequest request
    ) {
        Venta ventaActualizada =
                ventaService.actualizarVenta(idVenta, request);

        return ResponseEntity.ok(ventaActualizada);
    }

    @DeleteMapping("/{idVenta}")
    public ResponseEntity<Void> eliminarVenta(
            @PathVariable("idVenta") Long idVenta
    ) {
        ventaService.eliminarVenta(idVenta);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(
        "/{ventaId}/cliente/{clienteId}/producto/{productoId}/existe"
    )
    public ResponseEntity<Boolean> clienteComproProductoEnVenta(
            @PathVariable("ventaId") Long ventaId,
            @PathVariable("clienteId") Long clienteId,
            @PathVariable("productoId") Long productoId
    ) {
        boolean compro =
                detalleVentaService.clienteComproProductoEnVenta(
                        clienteId,
                        ventaId,
                        productoId
                );

        return ResponseEntity.ok(compro);
    }
}