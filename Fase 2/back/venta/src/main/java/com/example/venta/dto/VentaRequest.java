package com.example.venta.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VentaRequest {

    private Long clienteId;

    private String nombreInvitado;
    private String apellidoInvitado;
    private String correoInvitado;
    private String telefonoInvitado;
    private String direccionInvitado;

    private List<DetalleVentaRequest> detalles;
}
