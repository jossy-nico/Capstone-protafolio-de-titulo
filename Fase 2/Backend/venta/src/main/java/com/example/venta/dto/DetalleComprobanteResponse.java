package com.example.venta.dto;

public class DetalleComprobanteResponse {

    private Long productoId;
    private String nombreProducto;
    private Integer cantidadGramos;
    private Double precioKilo;
    private Double subtotal;

    public DetalleComprobanteResponse() {
    }

    public DetalleComprobanteResponse(
            Long productoId,
            String nombreProducto,
            Integer cantidadGramos,
            Double precioKilo,
            Double subtotal) {

        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.cantidadGramos = cantidadGramos;
        this.precioKilo = precioKilo;
        this.subtotal = subtotal;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public Integer getCantidadGramos() {
        return cantidadGramos;
    }

    public void setCantidadGramos(Integer cantidadGramos) {
        this.cantidadGramos = cantidadGramos;
    }

    public Double getPrecioKilo() {
        return precioKilo;
    }

    public void setPrecioKilo(Double precioKilo) {
        this.precioKilo = precioKilo;
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal;
    }
}