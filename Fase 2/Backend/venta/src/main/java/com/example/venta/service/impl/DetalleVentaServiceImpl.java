package com.example.venta.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.venta.dto.DetalleVentaRequest;
import com.example.venta.entity.DetalleVenta;
import com.example.venta.entity.DetalleVentaId;
import com.example.venta.repository.DetalleVentaRepository;
import com.example.venta.service.DetalleVentaService;

@Service
@Transactional
public class DetalleVentaServiceImpl implements DetalleVentaService {

    private final DetalleVentaRepository detalleVentaRepository;

    public DetalleVentaServiceImpl(DetalleVentaRepository detalleVentaRepository){
        this.detalleVentaRepository = detalleVentaRepository;
    }

    @Override
    public DetalleVenta guardar(DetalleVenta detalle){
        if (detalle == null) {
            throw new IllegalArgumentException("El detalle de venta no puede ser nulo.");
        }
        if (detalle.getId() == null ||
            detalle.getId().getVentaId() == null ||
            detalle.getId().getProductoId() == null) {
            throw new IllegalArgumentException("El ID embebido (ventaId, productoId) no puede ser nulo.");
        }
        if (detalle.getCantidad() == null || detalle.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
        }
        if (detalle.getPrecioUnitario() == null || detalle.getPrecioUnitario() < 0) {
            throw new IllegalArgumentException("El precio unitario no puede ser negativo.");
        }

        return detalleVentaRepository.save(detalle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DetalleVenta> obtenerDetallesPorVenta(Long idVenta){
        if (idVenta == null) {
            throw new IllegalArgumentException("El idVenta no puede ser nulo.");
        }
        return detalleVentaRepository.findByIdVentaId(idVenta);
    }

    @Override
    @Transactional(readOnly = true)
    public DetalleVenta obtenerDetallePorId(Long idVenta, Long productoId){
        if (idVenta == null || productoId == null) {
            throw new IllegalArgumentException("idVenta y productoId no pueden ser nulos.");
        }

        DetalleVentaId id = new DetalleVentaId(idVenta, productoId);

        return detalleVentaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Detalle de venta no encontrado."));
    }

    @Override
    public DetalleVenta crearDetalle(DetalleVentaRequest request){

        if (request == null) {
            throw new IllegalArgumentException("El request no puede ser nulo.");
        }
        if (request.getVentaId() == null) {
            throw new IllegalArgumentException("ventaId no puede ser nulo.");
        }
        if (request.getProductoId() == null) {
            throw new IllegalArgumentException("productoId no puede ser nulo.");
        }
        if (request.getCantidad() == null || request.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
        }
        if (request.getPrecioUnitario() == null || request.getPrecioUnitario() < 0) {
            throw new IllegalArgumentException("El precio unitario no puede ser negativo.");
        }

        DetalleVentaId id = new DetalleVentaId(request.getVentaId(), request.getProductoId());

        DetalleVenta detalle = new DetalleVenta();
        detalle.setId(id);
        detalle.setCantidad(request.getCantidad());
        detalle.setPrecioUnitario(request.getPrecioUnitario());

        return detalleVentaRepository.save(detalle);
    }

    @Override
    public DetalleVenta actualizarDetalle(Long idVenta, Long productoId, DetalleVentaRequest request){

        if (idVenta == null || productoId == null) {
            throw new IllegalArgumentException("idVenta y productoId no pueden ser nulos.");
        }
        if (request == null) {
            throw new IllegalArgumentException("El request no puede ser nulo.");
        }

        DetalleVentaId id = new DetalleVentaId(idVenta, productoId);

        DetalleVenta detalle = detalleVentaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("No existe ese detalle de venta."));

        if (request.getCantidad() != null) {
            if (request.getCantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
            }
            detalle.setCantidad(request.getCantidad());
        }

        if (request.getPrecioUnitario() != null) {
            if (request.getPrecioUnitario() < 0) {
                throw new IllegalArgumentException("El precio unitario no puede ser negativo.");
            }
            detalle.setPrecioUnitario(request.getPrecioUnitario());
        }

        return detalleVentaRepository.save(detalle);
    }

    @Override
    public void eliminarDetalle(Long idVenta, Long productoId){

        if (idVenta == null || productoId == null) {
            throw new IllegalArgumentException("idVenta y productoId no pueden ser nulos.");
        }

        DetalleVentaId id = new DetalleVentaId(idVenta, productoId);

        if (!detalleVentaRepository.existsById(id)) {
            throw new IllegalArgumentException("No existe el detalle a eliminar.");
        }

        detalleVentaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean clienteComproProducto(Long clienteId, Long productoId){
        if (clienteId == null || productoId == null) {
            throw new IllegalArgumentException(
                "clienteId y productoId no pueden ser nulos."
            );
            
        }
        Long cantidadCompras =
        detalleVentaRepository.contarComprasProductoPorCliente(clienteId, productoId);

        return cantidadCompras > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean clienteComproProductoEnVenta(
            Long clienteId,
            Long ventaId,
            Long productoId
    ) {

        if (clienteId == null) {
            throw new IllegalArgumentException(
                    "El clienteId no puede ser nulo."
            );
        }

        if (ventaId == null) {
            throw new IllegalArgumentException(
                    "El ventaId no puede ser nulo."
            );
        }

        if (productoId == null) {
            throw new IllegalArgumentException(
                    "El productoId no puede ser nulo."
            );
        }

        long cantidad =
                detalleVentaRepository.contarProductoCompradoEnVenta(
                        clienteId,
                        ventaId,
                        productoId
                );

        return cantidad > 0;
    }

}
