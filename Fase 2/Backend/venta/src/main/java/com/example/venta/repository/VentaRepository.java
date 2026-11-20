
package com.example.venta.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.venta.entity.Venta;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    // ==========================================
    // VENTAS DE CLIENTES REGISTRADOS
    // ==========================================

    List<Venta> findByClienteId(Long clienteId);

    // ==========================================
    // HU-32: SEGUIMIENTO DE INVITADOS
    // ==========================================
    //
    // Búsqueda exacta por código privado.
    // El código no se deriva del ID de venta.
    // El servicio debe verificar:
    // - Que sea una compra de invitado.
    // - Que el pago esté confirmado.
    // - Que solo se entreguen datos logísticos.
    // ==========================================

    Optional<Venta> findByCodigoSeguimiento(
            String codigoSeguimiento);
}