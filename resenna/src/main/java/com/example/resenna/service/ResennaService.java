package com.example.resenna.service;

import java.util.List;

import com.example.resenna.dto.ResennaRequest;
import com.example.resenna.dto.ResennaResponse;

public interface ResennaService {
        ResennaResponse crearResenna(ResennaRequest request);

        List<ResennaResponse> obtenerResennasProducto(Long productoId);

        ResennaResponse obtenerResennaCompra(
                Long clienteId,
                Long ventaId,
                Long productoId
        );

        Double obtenerPromedioProducto(Long productoId);

        ResennaResponse actualizarResena(
                Long idResenna,
                ResennaRequest request
        );

        void eliminarResena(Long idResenna);

}
