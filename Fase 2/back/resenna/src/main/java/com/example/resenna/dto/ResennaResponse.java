package com.example.resenna.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResennaResponse {
    private Long idResenna;
    private Long clienteId;
    private String nombreCliente;
    private Long ventaId;
    private Long productoId;
    private Integer estrellas;
    private String comentario;
    private String imagenUrl;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    

}

