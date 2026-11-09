package com.example.usuario.dto;

import java.time.LocalDate;

import com.example.usuario.enums.EstadoUsuario;
import com.example.usuario.enums.PuestoEmpleado;
import com.example.usuario.enums.TipoUsuario;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpleadoResponse {

    private Long idUsuario;

    private String nombre;
    private String apellido;
    private String correo;

    private TipoUsuario tipoUsuario;
    private EstadoUsuario estado;
    private Boolean debeCambiarPassword;

    private PuestoEmpleado puesto;
    private LocalDate fechaContrato;
    private Double sueldo;
}