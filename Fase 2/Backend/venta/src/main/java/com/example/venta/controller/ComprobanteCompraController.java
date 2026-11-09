package com.example.venta.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.venta.dto.ComprobanteCompraResponse;
import com.example.venta.service.ComprobanteCompraService;
import com.example.venta.service.ComprobantePdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/ventas")
@CrossOrigin(origins = "*")
public class ComprobanteCompraController {

    private final ComprobanteCompraService comprobanteCompraService;
    private final ComprobantePdfService comprobantePdfService;

    public ComprobanteCompraController(
            ComprobanteCompraService comprobanteCompraService,
            ComprobantePdfService comprobantePdfService) {

        this.comprobanteCompraService = comprobanteCompraService;
        this.comprobantePdfService = comprobantePdfService;
    }

    @GetMapping("/{ventaId}/comprobante")
    public ResponseEntity<ComprobanteCompraResponse> obtenerComprobante(
            @PathVariable("ventaId") Long ventaId) {

        ComprobanteCompraResponse comprobante = comprobanteCompraService
                .obtenerComprobante(ventaId);

        return ResponseEntity.ok(comprobante);
    }

    @GetMapping(value = "/{ventaId}/comprobante/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarComprobantePdf(
            @PathVariable("ventaId") Long ventaId) {

        byte[] pdf = comprobantePdfService.generarPdf(ventaId);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"comprobante-venta-"
                                + ventaId
                                + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}