package com.example.venta.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.venta.entity.CompraPendiente;

@Repository
public interface CompraPendienteRepository
        extends JpaRepository<CompraPendiente, Long> {

    Optional<CompraPendiente> findByTokenWs(String tokenWs);

    Optional<CompraPendiente> findByBuyOrder(String buyOrder);

    Optional<CompraPendiente> findByVentaId(Long ventaId);
}