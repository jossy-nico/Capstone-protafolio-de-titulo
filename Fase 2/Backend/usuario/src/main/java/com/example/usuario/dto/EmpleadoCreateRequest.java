package com.example.usuario.dto;

import java.time.LocalDate;

import com.example.usuario.enums.PuestoEmpleado;

import lombok.Data;

@Data
public class EmpleadoCreateRequest {

    private String nombre;
    private String apellido;
    private String correo;
    private String password;

    private PuestoEmpleado puesto;
    private LocalDate fechaContrato;
    private Double sueldo;
}