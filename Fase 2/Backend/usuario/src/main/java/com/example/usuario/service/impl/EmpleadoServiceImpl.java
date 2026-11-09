package com.example.usuario.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.usuario.dto.EmpleadoCreateRequest;
import com.example.usuario.entity.Empleado;
import com.example.usuario.entity.Usuario;
import com.example.usuario.enums.TipoUsuario;
import com.example.usuario.repository.EmpleadoRepository;
import com.example.usuario.repository.UsuarioRepository;
import com.example.usuario.service.EmpleadoService;
import com.example.usuario.service.UsuarioService;

@Service
@Transactional
public class EmpleadoServiceImpl implements EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    public EmpleadoServiceImpl(
            EmpleadoRepository empleadoRepository,
            UsuarioRepository usuarioRepository,
            UsuarioService usuarioService) {

        this.empleadoRepository = empleadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Empleado> findAll() {
        return empleadoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Empleado> findById(Long usuarioId) {
        return empleadoRepository.findById(usuarioId);
    }

    @Override
    public Empleado create(Empleado e) {
        if (e.getUsuario() == null ||
                e.getUsuario().getIdUsuario() == null) {

            throw new IllegalArgumentException(
                    "Debe enviar el usuario con su idUsuario");
        }

        Usuario usuarioBD = usuarioRepository
                .findById(e.getUsuario().getIdUsuario())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado con id: "
                                + e.getUsuario().getIdUsuario()));

        if (usuarioBD.getTipoUsuario() != TipoUsuario.EMPLEADO) {
            throw new IllegalArgumentException(
                    "El usuario indicado no es de tipo EMPLEADO");
        }

        if (e.getPuesto() == null) {
            throw new IllegalArgumentException(
                    "El puesto del empleado es obligatorio");
        }

        e.setUsuario(usuarioBD);

        return empleadoRepository.save(e);
    }

    @Override
    public Empleado update(Long usuarioId, Empleado e) {
        Empleado existe = empleadoRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Empleado no encontrado"));

        if (e.getPuesto() == null) {
            throw new IllegalArgumentException(
                    "El puesto del empleado es obligatorio");
        }

        existe.setPuesto(e.getPuesto());
        existe.setFechaContrato(e.getFechaContrato());
        existe.setSueldo(e.getSueldo());

        return empleadoRepository.save(existe);
    }

    @Override
    public void deleteById(Long usuarioId) {
        if (!empleadoRepository.existsById(usuarioId)) {
            throw new IllegalArgumentException("Empleado no encontrado con el ID: " + usuarioId);
        }
        empleadoRepository.deleteById(usuarioId);
    }

    @Override
    public Empleado crearCompleto(EmpleadoCreateRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Los datos del empleado son obligatorios");
        }

        if (request.getPuesto() == null) {
            throw new IllegalArgumentException(
                    "El puesto del empleado es obligatorio");
        }

        if (request.getFechaContrato() == null) {
            throw new IllegalArgumentException(
                    "La fecha de contrato es obligatoria");
        }

        if (request.getSueldo() == null ||
                request.getSueldo() <= 0) {

            throw new IllegalArgumentException(
                    "El sueldo debe ser mayor a 0");
        }

        // Crear la cuenta del empleado
        Usuario usuario = new Usuario();

        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setCorreo(request.getCorreo());
        usuario.setPassword(request.getPassword());
        usuario.setTipoUsuario(TipoUsuario.EMPLEADO);

        /*
         * UsuarioService se encarga de:
         * - validar correo @frutossecos.cl
         * - verificar correo duplicado
         * - BCrypt
         * - estado ACTIVO
         * - debeCambiarPassword = true
         */
        Usuario usuarioCreado = usuarioService.create(usuario);

        // Crear la ficha laboral
        Empleado empleado = new Empleado();

        empleado.setUsuario(usuarioCreado);
        empleado.setPuesto(request.getPuesto());
        empleado.setFechaContrato(request.getFechaContrato());
        empleado.setSueldo(request.getSueldo());

        return empleadoRepository.save(empleado);
    }
}
