
package com.example.usuario.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.usuario.dto.CambiarPasswordRequest;
import com.example.usuario.dto.LoginRequest;
import com.example.usuario.dto.LoginResponse;
import com.example.usuario.dto.UsuarioResponse;
import com.example.usuario.dto.UsuarioResumenResponse;

import com.example.usuario.entity.Usuario;
import com.example.usuario.enums.EstadoUsuario;
import com.example.usuario.enums.TipoUsuario;

import com.example.usuario.service.JwtService;
import com.example.usuario.service.UsuarioService;

import java.util.Map;
import com.example.usuario.service.SesionRefreshService;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

        private final UsuarioService usuarioService;
        private final JwtService jwtService;
        private final SesionRefreshService sesionRefreshService;

        public UsuarioController(
                        UsuarioService usuarioService,
                        JwtService jwtService,
                        SesionRefreshService sesionRefreshService) {

                this.usuarioService = usuarioService;
                this.jwtService = jwtService;
                this.sesionRefreshService = sesionRefreshService;
        }

        // ==========================================
        // LISTAR USUARIOS
        // SOLO ADMIN - CONTROLADO POR SECURITYCONFIG
        // ==========================================

        @GetMapping
        public ResponseEntity<List<UsuarioResponse>> listar() {

                List<UsuarioResponse> usuarios = usuarioService.findAll()
                                .stream()
                                .map(this::convertirAResponse)
                                .toList();

                return ResponseEntity.ok(usuarios);
        }

        // ==========================================
        // MI PERFIL - CLIENTE AUTENTICADO
        // ==========================================
        // El identificador se obtiene del JWT.
        // Nunca desde un ID enviado por Android.
        // No devuelve contraseñas.
        // ==========================================

        @GetMapping("/mi-perfil")
        public ResponseEntity<?> obtenerMiPerfil(
                        @AuthenticationPrincipal Jwt jwt) {

                if (jwt == null || jwt.getSubject() == null) {
                        return ResponseEntity
                                        .status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of(
                                                        "error",
                                                        "Debes iniciar sesión."));
                }

                final Long usuarioId;

                try {
                        usuarioId = Long.parseLong(jwt.getSubject());
                } catch (NumberFormatException ex) {
                        return ResponseEntity
                                        .status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of(
                                                        "error",
                                                        "Sesión inválida."));
                }

                var usuarioEncontrado = usuarioService.findById(usuarioId);

                if (usuarioEncontrado.isEmpty()) {
                        return ResponseEntity.notFound().build();
                }

                Usuario usuario = usuarioEncontrado.get();

                if (usuario.getEstado() != EstadoUsuario.ACTIVO
                                || usuario.getTipoUsuario() != TipoUsuario.CLIENTE) {

                        return ResponseEntity
                                        .status(HttpStatus.FORBIDDEN)
                                        .body(Map.of(
                                                        "error",
                                                        "Cuenta no autorizada."));
                }

                return ResponseEntity.ok(
                                convertirAResponse(usuario));
        }

        // ==========================================
        // BUSCAR USUARIO
        // SOLO ADMIN
        // ==========================================

        @GetMapping("/{idUsuario}")
        public ResponseEntity<UsuarioResponse> buscar(
                        @PathVariable Long idUsuario) {

                return usuarioService.findById(idUsuario)
                                .map(this::convertirAResponse)
                                .map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.notFound().build());
        }

        // ==========================================
        // REGISTRO PUBLICO DE CLIENTES
        // ==========================================

        @PostMapping
        public ResponseEntity<?> crear(
                        @RequestBody Usuario usuario) {

                if (usuario == null) {
                        return ResponseEntity.badRequest()
                                        .body(
                                                        "Los datos del usuario son obligatorios.");
                }

                // Nunca aceptar roles privilegiados
                // desde el registro público.

                if (usuario.getTipoUsuario() != null
                                && usuario.getTipoUsuario() != TipoUsuario.CLIENTE) {

                        return ResponseEntity
                                        .status(HttpStatus.FORBIDDEN)
                                        .body(
                                                        "El registro público solo permite crear clientes.");
                }

                usuario.setTipoUsuario(TipoUsuario.CLIENTE);

                try {

                        Usuario nuevo = usuarioService.create(usuario);

                        return ResponseEntity
                                        .status(HttpStatus.CREATED)
                                        .body(convertirAResponse(nuevo));

                } catch (IllegalArgumentException ex) {

                        return ResponseEntity.badRequest()
                                        .body(ex.getMessage());
                }
        }

        // ==========================================
        // ACTUALIZAR MI PERFIL
        // IDENTIDAD OBTENIDA DEL JWT
        // ==========================================

        public record ActualizarMiPerfilRequest(
                        String nombre,
                        String apellido) {
        }

        @PutMapping("/mi-perfil")
        public ResponseEntity<?> actualizarMiPerfil(
                        @AuthenticationPrincipal Jwt jwt,
                        @RequestBody(required = false) ActualizarMiPerfilRequest request) {

                if (jwt == null || jwt.getSubject() == null) {

                        return ResponseEntity
                                        .status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of(
                                                        "error",
                                                        "Debes iniciar sesión."));
                }

                if (request == null) {

                        return ResponseEntity.badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        "Los datos son obligatorios."));
                }

                final Long usuarioId;

                try {

                        usuarioId = Long.parseLong(
                                        jwt.getSubject());

                } catch (NumberFormatException ex) {

                        return ResponseEntity
                                        .status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of(
                                                        "error",
                                                        "Sesión inválida."));
                }

                try {

                        Usuario actualizado = usuarioService.actualizarMiPerfil(
                                        usuarioId,
                                        request.nombre(),
                                        request.apellido());

                        return ResponseEntity.ok(
                                        convertirAResponse(actualizado));

                } catch (IllegalArgumentException ex) {

                        return ResponseEntity.badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        ex.getMessage()));
                }
        }

        // ==========================================
        // ACTUALIZAR DATOS PERSONALES
        // SOLO ADMIN
        // NO MODIFICA ROLES NI CONTRASEÑAS
        // ==========================================

        @PutMapping("/{idUsuario}")
        public ResponseEntity<?> actualizar(
                        @PathVariable Long idUsuario,
                        @RequestBody Usuario usuario) {

                if (usuarioService.findById(idUsuario).isEmpty()) {

                        return ResponseEntity
                                        .status(HttpStatus.NOT_FOUND)
                                        .body("Usuario no encontrado.");
                }

                try {

                        Usuario actualizado = usuarioService.update(
                                        idUsuario,
                                        usuario);

                        return ResponseEntity.ok(
                                        convertirAResponse(actualizado));

                } catch (IllegalArgumentException ex) {

                        return ResponseEntity.badRequest()
                                        .body(ex.getMessage());
                }
        }

        // ==========================================
        // ELIMINACION FISICA DESHABILITADA
        // ==========================================

        @DeleteMapping("/{idUsuario}")
        public ResponseEntity<String> eliminar(
                        @PathVariable Long idUsuario) {

                return ResponseEntity
                                .status(HttpStatus.METHOD_NOT_ALLOWED)
                                .body(
                                                "La eliminación física está deshabilitada. " +
                                                                "Utiliza la suspensión de cuentas.");
        }

        // ==========================================
        // LOGIN ORIGINAL
        // COMPATIBILIDAD CON ANDROID
        // ==========================================

        @PostMapping("/login")
        public ResponseEntity<UsuarioResponse> login(
                        @RequestBody LoginRequest request) {

                if (request == null) {
                        return ResponseEntity.badRequest().build();
                }

                return usuarioService
                                .login(
                                                request.getCorreo(),
                                                request.getPassword())
                                .map(this::convertirAResponse)
                                .map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity
                                                .status(HttpStatus.UNAUTHORIZED)
                                                .build());
        }

        // ==========================================
        // LOGIN JWT
        // ==========================================

        @PostMapping("/login-token")
        public ResponseEntity<LoginResponse> loginConToken(
                        @RequestBody LoginRequest request) {

                if (request == null) {
                        return ResponseEntity.badRequest().build();
                }

                return usuarioService
                                .login(
                                                request.getCorreo(),
                                                request.getPassword())
                                .map(usuario -> {

                                        String token = jwtService.generarToken(usuario);

                                        LoginResponse respuesta = new LoginResponse(
                                                        token,
                                                        "Bearer",
                                                        900L,
                                                        convertirAResponse(usuario));

                                        return ResponseEntity.ok(respuesta);

                                })
                                .orElseGet(() -> ResponseEntity
                                                .status(HttpStatus.UNAUTHORIZED)
                                                .build());
        }

        // =====================================================
        // SESIÓN PERSISTENTE - NATUR COSECHA
        // =====================================================
        //
        // Mantiene el login JWT original sin modificaciones.
        // Los nuevos endpoints son exclusivos para clientes.
        // =====================================================

        // ==========================================
        // 1. INICIAR SESIÓN PERSISTENTE
        // ==========================================

        @PostMapping("/login-token-persistente")
        public ResponseEntity<?> loginTokenPersistente(
                        @RequestBody(required = false) LoginRequest request) {

                if (request == null
                                || request.getCorreo() == null
                                || request.getPassword() == null
                                || request.getCorreo().isBlank()
                                || request.getPassword().isBlank()) {

                        return ResponseEntity.badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        "Correo y contraseña son obligatorios."));
                }

                try {

                        var usuarioEncontrado = usuarioService.login(
                                        request.getCorreo(),
                                        request.getPassword());

                        if (usuarioEncontrado.isEmpty()) {

                                return ResponseEntity
                                                .status(HttpStatus.UNAUTHORIZED)
                                                .body(Map.of(
                                                                "error",
                                                                "Credenciales incorrectas."));
                        }

                        Usuario usuario = usuarioEncontrado.get();

                        SesionRefreshService.ResultadoSesion sesion = sesionRefreshService.crearSesion(usuario);

                        return ResponseEntity.ok(Map.of(
                                        "token", sesion.accessToken(),
                                        "tokenType", "Bearer",
                                        "expiresIn", sesion.expiresIn(),
                                        "refreshToken", sesion.refreshToken(),
                                        "usuario", convertirAResponse(usuario)));

                } catch (IllegalArgumentException
                                | IllegalStateException ex) {

                        return ResponseEntity
                                        .status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of(
                                                        "error",
                                                        "No fue posible iniciar la sesión."));
                }
        }

        // ==========================================
        // 2. RENOVAR SESIÓN AUTOMÁTICAMENTE
        // ==========================================

        @PostMapping("/refresh-token")
        public ResponseEntity<?> renovarToken(
                        @RequestBody(required = false) Map<String, String> request) {

                if (request == null
                                || request.get("refreshToken") == null
                                || request.get("refreshToken").isBlank()) {

                        return ResponseEntity.badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        "El refresh token es obligatorio."));
                }

                try {

                        SesionRefreshService.ResultadoSesion sesion = sesionRefreshService.renovarSesion(
                                        request.get("refreshToken"));

                        Usuario usuario = usuarioService
                                        .findById(sesion.usuarioId())
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "Sesión no disponible."));

                        return ResponseEntity.ok(Map.of(
                                        "token", sesion.accessToken(),
                                        "tokenType", "Bearer",
                                        "expiresIn", sesion.expiresIn(),
                                        "refreshToken", sesion.refreshToken(),
                                        "usuario", convertirAResponse(usuario)));

                } catch (IllegalArgumentException
                                | IllegalStateException ex) {

                        return ResponseEntity
                                        .status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of(
                                                        "error",
                                                        "La sesión ha expirado o fue revocada."));
                }
        }

        // ==========================================
        // 3. CERRAR SESIÓN PERSISTENTE
        // ==========================================

        @PostMapping("/logout-token")
        public ResponseEntity<?> cerrarSesionPersistente(
                        @RequestBody(required = false) Map<String, String> request) {

                if (request == null
                                || request.get("refreshToken") == null
                                || request.get("refreshToken").isBlank()) {

                        return ResponseEntity.badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        "El refresh token es obligatorio."));
                }

                // Revoca la sesión si existe.
                // No informa si el token era válido o no.
                sesionRefreshService.revocarSesion(
                                request.get("refreshToken"));

                return ResponseEntity.ok(Map.of(
                                "mensaje",
                                "Solicitud de cierre de sesión procesada."));
        }

        // ==========================================
        // PERFIL RESUMIDO
        // TITULAR O ADMIN
        // ==========================================

        @GetMapping("/{idUsuario}/resumen")
        public ResponseEntity<UsuarioResumenResponse> obtenerResumen(
                        @PathVariable Long idUsuario) {

                return usuarioService.findById(idUsuario)
                                .map(usuario -> ResponseEntity.ok(
                                                new UsuarioResumenResponse(
                                                                usuario.getIdUsuario(),
                                                                usuario.getNombre())))
                                .orElseGet(() -> ResponseEntity.notFound().build());
        }

        // ==========================================
        // SUSPENDER CUENTA
        // SOLO ADMIN
        // ==========================================

        @PutMapping("/{idUsuario}/suspender")
        public ResponseEntity<?> suspender(
                        @PathVariable Long idUsuario,
                        @AuthenticationPrincipal Jwt jwt) {

                // Impedir que el administrador
                // suspenda su propia cuenta.

                if (jwt != null
                                && String.valueOf(idUsuario)
                                                .equals(jwt.getSubject())) {

                        return ResponseEntity
                                        .status(HttpStatus.CONFLICT)
                                        .body(
                                                        "No puedes suspender tu propia cuenta administrativa.");
                }

                var usuarioExistente = usuarioService.findById(idUsuario);

                if (usuarioExistente.isEmpty()) {

                        return ResponseEntity
                                        .status(HttpStatus.NOT_FOUND)
                                        .body("Usuario no encontrado.");
                }

                Usuario objetivo = usuarioExistente.get();

                // No permitir la suspensión del último
                // administrador activo.

                if (objetivo.getTipoUsuario() == TipoUsuario.ADMIN
                                && objetivo.getEstado() == EstadoUsuario.ACTIVO) {

                        long otrosAdministradoresActivos = usuarioService.findAll()
                                        .stream()
                                        .filter(usuario -> usuario.getTipoUsuario() == TipoUsuario.ADMIN)
                                        .filter(usuario -> usuario.getEstado() == EstadoUsuario.ACTIVO)
                                        .filter(usuario -> !usuario.getIdUsuario()
                                                        .equals(idUsuario))
                                        .count();

                        if (otrosAdministradoresActivos == 0) {

                                return ResponseEntity
                                                .status(HttpStatus.CONFLICT)
                                                .body(
                                                                "No es posible suspender al último administrador activo.");
                        }
                }

                try {

                        Usuario usuario = usuarioService.suspender(idUsuario);

                        return ResponseEntity.ok(
                                        convertirAResponse(usuario));

                } catch (IllegalArgumentException ex) {

                        return ResponseEntity
                                        .status(HttpStatus.NOT_FOUND)
                                        .body(ex.getMessage());
                }
        }

        // ==========================================
        // REACTIVAR CUENTA
        // SOLO ADMIN
        // ==========================================

        @PutMapping("/{idUsuario}/reactivar")
        public ResponseEntity<?> reactivar(
                        @PathVariable Long idUsuario) {

                try {

                        Usuario usuario = usuarioService.reactivar(idUsuario);

                        return ResponseEntity.ok(
                                        convertirAResponse(usuario));

                } catch (IllegalArgumentException ex) {

                        return ResponseEntity
                                        .status(HttpStatus.NOT_FOUND)
                                        .body(ex.getMessage());
                }
        }

        // ==========================================
        // CAMBIAR CONTRASEÑA
        // SOLO TITULAR DE CUENTA ACTIVA
        // ==========================================

        @PutMapping("/{idUsuario}/cambiar-password")
        public ResponseEntity<?> cambiarPassword(
                        @PathVariable("idUsuario") Long idUsuario,
                        @RequestBody CambiarPasswordRequest request) {

                if (request == null) {
                        return ResponseEntity.badRequest()
                                        .body(
                                                        "Los datos de contraseña son obligatorios.");
                }

                try {

                        Usuario usuario = usuarioService.cambiarPassword(
                                        idUsuario,
                                        request.getPasswordActual(),
                                        request.getPasswordNueva());

                        return ResponseEntity.ok(
                                        convertirAResponse(usuario));

                } catch (IllegalArgumentException ex) {

                        return ResponseEntity.badRequest()
                                        .body(ex.getMessage());
                }
        }

        // ==========================================
        // RESPUESTA SIN CONTRASEÑA
        // ==========================================

        private UsuarioResponse convertirAResponse(
                        Usuario usuario) {

                return new UsuarioResponse(
                                usuario.getIdUsuario(),
                                usuario.getNombre(),
                                usuario.getApellido(),
                                usuario.getCorreo(),
                                usuario.getTipoUsuario(),
                                usuario.getEstado(),
                                usuario.getDebeCambiarPassword());
        }
}
