package com.example.venta.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "compras_pendientes")
public class CompraPendiente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compra_pendiente")
    private Long idCompraPendiente;

    @Column(name = "token_ws", nullable = false, unique = true, length = 100)
    private String tokenWs;

    @Column(name = "buy_order", nullable = false, unique = true, length = 50)
    private String buyOrder;

    /*
     * Guardaremos aquí el VentaRequest completo en formato JSON:
     *
     * cliente/invitado
     * productos
     * cantidades
     * precios
     *
     * Después de AUTHORIZED lo reconstruiremos y crearemos
     * la Venta definitiva.
     */
    @Column(name = "venta_request_json", nullable = false, columnDefinition = "TEXT")
    private String ventaRequestJson;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_procesamiento")
    private LocalDateTime fechaProcesamiento;

    @Column(name = "venta_id")
    private Long ventaId;

    public CompraPendiente() {
    }

    public Long getIdCompraPendiente() {
        return idCompraPendiente;
    }

    public void setIdCompraPendiente(Long idCompraPendiente) {
        this.idCompraPendiente = idCompraPendiente;
    }

    public String getTokenWs() {
        return tokenWs;
    }

    public void setTokenWs(String tokenWs) {
        this.tokenWs = tokenWs;
    }

    public String getBuyOrder() {
        return buyOrder;
    }

    public void setBuyOrder(String buyOrder) {
        this.buyOrder = buyOrder;
    }

    public String getVentaRequestJson() {
        return ventaRequestJson;
    }

    public void setVentaRequestJson(String ventaRequestJson) {
        this.ventaRequestJson = ventaRequestJson;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaProcesamiento() {
        return fechaProcesamiento;
    }

    public void setFechaProcesamiento(LocalDateTime fechaProcesamiento) {
        this.fechaProcesamiento = fechaProcesamiento;
    }

    public Long getVentaId() {
        return ventaId;
    }

    public void setVentaId(Long ventaId) {
        this.ventaId = ventaId;
    }
}