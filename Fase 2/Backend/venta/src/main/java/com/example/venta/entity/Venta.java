package com.example.venta.entity;


import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ventas")
public class Venta {
     @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta")
    private Long idVenta;

    // Puede ser null cuando compra un invitado
    @Column(name = "cliente_id", nullable = true)
    private Long clienteId;

    @Column(name = "nombre_invitado")
    private String nombreInvitado;

    @Column(name = "apellido_invitado")
    private String apellidoInvitado;

    @Column(name = "correo_invitado")
    private String correoInvitado;

    @Column(name = "telefono_invitado")
    private String telefonoInvitado;

    @Column(name = "direccion_invitado")
    private String direccionInvitado;

    @Column(name = "fecha_venta", nullable = false)
    private LocalDateTime fechaVenta;

    @Column(name = "total_venta", nullable = false)
    private Double totalVenta;

}
