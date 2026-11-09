package com.example.venta.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductoResponse {

    private Long idProductos;
    private String nomProducto;
    private String descripcion;
    private Integer stockGramos;
    private Double precioKilo;
    private Double precio250g;

    public ProductoResponse() {
    }

    public Long getIdProductos() {
        return idProductos;
    }

    public void setIdProductos(Long idProductos) {
        this.idProductos = idProductos;
    }

    public String getNomProducto() {
        return nomProducto;
    }

    public void setNomProducto(String nomProducto) {
        this.nomProducto = nomProducto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getStockGramos() {
        return stockGramos;
    }

    public void setStockGramos(Integer stockGramos) {
        this.stockGramos = stockGramos;
    }

    public Double getPrecioKilo() {
        return precioKilo;
    }

    public void setPrecioKilo(Double precioKilo) {
        this.precioKilo = precioKilo;
    }

    public Double getPrecio250g() {
        return precio250g;
    }

    public void setPrecio250g(Double precio250g) {
        this.precio250g = precio250g;
    }
}