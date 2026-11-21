
package com.example.usuario.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.usuario.dto.ClienteResponse;
import com.example.usuario.entity.Cliente;
import com.example.usuario.service.ClienteService;
import com.example.usuario.service.PerfilClienteService;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

        @Autowired
        private ClienteService clienteService;

        @Autowired
        private PerfilClienteService perfilClienteService;

        // ==========================================
        // LISTAR CLIENTES
        // ==========================================
        // Spring Security controla el acceso.
        // Actualmente esta ruta queda denegada
        // por las reglas generales.

        @GetMapping
        public ResponseEntity<List<Cliente>> listar() {

                List<Cliente> clientes = clienteService.findAll();

                if (clientes.isEmpty()) {
                        return new ResponseEntity<>(
                                        HttpStatus.NO_CONTENT);
                }

                return ResponseEntity.ok(clientes);
        }

        // ==========================================
        // CONSULTAR PERFIL DE CLIENTE
        // ==========================================
        // SecurityConfig permite al titular
        // autenticado o al administrador autorizado.

        @GetMapping("/{usuarioId}")
        public ResponseEntity<Cliente> buscar(
                        @PathVariable("usuarioId") Long usuarioId) {

                return clienteService.findById(usuarioId)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        }

        // ==========================================
        // CREAR PERFIL CLIENTE CON JWT
        // ==========================================
        //
        // El usuario debe:
        // 1. Estar autenticado.
        // 2. Tener rol CLIENTE.
        // 3. Estar ACTIVO (SecurityConfig).
        // 4. Crear únicamente su propio perfil.
        //
        // Se conserva ClienteServiceImpl,
        // incluyendo validación del teléfono.
        // ==========================================

        @PostMapping
        public ResponseEntity<?> crear(
                        @RequestBody Cliente cliente,
                        @AuthenticationPrincipal Jwt jwt) {

                // ======================================
                // 1. VERIFICAR AUTENTICACIÓN
                // ======================================

                if (jwt == null || jwt.getSubject() == null) {

                        return ResponseEntity
                                        .status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of(
                                                        "error",
                                                        "Debes iniciar sesión para crear tu perfil."));
                }

                // ======================================
                // 2. VALIDAR IDENTIDAD JWT
                // ======================================

                final Long usuarioAutenticadoId;

                try {

                        usuarioAutenticadoId = Long.valueOf(jwt.getSubject());

                        if (usuarioAutenticadoId <= 0) {
                                throw new NumberFormatException();
                        }

                } catch (NumberFormatException ex) {

                        return ResponseEntity
                                        .status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of(
                                                        "error",
                                                        "La identidad de la sesión no es válida."));
                }

                // ======================================
                // 3. VALIDAR ROL
                // ======================================

                if (!"CLIENTE".equals(
                                jwt.getClaimAsString("rol"))) {

                        return ResponseEntity
                                        .status(HttpStatus.FORBIDDEN)
                                        .body(Map.of(
                                                        "error",
                                                        "Solo los clientes pueden crear su perfil."));
                }

                // ======================================
                // 4. VALIDAR DATOS RECIBIDOS
                // ======================================

                if (cliente == null ||
                                cliente.getUsuario() == null ||
                                cliente.getUsuario().getIdUsuario() == null) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        "Debes indicar el usuario asociado al perfil."));
                }

                Long usuarioSolicitadoId = cliente.getUsuario().getIdUsuario();

                // ======================================
                // 5. VALIDAR PROPIEDAD DEL PERFIL
                // ======================================
                //
                // Nunca confiar solamente en el ID
                // enviado desde Android.
                //
                // El ID debe coincidir con el JWT.

                if (!usuarioAutenticadoId.equals(
                                usuarioSolicitadoId)) {

                        return ResponseEntity
                                        .status(HttpStatus.FORBIDDEN)
                                        .body(Map.of(
                                                        "error",
                                                        "No puedes crear el perfil de otro usuario."));
                }

                // ======================================
                // 6. EVITAR DUPLICADOS
                // ======================================

                if (clienteService
                                .findById(usuarioAutenticadoId)
                                .isPresent()) {

                        return ResponseEntity
                                        .status(HttpStatus.CONFLICT)
                                        .body(Map.of(
                                                        "error",
                                                        "Este usuario ya tiene un perfil de cliente."));
                }

                // ======================================
                // 7. CREAR PERFIL
                // ======================================

                try {

                        Cliente nuevoCliente = clienteService.create(cliente);

                        ClienteResponse response = new ClienteResponse(
                                        nuevoCliente.getUsuarioId(),
                                        nuevoCliente.getUsuario().getNombre(),
                                        nuevoCliente.getUsuario().getApellido(),
                                        nuevoCliente.getUsuario().getCorreo(),
                                        nuevoCliente.getTelefono(),
                                        nuevoCliente.getDireccion());

                        return ResponseEntity
                                        .status(HttpStatus.CREATED)
                                        .body(response);

                } catch (IllegalArgumentException ex) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        ex.getMessage() == null
                                                                        ? "Datos del cliente inválidos."
                                                                        : ex.getMessage()));
                }
        }

        // ==========================================
        // ACTUALIZAR MI PERFIL
        // CLIENTE AUTENTICADO
        // ==========================================

        public record ActualizarPerfilRequest(
                        String nombre,
                        String apellido,
                        String telefono,
                        String direccion) {
        }

        @PutMapping("/mi-perfil")
        public ResponseEntity<?> actualizarMiPerfil(
                        @AuthenticationPrincipal Jwt jwt,
                        @RequestBody(required = false) ActualizarPerfilRequest request) {

                if (jwt == null || jwt.getSubject() == null) {

                        return ResponseEntity
                                        .status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of(
                                                        "error",
                                                        "Debes iniciar sesión."));
                }

                if (!"CLIENTE".equals(
                                jwt.getClaimAsString("rol"))) {

                        return ResponseEntity
                                        .status(HttpStatus.FORBIDDEN)
                                        .body(Map.of(
                                                        "error",
                                                        "Solo los clientes pueden editar su perfil."));
                }

                if (request == null) {

                        return ResponseEntity.badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        "Los datos del perfil son obligatorios."));
                }

                final Long usuarioId;

                try {

                        usuarioId = Long.valueOf(jwt.getSubject());

                        if (usuarioId <= 0) {
                                throw new NumberFormatException();
                        }

                } catch (NumberFormatException ex) {

                        return ResponseEntity
                                        .status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of(
                                                        "error",
                                                        "La identidad de la sesión no es válida."));
                }

                try {

                        perfilClienteService.actualizarMiPerfil(
                                        usuarioId,
                                        request.nombre(),
                                        request.apellido(),
                                        request.telefono(),
                                        request.direccion());

                        return ResponseEntity.ok(Map.of(
                                        "mensaje",
                                        "Perfil actualizado correctamente.",
                                        "usuarioId",
                                        usuarioId));

                } catch (IllegalArgumentException ex) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        ex.getMessage() == null
                                                                        ? "Datos del perfil inválidos."
                                                                        : ex.getMessage()));
                }
        }

        // ==========================================
        // ACTUALIZAR PERFIL CLIENTE
        // ==========================================
        //
        // SecurityConfig comprueba que:
        // - JWT sea válido.
        // - La cuenta esté ACTIVA.
        // - El usuario sea titular del perfil.
        //
        // No se modifica contraseña ni rol.

        @PutMapping("/{usuarioId}")
        public ResponseEntity<?> actualizar(
                        @PathVariable("usuarioId") Long usuarioId,
                        @RequestBody Cliente cliente) {

                try {

                        Cliente actualizado = clienteService.update(
                                        usuarioId,
                                        cliente);

                        return ResponseEntity.ok(actualizado);

                } catch (IllegalArgumentException ex) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        ex.getMessage() == null
                                                                        ? "No fue posible actualizar el cliente."
                                                                        : ex.getMessage()));
                }
        }

        // ==========================================
        // ELIMINAR PERFIL CLIENTE
        // ==========================================
        //
        // Se conserva el método existente,
        // pero SecurityConfig no permite
        // esta operación actualmente.

        @DeleteMapping("/{usuarioId}")
        public ResponseEntity<?> eliminar(
                        @PathVariable("usuarioId") Long usuarioId) {

                try {

                        clienteService.deleteById(usuarioId);

                        return ResponseEntity.ok(
                                        "Cliente eliminado correctamente");

                } catch (IllegalArgumentException ex) {

                        return ResponseEntity
                                        .status(HttpStatus.NOT_FOUND)
                                        .body(Map.of(
                                                        "error",
                                                        ex.getMessage() == null
                                                                        ? "Cliente no encontrado."
                                                                        : ex.getMessage()));
                }
        }
}
