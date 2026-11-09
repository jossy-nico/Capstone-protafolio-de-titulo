package com.example.venta.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.venta.entity.PagoWebpay;

@Repository
public interface PagoWebpayRepository
        extends JpaRepository<PagoWebpay, Long> {

    Optional<PagoWebpay> findByTokenWs(String tokenWs);

    Optional<PagoWebpay> findByBuyOrder(String buyOrder);

    Optional<PagoWebpay> findByVentaId(Long ventaId);
}