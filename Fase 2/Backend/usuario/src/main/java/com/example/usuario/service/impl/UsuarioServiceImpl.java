
package com.example.usuario.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.time.Instant;
import com.example.usuario.repository.SesionRefreshRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.usuario.entity.Usuario;
import com.example.usuario.enums.EstadoUsuario;
import com.example.usuario.enums.TipoUsuario;
import com.example.usuario.repository.UsuarioRepository;
import com.example.usuario.service.UsuarioService;

@Service
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

        private final UsuarioRepository usuarioRepository;
        private final PasswordEncoder passwordEncoder;
        private final SesionRefreshRepository sesionRefreshRepository;

        public UsuarioServiceImpl(
                        UsuarioRepository usuarioRepository,
                        PasswordEncoder passwordEncoder,
                        SesionRefreshRepository sesionRefreshRepository) {

                this.usuarioRepository = usuarioRepository;
                this.passwordEncoder = passwordEncoder;
                this.sesionRefreshRepository = sesionRefreshRepository;
        }

        // ==========================================
        // CONSULTAS
        // ==========================================

        @Override
        @Transactional(readOnly = true)
        public List<Usuario> findAll() {
                return usuarioRepository.findAll();
        }

        @Override
        @Transactional(readOnly = true)
        public Optional<Usuario> findById(Long idUsuario) {
                return usuarioRepository.findById(idUsuario);
        }

        @Override
        @Transactional(readOnly = true)
        public Optional<Usuario> findByCorreo(String correo) {

                if (correo == null || correo.isBlank()) {
                        return Optional.empty();
                }

                return usuarioRepository.findByCorreo(
                                normalizarCorreo(correo));
        }

        // ==========================================
        // CREAR USUARIO
        // ==========================================

        @Override
        public Usuario create(Usuario u) {

                if (u == null) {
                        throw new IllegalArgumentException(
                                        "El usuario no puede ser nulo");
                }

                if (u.getTipoUsuario() == null) {
                        throw new IllegalArgumentException(
                                        "El tipo de usuario es obligatorio");
                }

                validarNombre(u.getNombre(), "nombre");
                validarNombre(u.getApellido(), "apellido");

                validarCorreo(
                                u.getCorreo(),
                                u.getTipoUsuario());

                String correoNormalizado = normalizarCorreo(u.getCorreo());

                if (usuarioRepository
                                .findByCorreo(correoNormalizado)
                                .isPresent()) {

                        throw new IllegalArgumentException(
                                        "Ya existe un usuario con este correo");
                }

                validarPasswordNueva(u.getPassword());

                u.setNombre(u.getNombre().trim());
                u.setApellido(u.getApellido().trim());
                u.setCorreo(correoNormalizado);

                // Nunca guardar contraseñas en texto plano.
                u.setPassword(
                                passwordEncoder.encode(u.getPassword()));

                // Las cuentas nuevas comienzan activas.
                u.setEstado(EstadoUsuario.ACTIVO);

                // Los empleados cambian su contraseña temporal.
                u.setDebeCambiarPassword(
                                u.getTipoUsuario() == TipoUsuario.EMPLEADO);

                return usuarioRepository.save(u);
        }

        // ==========================================
        // ACTUALIZAR DATOS PERSONALES
        // NO MODIFICAR ROLES NI CONTRASEÑAS
        // ==========================================

        @Override
        public Usuario update(Long idUsuario, Usuario u) {

                if (u == null) {
                        throw new IllegalArgumentException(
                                        "Los datos del usuario son obligatorios");
                }

                Usuario existe = usuarioRepository
                                .findById(idUsuario)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Usuario no encontrado"));

                // Impedir cambios de rol mediante UPDATE general.
                if (u.getTipoUsuario() != null
                                && u.getTipoUsuario() != existe.getTipoUsuario()) {

                        throw new IllegalArgumentException(
                                        "No está permitido cambiar el rol mediante " +
                                                        "la actualización general del usuario");
                }

                // La contraseña solo se cambia por el endpoint
                // que solicita y verifica la contraseña actual.
                if (u.getPassword() != null
                                && !u.getPassword().isBlank()) {

                        throw new IllegalArgumentException(
                                        "Utiliza el proceso de cambio de contraseña");
                }

                validarNombre(u.getNombre(), "nombre");
                validarNombre(u.getApellido(), "apellido");

                // Mantener el rol actual al validar el correo.
                validarCorreo(
                                u.getCorreo(),
                                existe.getTipoUsuario());

                String correoNormalizado = normalizarCorreo(u.getCorreo());

                Optional<Usuario> propietarioCorreo = usuarioRepository.findByCorreo(
                                correoNormalizado);

                if (propietarioCorreo.isPresent()
                                && !propietarioCorreo.get()
                                                .getIdUsuario()
                                                .equals(idUsuario)) {

                        throw new IllegalArgumentException(
                                        "Ya existe otro usuario con este correo");
                }

                existe.setNombre(u.getNombre().trim());
                existe.setApellido(u.getApellido().trim());
                existe.setCorreo(correoNormalizado);

                // NO modificar:
                // - password
                // - tipoUsuario
                // - estado
                // - debeCambiarPassword

                return usuarioRepository.save(existe);
        }

        // ==========================================
        // ACTUALIZAR MI PERFIL
        // SOLO TITULAR CLIENTE ACTIVO
        // ==========================================

        @Override
        public Usuario actualizarMiPerfil(
                        Long idUsuario,
                        String nombre,
                        String apellido) {

                Usuario usuario = usuarioRepository
                                .findById(idUsuario)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Usuario no encontrado."));

                if (usuario.getEstado() != EstadoUsuario.ACTIVO
                                || usuario.getTipoUsuario() != TipoUsuario.CLIENTE) {

                        throw new IllegalArgumentException(
                                        "La cuenta no está autorizada.");
                }

                validarNombre(nombre, "nombre");
                validarNombre(apellido, "apellido");

                usuario.setNombre(nombre.trim());
                usuario.setApellido(apellido.trim());

                // No modificar correo, contraseña,
                // rol, estado ni configuración de cuenta.

                return usuarioRepository.save(usuario);
        }

        // ==========================================
        // ELIMINACIÓN
        // ==========================================

        @Override
        public void deleteById(Long idUsuario) {

                // Natur Cosecha utiliza suspensión de cuentas
                // para conservar trazabilidad e historial.
                //
                // No permitir eliminación física mediante
                // este servicio general.

                throw new UnsupportedOperationException(
                                "La eliminación física de usuarios no está permitida. " +
                                                "Utiliza la suspensión de la cuenta.");
        }

        // ==========================================
        // NORMALIZACIÓN DE CORREO
        // ==========================================

        private String normalizarCorreo(String correo) {

                if (correo == null) {
                        return null;
                }

                return correo.trim()
                                .toLowerCase(Locale.ROOT);
        }

        // ==========================================
        // VALIDACIONES DE IDENTIDAD
        // ==========================================

        private void validarNombre(
                        String valor,
                        String campo) {

                if (valor == null || valor.isBlank()) {
                        throw new IllegalArgumentException(
                                        "El " + campo + " es obligatorio");
                }

                if (valor.trim().length() > 100) {
                        throw new IllegalArgumentException(
                                        "El " + campo + " supera 100 caracteres");
                }
        }

        // ==========================================
        // VALIDACIÓN DE CORREO
        // ==========================================

        private void validarCorreo(
                        String correo,
                        TipoUsuario tipoUsuario) {

                if (correo == null || correo.isBlank()) {
                        throw new IllegalArgumentException(
                                        "El correo es obligatorio");
                }

                String correoLimpio = normalizarCorreo(correo);

                String regex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

                if (!correoLimpio.matches(regex)) {
                        throw new IllegalArgumentException(
                                        "El correo no tiene un formato válido");
                }

                if (correoLimpio.length() > 254) {
                        throw new IllegalArgumentException(
                                        "El correo supera la longitud permitida");
                }

                if (tipoUsuario == TipoUsuario.EMPLEADO
                                && !correoLimpio.endsWith(
                                                "@frutossecos.cl")) {

                        throw new IllegalArgumentException(
                                        "El correo de un empleado debe terminar " +
                                                        "en @frutossecos.cl");
                }

                validarDominioConocido(correoLimpio);
        }

        private void validarDominioConocido(String correo) {

                String dominio = correo.substring(
                                correo.indexOf("@") + 1);

                switch (dominio) {

                        case "gmail.co":
                                throw new IllegalArgumentException(
                                                "El dominio gmail.co no es válido. " +
                                                                "¿Quiso escribir gmail.com?");

                        case "hotmail.co":
                                throw new IllegalArgumentException(
                                                "El dominio hotmail.co no es válido. " +
                                                                "¿Quiso escribir hotmail.com?");

                        case "outlook.co":
                                throw new IllegalArgumentException(
                                                "El dominio outlook.co no es válido. " +
                                                                "¿Quiso escribir outlook.com?");

                        default:
                                break;
                }
        }

        // ==========================================
        // VALIDACIÓN DE CONTRASEÑA NUEVA
        // ==========================================

        private void validarPasswordNueva(String password) {

                if (password == null || password.isBlank()) {
                        throw new IllegalArgumentException(
                                        "La contraseña es obligatoria");
                }

                // Política inicial compatible con el sistema.
                // Puede reforzarse posteriormente.
                if (password.length() < 8) {
                        throw new IllegalArgumentException(
                                        "La contraseña debe tener al menos 8 caracteres");
                }

                if (password.length() > 72) {
                        throw new IllegalArgumentException(
                                        "La contraseña no puede superar 72 caracteres");
                }
        }

        // ==========================================
        // LOGIN
        // ==========================================

        @Override
        @Transactional(readOnly = true)
        public Optional<Usuario> login(
                        String correo,
                        String password) {

                if (correo == null || correo.isBlank()
                                || password == null || password.isBlank()) {

                        return Optional.empty();
                }

                String correoNormalizado = normalizarCorreo(correo);

                return usuarioRepository
                                .findByCorreo(correoNormalizado)

                                .filter(usuario -> usuario.getEstado() == EstadoUsuario.ACTIVO)

                                .filter(usuario -> passwordEncoder.matches(
                                                password,
                                                usuario.getPassword()));
        }

        // ==========================================
        // SUSPENDER USUARIO
        // ==========================================

        @Override
        public Usuario suspender(Long idUsuario) {

                Usuario usuario = usuarioRepository
                                .findById(idUsuario)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Usuario no encontrado con ID: "
                                                                + idUsuario));

                if (usuario.getEstado() == EstadoUsuario.SUSPENDIDO) {
                        return usuario;
                }

                usuario.setEstado(EstadoUsuario.SUSPENDIDO);

                return usuarioRepository.save(usuario);
        }

        // ==========================================
        // REACTIVAR USUARIO
        // ==========================================

        @Override
        public Usuario reactivar(Long idUsuario) {

                Usuario usuario = usuarioRepository
                                .findById(idUsuario)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Usuario no encontrado con ID: "
                                                                + idUsuario));

                if (usuario.getEstado() == EstadoUsuario.ACTIVO) {
                        return usuario;
                }

                usuario.setEstado(EstadoUsuario.ACTIVO);

                return usuarioRepository.save(usuario);
        }

        // ==========================================
        // CAMBIAR CONTRASEÑA
        // ==========================================
        // Valida la contraseña actual,
        // protege la nueva con BCrypt y revoca
        // todas las sesiones persistentes.
        // ==========================================

        @Override
        @Transactional
        public Usuario cambiarPassword(
                        Long idUsuario,
                        String passwordActual,
                        String passwordNueva) {

                Usuario usuario = usuarioRepository
                                .findById(idUsuario)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Usuario no encontrado."));

                if (usuario.getEstado() != EstadoUsuario.ACTIVO) {

                        throw new IllegalArgumentException(
                                        "La cuenta no está activa.");
                }

                if (passwordActual == null
                                || passwordActual.isBlank()) {

                        throw new IllegalArgumentException(
                                        "La contraseña actual es obligatoria.");
                }

                validarPasswordNueva(passwordNueva);

                if (!passwordEncoder.matches(
                                passwordActual,
                                usuario.getPassword())) {

                        throw new IllegalArgumentException(
                                        "La contraseña actual es incorrecta.");
                }

                if (passwordEncoder.matches(
                                passwordNueva,
                                usuario.getPassword())) {

                        throw new IllegalArgumentException(
                                        "La nueva contraseña debe ser diferente a la actual.");
                }

                // ======================================
                // 1. GUARDAR NUEVA CONTRASEÑA
                // ======================================

                usuario.setPassword(
                                passwordEncoder.encode(passwordNueva));

                usuario.setDebeCambiarPassword(false);

                Usuario actualizado = usuarioRepository.save(usuario);

                // ======================================
                // 2. REVOCAR TODAS LAS SESIONES
                // ======================================
                // Se ejecuta en la misma transacción.
                // Si falla la revocación, se revierte
                // también el cambio de contraseña.
                // ======================================

                sesionRefreshRepository.revocarSesionesDeUsuario(
                                idUsuario,
                                Instant.now());

                return actualizado;
        }

}
