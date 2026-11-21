
package com.example.usuario.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.usuario.entity.SesionRefresh;
import com.example.usuario.entity.Usuario;
import com.example.usuario.enums.EstadoUsuario;
import com.example.usuario.enums.TipoUsuario;
import com.example.usuario.repository.SesionRefreshRepository;

@Service
public class SesionRefreshService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int DURACION_DIAS = 30;

    private final SesionRefreshRepository sesionRepository;
    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    public SesionRefreshService(
            SesionRefreshRepository sesionRepository,
            UsuarioService usuarioService,
            JwtService jwtService) {

        this.sesionRepository = sesionRepository;
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
    }

    // ==========================================
    // RESULTADO DE UNA SESIÓN
    // ==========================================

    public record ResultadoSesion(
            String accessToken,
            String refreshToken,
            long expiresIn,
            Long usuarioId) {
    }

    // ==========================================
    // VALIDAR CLIENTE ACTIVO
    // ==========================================

    private void validarCliente(Usuario usuario) {

        if (usuario == null
                || usuario.getIdUsuario() == null
                || usuario.getEstado() != EstadoUsuario.ACTIVO
                || usuario.getTipoUsuario() != TipoUsuario.CLIENTE) {

            throw new IllegalArgumentException(
                    "La sesión no está disponible.");
        }
    }

    // ==========================================
    // GENERAR TOKEN ALEATORIO
    // ==========================================

    private String generarRefreshToken() {

        byte[] bytes = new byte[32];

        RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    // ==========================================
    // HASH SHA-256
    // ==========================================

    private String calcularHash(String token) {

        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {

            throw new IllegalStateException(
                    "No se pudo proteger la sesión.", ex);
        }
    }

    // ==========================================
    // CREAR SESIÓN PERSISTENTE
    // ==========================================

    @Transactional
    public ResultadoSesion crearSesion(Usuario usuario) {

        validarCliente(usuario);

        Instant ahora = Instant.now();

        String refreshToken = generarRefreshToken();

        SesionRefresh sesion = new SesionRefresh();

        sesion.setUsuario(usuario);
        sesion.setTokenHash(
                calcularHash(refreshToken));
        sesion.setFechaCreacion(ahora);
        sesion.setFechaExpiracion(
                ahora.plus(DURACION_DIAS, ChronoUnit.DAYS));
        sesion.setFechaRevocacion(null);

        sesionRepository.save(sesion);

        String accessToken = jwtService.generarToken(usuario);

        return new ResultadoSesion(
                accessToken,
                refreshToken,
                900L,
                usuario.getIdUsuario());
    }

    // ==========================================
    // RENOVAR SESIÓN
    // ==========================================

    @Transactional
    public ResultadoSesion renovarSesion(
            String refreshToken) {

        final String mensajeError = "La sesión no es válida o ha expirado.";

        if (refreshToken == null
                || refreshToken.isBlank()
                || refreshToken.length() > 200) {

            throw new IllegalArgumentException(
                    mensajeError);
        }

        String hash = calcularHash(refreshToken.trim());

        SesionRefresh sesion = sesionRepository
                .findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException(
                        mensajeError));

        Instant ahora = Instant.now();

        if (sesion.getFechaRevocacion() != null
                || sesion.getFechaExpiracion() == null
                || !sesion.getFechaExpiracion().isAfter(ahora)) {

            throw new IllegalArgumentException(
                    mensajeError);
        }

        Long usuarioId = sesion.getUsuario().getIdUsuario();

        Usuario usuario = usuarioService
                .findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        mensajeError));

        validarCliente(usuario);

        // Revocar el refresh token utilizado.
        sesion.setFechaRevocacion(ahora);
        sesionRepository.save(sesion);

        // Emitir una nueva credencial de renovación.
        return crearSesion(usuario);
    }

    // ==========================================
    // CERRAR SESIÓN
    // ==========================================

    @Transactional
    public void revocarSesion(String refreshToken) {

        if (refreshToken == null
                || refreshToken.isBlank()
                || refreshToken.length() > 200) {
            return;
        }

        String hash = calcularHash(refreshToken.trim());

        sesionRepository.findByTokenHash(hash)
                .ifPresent(sesion -> {

                    if (sesion.getFechaRevocacion() == null) {

                        sesion.setFechaRevocacion(
                                Instant.now());

                        sesionRepository.save(sesion);
                    }
                });
    }
}
