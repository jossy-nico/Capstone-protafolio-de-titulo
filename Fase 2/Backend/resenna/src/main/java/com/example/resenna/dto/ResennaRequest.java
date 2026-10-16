package com.example.resenna.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResennaRequest {
    private Long clienteId;
    private Long ventaId;
    private Long productoId;
    private Integer estrellas;
    private String comentario;
}
