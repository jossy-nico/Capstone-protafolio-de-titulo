package com.example.venta.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetalleVentaRequest {
    private Long productoId;
    private Integer cantidad;
    private Double precioUnitario;
    private Long ventaId;
}
