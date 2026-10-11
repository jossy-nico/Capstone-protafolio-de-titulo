package com.example.resenna.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.resenna.client.UsuarioClient;
import com.example.resenna.client.VentaClient;
import com.example.resenna.dto.ResennaRequest;
import com.example.resenna.dto.ResennaResponse;
import com.example.resenna.entity.Resenna;
import com.example.resenna.repository.ResennaRepository;
import com.example.resenna.service.ResennaService;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ResennaServiceImpl implements ResennaService {

    private final ResennaRepository resennaRepository;
    private final VentaClient ventaClient;
    private final UsuarioClient usuarioClient;

    public ResennaServiceImpl(
            ResennaRepository resennaRepository,
            VentaClient ventaClient,
            UsuarioClient usuarioClient
    ) {
        this.resennaRepository = resennaRepository;
        this.ventaClient = ventaClient;
        this.usuarioClient = usuarioClient;
    }

    @Override
    public ResennaResponse crearResenna(ResennaRequest request) {

        validarRequestCreacion(request);

        /*
         * Debe validar que:
         * 1. La venta pertenece al cliente.
         * 2. El producto está incluido en esa venta.
         */
        boolean comproProductoEnVenta =
                ventaClient.clienteComproProductoEnVenta(
                        request.getClienteId(),
                        request.getVentaId(),
                        request.getProductoId()
                );

        if (!comproProductoEnVenta) {
            throw new IllegalArgumentException(
                    "El cliente no puede valorar este producto porque no aparece en la compra indicada."
            );
        }

        boolean yaExiste =
                resennaRepository
                        .existsByClienteIdAndVentaIdAndProductoId(
                                request.getClienteId(),
                                request.getVentaId(),
                                request.getProductoId()
                        );

        if (yaExiste) {
            throw new IllegalArgumentException(
                    "Ya existe una reseña para este producto en esta compra. Puede editar la reseña existente."
            );
        }

        LocalDateTime ahora = LocalDateTime.now();

        Resenna resenna = new Resenna();

        resenna.setClienteId(request.getClienteId());
        resenna.setVentaId(request.getVentaId());
        resenna.setProductoId(request.getProductoId());
        resenna.setEstrellas(request.getEstrellas());
        resenna.setComentario(
                limpiarComentario(request.getComentario())
        );
        resenna.setImagenUrl(null);
        resenna.setImagePublicId(null);
        resenna.setFechaCreacion(ahora);
        resenna.setFechaActualizacion(null);

        Resenna guardada = resennaRepository.save(resenna);

        return convertirAResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResennaResponse> obtenerResennasProducto(Long productoId) {

        if (productoId == null) {
            throw new IllegalArgumentException(
                    "El productoId no puede ser nulo."
            );
        }

        return resennaRepository
                .findByProductoId(productoId)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Double obtenerPromedioProducto(Long productoId) {

        if (productoId == null) {
            throw new IllegalArgumentException(
                    "El productoId no puede ser nulo."
            );
        }

        List<Resenna> resennas =
                resennaRepository.findByProductoId(productoId);

        if (resennas.isEmpty()) {
            return 0.0;
        }

        return resennas.stream()
                .mapToInt(Resenna::getEstrellas)
                .average()
                .orElse(0.0);
    }

    @Override
    public ResennaResponse actualizarResena(
            Long idResenna,
            ResennaRequest request
    ) {

        if (idResenna == null) {
            throw new IllegalArgumentException(
                    "El idResenna no puede ser nulo."
            );
        }

        if (request == null) {
            throw new IllegalArgumentException(
                    "La solicitud no puede ser nula."
            );
        }

        if (request.getClienteId() == null) {
            throw new IllegalArgumentException(
                    "El clienteId es obligatorio."
            );
        }

        Resenna existente = resennaRepository
                .findById(idResenna)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "La reseña no existe."
                        )
                );

        /*
         * Solo el cliente propietario puede editarla.
         */
        if (!existente.getClienteId().equals(request.getClienteId())) {
            throw new IllegalArgumentException(
                    "El cliente no tiene permiso para modificar esta reseña."
            );
        }

        /*
         * No se permite cambiar la venta ni el producto
         * asociados originalmente a la reseña.
         */
        if (request.getVentaId() != null &&
                !existente.getVentaId().equals(request.getVentaId())) {

            throw new IllegalArgumentException(
                    "No se puede cambiar la venta asociada a la reseña."
            );
        }

        if (request.getProductoId() != null &&
                !existente.getProductoId().equals(request.getProductoId())) {

            throw new IllegalArgumentException(
                    "No se puede cambiar el producto asociado a la reseña."
            );
        }

        if (request.getEstrellas() != null) {
            validarEstrellas(request.getEstrellas());
            existente.setEstrellas(request.getEstrellas());
        }

        if (request.getComentario() != null) {

            validarLongitudComentario(request.getComentario());

            existente.setComentario(
                    limpiarComentario(request.getComentario())
            );
        }

        existente.setFechaActualizacion(LocalDateTime.now());

        Resenna actualizada =
                resennaRepository.save(existente);

        return convertirAResponse(actualizada);
    }

    @Override
    public void eliminarResena(Long idResenna) {

        if (idResenna == null) {
            throw new IllegalArgumentException(
                    "El idResenna no puede ser nulo."
            );
        }

        Resenna existente = resennaRepository
                .findById(idResenna)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "La reseña no existe."
                        )
                );

        resennaRepository.delete(existente);
    }

    private void validarRequestCreacion(ResennaRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "La solicitud de reseña no puede ser nula."
            );
        }

        if (request.getClienteId() == null) {
            throw new IllegalArgumentException(
                    "El clienteId es obligatorio."
            );
        }

        if (request.getVentaId() == null) {
            throw new IllegalArgumentException(
                    "El ventaId es obligatorio."
            );
        }

        if (request.getProductoId() == null) {
            throw new IllegalArgumentException(
                    "El productoId es obligatorio."
            );
        }

        if (request.getEstrellas() == null) {
            throw new IllegalArgumentException(
                    "La cantidad de estrellas es obligatoria."
            );
        }

        validarEstrellas(request.getEstrellas());
        validarLongitudComentario(request.getComentario());
    }

    private void validarEstrellas(Integer estrellas) {

        if (estrellas == null || estrellas < 1 || estrellas > 5) {
            throw new IllegalArgumentException(
                    "Las estrellas deben estar entre 1 y 5."
            );
        }
    }

    private void validarLongitudComentario(String comentario) {

        if (comentario != null && comentario.length() > 1000) {
            throw new IllegalArgumentException(
                    "El comentario no puede superar los 1000 caracteres."
            );
        }
    }

    private String limpiarComentario(String comentario) {

        if (comentario == null || comentario.isBlank()) {
            return null;
        }

        return comentario.trim();
    }

    private ResennaResponse convertirAResponse(Resenna resenna) {

        String nombreCliente =
                usuarioClient.obtenerNombreUsuario(
                        resenna.getClienteId()
                );

        return new ResennaResponse(
                resenna.getIdResenna(),
                resenna.getClienteId(),
                nombreCliente,
                resenna.getVentaId(),
                resenna.getProductoId(),
                resenna.getEstrellas(),
                resenna.getComentario(),
                resenna.getImagenUrl(),
                resenna.getFechaCreacion(),
                resenna.getFechaActualizacion()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ResennaResponse obtenerResennaCompra(
            Long clienteId,
            Long ventaId,
            Long productoId
    ) {

        if (clienteId == null || ventaId == null || productoId == null) {
            throw new IllegalArgumentException(
                    "clienteId, ventaId y productoId son obligatorios."
            );
        }

        Resenna resenna = resennaRepository
                .findByClienteIdAndVentaIdAndProductoId(
                        clienteId,
                        ventaId,
                        productoId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No existe una reseña para este producto en esta compra."
                        )
                );

        return convertirAResponse(resenna);
    }
}
