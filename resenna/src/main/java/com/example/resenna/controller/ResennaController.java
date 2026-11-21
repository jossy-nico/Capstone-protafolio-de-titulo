package com.example.resenna.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
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

import com.example.resenna.dto.ResennaRequest;
import com.example.resenna.dto.ResennaResponse;
import com.example.resenna.service.ResennaService;

@RestController
@RequestMapping("/api/resennas")
@CrossOrigin(origins = "*")
public class ResennaController {

    private final ResennaService resennaService;

    public ResennaController(ResennaService resennaService) {
        this.resennaService = resennaService;
    }

    @PostMapping
    public ResponseEntity<ResennaResponse> crearResenna(
            @RequestBody ResennaRequest request
    ) {

        ResennaResponse response =
                resennaService.crearResenna(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/producto/{productoId}")
    public ResponseEntity<List<ResennaResponse>> obtenerResennasProducto(
            @PathVariable("productoId") Long productoId
    ) {

        return ResponseEntity.ok(
                resennaService.obtenerResennasProducto(productoId)
        );
    }

    @GetMapping(
        "/cliente/{clienteId}/venta/{ventaId}/producto/{productoId}"
    )
    public ResponseEntity<ResennaResponse> obtenerResennaCompra(
            @PathVariable("clienteId") Long clienteId,
            @PathVariable("ventaId") Long ventaId,
            @PathVariable("productoId") Long productoId
    ) {

        return ResponseEntity.ok(
                resennaService.obtenerResennaCompra(
                        clienteId,
                        ventaId,
                        productoId
                )
        );
    }

    @GetMapping("/producto/{productoId}/promedio")
    public ResponseEntity<Double> obtenerPromedioProducto(
            @PathVariable("productoId") Long productoId
    ) {

        return ResponseEntity.ok(
                resennaService.obtenerPromedioProducto(productoId)
        );
    }

    @PutMapping("/{idResenna}")
    public ResponseEntity<ResennaResponse> actualizarResenna(
            @PathVariable("idResenna") Long idResenna,
            @RequestBody ResennaRequest request
    ) {

        return ResponseEntity.ok(
                resennaService.actualizarResena(
                        idResenna,
                        request
                )
        );
    }

    @DeleteMapping("/{idResenna}")
    public ResponseEntity<Void> eliminarResenna(
            @PathVariable("idResenna") Long idResenna
    ) {

        resennaService.eliminarResena(idResenna);

        return ResponseEntity.noContent().build();
    }
}

