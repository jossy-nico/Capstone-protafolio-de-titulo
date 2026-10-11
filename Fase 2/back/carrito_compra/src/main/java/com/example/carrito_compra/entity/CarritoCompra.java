package com.example.carrito_compra.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "carrito_compra")
public class CarritoCompra {
    @EmbeddedId
    private CarritoCompraId id;

    @Column(nullable = false)
    private Integer cantidad;

}
