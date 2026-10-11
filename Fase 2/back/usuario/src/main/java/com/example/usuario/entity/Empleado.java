package com.example.usuario.entity;

import java.time.LocalDate;

import com.example.usuario.enums.PuestoEmpleado;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "empleados")
public class Empleado {
    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "usuario_id")  
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PuestoEmpleado puesto;

    @Column(name = "fecha_contrato", nullable = false)
    private LocalDate fechaContrato;

    @Column(nullable = false)
    private Double sueldo;
}
