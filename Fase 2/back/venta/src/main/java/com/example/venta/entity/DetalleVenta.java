package com.example.venta.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "detalle_ventas")
public class DetalleVenta {
    @EmbeddedId
    private DetalleVentaId id;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario_venta", nullable = false)
    private Double precioUnitario;
}
