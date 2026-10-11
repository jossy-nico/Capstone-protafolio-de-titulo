package com.example.venta.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.venta.client.ProductoClient;
import com.example.venta.dto.DetalleVentaRequest;
import com.example.venta.dto.VentaRequest;
import com.example.venta.entity.DetalleVenta;
import com.example.venta.entity.DetalleVentaId;
import com.example.venta.entity.Venta;
import com.example.venta.repository.DetalleVentaRepository;
import com.example.venta.repository.VentaRepository;
import com.example.venta.service.VentaService;


@Service
@Transactional
public class VentaServiceImpl implements VentaService{

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;

    private final ProductoClient productoClient;

    public VentaServiceImpl(VentaRepository ventaRepository,
                            DetalleVentaRepository detalleVentaRepository,
                            ProductoClient productoClient) {
        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.productoClient = productoClient;
    }
    
    @Override
    public Venta crearVenta(VentaRequest request) {

        boolean clienteRegistrado = request.getClienteId() != null;

        boolean datosInvitadoCompletos =
                request.getNombreInvitado() != null &&
                !request.getNombreInvitado().isBlank() &&
                request.getApellidoInvitado() != null &&
                !request.getApellidoInvitado().isBlank() &&
                request.getCorreoInvitado() != null &&
                !request.getCorreoInvitado().isBlank() &&
                request.getTelefonoInvitado() != null &&
                !request.getTelefonoInvitado().isBlank() &&
                request.getDireccionInvitado() != null &&
                !request.getDireccionInvitado().isBlank();

        if (!clienteRegistrado && !datosInvitadoCompletos) {
            throw new IllegalArgumentException(
                    "La venta debe tener un cliente registrado o los datos completos del invitado."
            );
        }

        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new IllegalArgumentException(
                    "La venta debe tener al menos un detalle."
            );
        }

        List<DetalleVentaRequest> descontados =
                new java.util.ArrayList<>();

