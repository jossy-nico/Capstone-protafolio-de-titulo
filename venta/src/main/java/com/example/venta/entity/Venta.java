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

    // ==========================================
    // CLIENTE REGISTRADO
    // ==========================================

    // Puede ser null cuando compra un invitado
    @Column(name = "cliente_id", nullable = true)
    private Long clienteId;

    // ==========================================
    // DATOS DEL CLIENTE INVITADO
    // ==========================================

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

    // ==========================================
    // INFORMACIÓN DE LA VENTA
    // ==========================================

    @Column(name = "fecha_venta", nullable = false)
    private LocalDateTime fechaVenta;

    @Column(name = "total_venta", nullable = false)
    private Double totalVenta;

    // ==========================================
    // SEGUIMIENTO DEL PEDIDO - HU-32
    // ==========================================

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pedido", length = 30)
    private EstadoPedido estadoPedido = EstadoPedido.PENDIENTE_PAGO;

    // Código privado para consultar un pedido
    // realizado como invitado.
    // Debe generarse de manera segura en el backend.
    @Column(name = "codigo_seguimiento", length = 128, unique = true)
    private String codigoSeguimiento;

    // ==========================================
    // INFORMACIÓN DEL DESPACHO
    // ==========================================

    @Column(name = "transportista", length = 100)
    private String transportista;

    @Column(name = "numero_seguimiento_transporte", length = 150)
    private String numeroSeguimientoTransporte;

    @Column(name = "url_seguimiento_transporte", length = 1000)
    private String urlSeguimientoTransporte;

    // ==========================================
    // FECHAS DEL PROCESO
    // ==========================================

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "fecha_preparacion")
    private LocalDateTime fechaPreparacion;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;
}
