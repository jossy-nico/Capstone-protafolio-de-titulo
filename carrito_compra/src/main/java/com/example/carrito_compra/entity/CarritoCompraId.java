package com.example.carrito_compra.entity;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarritoCompraId implements Serializable{
    private Long usuarioId;
    private Integer productoId;

    

}
