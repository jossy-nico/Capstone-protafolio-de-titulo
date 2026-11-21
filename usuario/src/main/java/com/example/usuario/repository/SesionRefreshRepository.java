
package com.example.usuario.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.usuario.entity.SesionRefresh;

@Repository
public interface SesionRefreshRepository
        extends JpaRepository<SesionRefresh, Long> {

    Optional<SesionRefresh> findByTokenHash(
            String tokenHash);

    // Revocar todas las sesiones persistentes
    // activas del usuario después de cambiar
    // correctamente la contraseña.

    @Modifying
    @Query("""
            UPDATE SesionRefresh s
            SET s.fechaRevocacion = :fecha
            WHERE s.usuario.idUsuario = :usuarioId
              AND s.fechaRevocacion IS NULL
            """)
    int revocarSesionesDeUsuario(
            @Param("usuarioId") Long usuarioId,
            @Param("fecha") Instant fecha);
}
