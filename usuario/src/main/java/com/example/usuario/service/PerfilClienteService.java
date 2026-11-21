
package com.example.usuario.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.usuario.entity.Cliente;
import com.example.usuario.entity.Usuario;

@Service
public class PerfilClienteService {

    private final UsuarioService usuarioService;
    private final ClienteService clienteService;

    public PerfilClienteService(
            UsuarioService usuarioService,
            ClienteService clienteService) {

        this.usuarioService = usuarioService;
        this.clienteService = clienteService;
    }

    // ==========================================
    // ACTUALIZAR PERFIL COMPLETO
    // ==========================================
    // Ambas actualizaciones se ejecutan
    // dentro de una misma transacción.
    // ==========================================

    @Transactional
    public void actualizarMiPerfil(
            Long usuarioId,
            String nombre,
            String apellido,
            String telefono,
            String direccion) {

        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException(
                    "La identidad del cliente no es válida.");
        }

        if (nombre == null || nombre.trim().length() < 2) {
            throw new IllegalArgumentException(
                    "El nombre debe tener al menos 2 caracteres.");
        }

        if (apellido == null || apellido.trim().length() < 2) {
            throw new IllegalArgumentException(
                    "El apellido debe tener al menos 2 caracteres.");
        }

        if (telefono == null ||
                !telefono.trim().matches("9\\d{8}")) {

            throw new IllegalArgumentException(
                    "El teléfono debe tener 9 dígitos y comenzar con 9.");
        }

        if (direccion == null || direccion.isBlank()) {
            throw new IllegalArgumentException(
                    "La dirección es obligatoria.");
        }

        // ======================================
        // 1. ACTUALIZAR NOMBRE Y APELLIDO
        // ======================================
        // UsuarioService valida adicionalmente
        // que la cuenta sea CLIENTE y ACTIVA.

        Usuario usuario = usuarioService.actualizarMiPerfil(
                usuarioId,
                nombre.trim(),
                apellido.trim());

        // ======================================
        // 2. ACTUALIZAR TELÉFONO Y DIRECCIÓN
        // ======================================

        Cliente datosCliente = new Cliente();

        datosCliente.setTelefono(telefono.trim());
        datosCliente.setDireccion(direccion.trim());

        clienteService.update(
                usuario.getIdUsuario(),
                datosCliente);

        // Si falla cualquiera de los pasos,
        // Spring revierte toda la transacción.
    }
}
