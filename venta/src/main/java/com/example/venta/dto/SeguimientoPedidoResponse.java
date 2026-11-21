
package com.example.venta.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeguimientoPedidoResponse {

    // Identificación del pedido
    private Long ventaId;

    // Estado actual del despacho
    private String estadoActual;
    private String mensajeEstado;

    // Fechas de avance
    private LocalDateTime fechaPago;
    private LocalDateTime fechaPreparacion;
    private LocalDateTime fechaEnvio;
    private LocalDateTime fechaEntrega;
    private LocalDateTime fechaCancelacion;

    // Información logística
    private String transportista;
    private String numeroSeguimientoTransporte;
    private String urlSeguimientoTransporte;

    // Se utilizará únicamente cuando
    // corresponda entregar una credencial
    // privada al comprador invitado.
    private String codigoSeguimiento;
}
