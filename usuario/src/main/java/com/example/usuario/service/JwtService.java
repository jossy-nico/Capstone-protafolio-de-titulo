
package com.example.usuario.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import com.example.usuario.entity.Usuario;
import com.example.usuario.enums.EstadoUsuario;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${app.jwt.issuer:natur-cosecha-usuario-api}") String issuer) {

        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
    }

    // ==========================================
    // GENERAR TOKEN DE ACCESO
    // ==========================================

    public String generarToken(Usuario usuario) {

        if (usuario == null
                || usuario.getIdUsuario() == null
                || usuario.getTipoUsuario() == null) {
            throw new IllegalArgumentException(
                    "El usuario no es válido para generar JWT.");
        }

        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new IllegalStateException(
                    "No se puede generar JWT para una cuenta suspendida.");
        }

        Instant ahora = Instant.now();

        Instant vencimiento = ahora.plus(
                15,
                ChronoUnit.MINUTES);

        // ==========================================
        // INFORMACIÓN DEL TOKEN
        // ==========================================

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(usuario.getIdUsuario().toString())
                .issuedAt(ahora)
                .expiresAt(vencimiento)
                .audience(java.util.List.of("natur-cosecha-api"))
                .claim(
                        "rol",
                        usuario.getTipoUsuario().name())
                .claim(
                        "debeCambiarPassword",
                        Boolean.TRUE.equals(
                                usuario.getDebeCambiarPassword()))
                .build();

        // ==========================================
        // FIRMA RSA SHA-256
        // ==========================================

        JwsHeader header = JwsHeader
                .with(SignatureAlgorithm.RS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
