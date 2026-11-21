package com.example.resenna.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.resenna.entity.Resenna;

public interface ResennaRepository extends JpaRepository<Resenna, Long> {

    List<Resenna> findByProductoId(Long productoId);

    List<Resenna> findByClienteId(Long clienteId);

    Optional<Resenna> findByClienteIdAndVentaIdAndProductoId(
            Long clienteId,
            Long ventaId,
            Long productoId
    );

    boolean existsByClienteIdAndVentaIdAndProductoId(
            Long clienteId,
            Long ventaId,
            Long productoId
    );
}