        try {

            // 1. VALIDAR Y DESCONTAR STOCK
            for (DetalleVentaRequest d : request.getDetalles()) {

                if (d.getProductoId() == null) {
                    throw new IllegalArgumentException(
                            "productoId no puede ser nulo."
                    );
                }

                if (d.getCantidad() == null || d.getCantidad() <= 0) {
                    throw new IllegalArgumentException(
                            "cantidad (gramos) debe ser mayor a 0."
                    );
                }

                if (d.getPrecioUnitario() == null ||
                        d.getPrecioUnitario() < 0) {
                    throw new IllegalArgumentException(
                            "precioUnitario no puede ser negativo."
                    );
                }

                productoClient.descontarStock(
                        d.getProductoId().intValue(),
                        d.getCantidad()
                );

                descontados.add(d);
            }

            // 2. CALCULAR TOTAL
            double total = request.getDetalles()
                    .stream()
                    .mapToDouble(
                            d -> (d.getCantidad() / 1000.0) *
                                    d.getPrecioUnitario()
                    )
                    .sum();

            // 3. CREAR VENTA
            Venta venta = new Venta();

            venta.setClienteId(request.getClienteId());

            if (request.getClienteId() == null) {
                venta.setNombreInvitado(
                        request.getNombreInvitado().trim()
                );
                venta.setApellidoInvitado(
                        request.getApellidoInvitado().trim()
                );
                venta.setCorreoInvitado(
                        request.getCorreoInvitado().trim()
                );
                venta.setTelefonoInvitado(
                        request.getTelefonoInvitado().trim()
                );
                venta.setDireccionInvitado(
                        request.getDireccionInvitado().trim()
                );
            }

            venta.setFechaVenta(LocalDateTime.now());
            venta.setTotalVenta(total);

            Venta guardarVenta = ventaRepository.save(venta);

            // 4. GUARDAR DETALLES
            for (DetalleVentaRequest d : request.getDetalles()) {

                DetalleVentaId id = new DetalleVentaId(
                        guardarVenta.getIdVenta(),
                        d.getProductoId()
                );

                DetalleVenta detalle = new DetalleVenta();
                detalle.setId(id);
                detalle.setCantidad(d.getCantidad());
                detalle.setPrecioUnitario(d.getPrecioUnitario());

                detalleVentaRepository.save(detalle);
            }

            return guardarVenta;

        } catch (Exception e) {

            // DEVOLVER STOCK SI ALGO FALLA
            for (DetalleVentaRequest d : descontados) {
                try {
                    productoClient.sumarStock(
                            d.getProductoId().intValue(),
                            d.getCantidad()
                    );
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            throw e;
        }
    }


    @Override
    public Optional<Venta> obtenerVentaPorId(Long idVenta){
        return ventaRepository.findById(idVenta);
    }

    @Override
    public List<Venta> obtenerVentasPorCliente(Long clienteId){
        return ventaRepository.findByClienteId(clienteId);
    }

    @Override
    public List<DetalleVenta> obtenerDetallesPorVenta(Long idVenta){
        return detalleVentaRepository.findByIdVentaId(idVenta);
    }

    @Override
    public List<Venta> listarTodas(){
        return ventaRepository.findAll();
    }

    //actualizar venta
    @Override
    public Venta actualizarVenta(Long idVenta, VentaRequest request) {

        if (idVenta == null) {
            throw new IllegalArgumentException("idVenta no puede ser nulo.");
        }
        if (request == null) {
            throw new IllegalArgumentException("El request no puede ser nulo.");
        }

        Venta venta = ventaRepository.findById(idVenta)
                .orElseThrow(() -> new IllegalArgumentException("La venta no existe."));

        // 1) Actualización simple del clienteId (no afecta stock)
        if (request.getClienteId() != null) {
            venta.setClienteId(request.getClienteId());
        }

        // Si NO vienen detalles nuevos, solo guarda cambios de clienteId
        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            return ventaRepository.save(venta);
        }

        // 2) Traer detalles actuales
        List<DetalleVenta> detallesActuales = detalleVentaRepository.findByIdVentaId(idVenta);

        // 3) Validar detalles nuevos
        for (DetalleVentaRequest d : request.getDetalles()) {
            if (d.getProductoId() == null) {
                throw new IllegalArgumentException("productoId no puede ser nulo.");
            }
            if (d.getCantidad() == null || d.getCantidad() <= 0) {
                throw new IllegalArgumentException("cantidad (gramos) debe ser mayor a 0.");
            }
            if (d.getPrecioUnitario() == null || d.getPrecioUnitario() < 0) {
                throw new IllegalArgumentException("precioUnitario no puede ser negativo.");
            }
        }

        // Guardar tracking para compensación
        // devolvimos: para saber qué stock devolvimos
        // descontadosNuevos: para revertir si algo falla después
        List<DetalleVenta> devueltos = new java.util.ArrayList<>();
        List<DetalleVentaRequest> descontadosNuevos = new java.util.ArrayList<>();

        try {
            // 4) DEVOLVER stock de los detalles actuales
            for (DetalleVenta det : detallesActuales) {
                Long productoIdLong = det.getId().getProductoId();
                if (productoIdLong != null) {
                    productoClient.sumarStock(productoIdLong.intValue(), det.getCantidad());
                    devueltos.add(det);
                }
            }

            // 5) DESCONTAR stock de los detalles nuevos (en gramos)
            for (DetalleVentaRequest d : request.getDetalles()) {
                productoClient.descontarStock(d.getProductoId().intValue(), d.getCantidad());
                descontadosNuevos.add(d);
            }

            // 6) Reemplazar detalles en DB (ya el stock está ok)
            detalleVentaRepository.deleteAll(detallesActuales);

            double totalNuevo = 0;

            for (DetalleVentaRequest d : request.getDetalles()) {
                DetalleVentaId id = new DetalleVentaId(idVenta, d.getProductoId());

                DetalleVenta detalleNuevo = new DetalleVenta();
                detalleNuevo.setId(id);
                detalleNuevo.setCantidad(d.getCantidad()); // gramos
                detalleNuevo.setPrecioUnitario(d.getPrecioUnitario()); // precio por kilo

                detalleVentaRepository.save(detalleNuevo);

                // total = (gramos/1000)*precio_kilo
                totalNuevo += (d.getCantidad() / 1000.0) * d.getPrecioUnitario();
            }

            venta.setTotalVenta(totalNuevo);
            return ventaRepository.save(venta);

        } catch (Exception e) {

            // COMPENSACIÓN 1: si alcanzamos a descontar nuevos, los devolvemos
            for (DetalleVentaRequest d : descontadosNuevos) {
                try {
                    productoClient.sumarStock(d.getProductoId().intValue(), d.getCantidad());
                } catch (Exception ignored) {}
            }

            // COMPENSACIÓN 2: si ya devolvimos los anteriores, debemos volver a descontarlos
            // para dejar el sistema como estaba
            for (DetalleVenta det : devueltos) {
                try {
                    Long productoIdLong = det.getId().getProductoId();
                    if (productoIdLong != null) {
                        productoClient.descontarStock(productoIdLong.intValue(), det.getCantidad());
                    }
                } catch (Exception ignored) {}
            }

            throw e;
        }
    }


    //eliminar venta
    @Override
    public void eliminarVenta(Long idVenta) {
        if (idVenta == null) {
            throw new IllegalArgumentException("idVenta no puede ser nulo.");
        }

        if (!ventaRepository.existsById(idVenta)) {
            throw new IllegalArgumentException("La venta no existe.");
        }

        // 1) Obtener detalles (para devolver stock)
        List<DetalleVenta> detalles = detalleVentaRepository.findByIdVentaId(idVenta);

        // 2) Devolver stock al MS producto
        //    (cantidad en DetalleVenta = gramos)
        for (DetalleVenta det : detalles) {
            Long productoIdLong = det.getId().getProductoId();
            if (productoIdLong != null) {
                productoClient.sumarStock(productoIdLong.intValue(), det.getCantidad());
            }
        }

        // 3) Borrar detalles + venta
        detalleVentaRepository.deleteAll(detalles);
        ventaRepository.deleteById(idVenta);
    }



}
