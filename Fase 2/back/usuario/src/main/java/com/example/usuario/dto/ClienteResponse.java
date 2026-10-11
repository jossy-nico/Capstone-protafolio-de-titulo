package com.example.usuario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponse {

    private Long usuarioId;
    private String nombre;
    private String apellido;
    private String correo;
    private String telefono;
    private String direccion;
}
