package com.example.producto.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "productos")
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idProductos;

    @Column(nullable = false, length = 50)
    private String nomProducto;

    @Column(nullable = false, length = 200)
    private String descripcion;


    @Column(name = "stock_gramos")
    private Integer stockGramos;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "image_public_id", length = 255)
    private String imagePublicId;
    
    @ManyToOne
    @JoinColumn(name = "id_categoria")
    private Categoria categoria;

    @Column(name = "precio_kilo")
    private Double precioKilo;

    @Column(name = "precio_250g")
    private Double precio250g;

    @Column(name = "tabla_nutricional", length = 1000)
    private String tablaNutricional;

    @Column(name = "ingredientes", length = 1000)
    private String ingredientes;

    @Column(name = "alergenos", length = 1000)
    private String alergenos;

}
