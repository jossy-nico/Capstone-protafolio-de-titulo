package com.example.resenna.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "resennas",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_resenna_cliente_venta_producto",
            columnNames = {
                "cliente_id",
                "venta_id",
                "producto_id"
            }
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Resenna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resenna")
    private Long idResenna;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "venta_id", nullable = false)
    private Long ventaId;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(nullable = false)
    private Integer estrellas;

    @Column(length = 1000)
    private String comentario;

    @Column(name = "imagen_url", length = 1000)
    private String imagenUrl;

    @Column(name = "image_public_id", length = 255)
    private String imagePublicId;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}