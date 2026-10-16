package com.example.venta.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.venta.entity.DetalleVenta;
import com.example.venta.entity.DetalleVentaId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface DetalleVentaRepository
        extends JpaRepository<DetalleVenta, DetalleVentaId> {

    List<DetalleVenta> findByIdVentaId(Long ventaId);

    @Query("""
            SELECT COUNT(d)
            FROM DetalleVenta d, Venta v
            WHERE d.id.ventaId = v.idVenta
              AND v.clienteId = :clienteId
              AND d.id.productoId = :productoId
            """)
    long contarComprasProductoPorCliente(
            @Param("clienteId") Long clienteId,
            @Param("productoId") Long productoId
    );

    @Query("""
            SELECT COUNT(d)
            FROM DetalleVenta d, Venta v
            WHERE d.id.ventaId = v.idVenta
              AND v.idVenta = :ventaId
              AND v.clienteId = :clienteId
              AND d.id.productoId = :productoId
            """)
    long contarProductoCompradoEnVenta(
            @Param("clienteId") Long clienteId,
            @Param("ventaId") Long ventaId,
            @Param("productoId") Long productoId
    );
}
