package com.example.usuario.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.usuario.entity.Cliente;
import com.example.usuario.entity.Usuario;
import com.example.usuario.enums.TipoUsuario;
import com.example.usuario.repository.ClienteRepository;
import com.example.usuario.repository.UsuarioRepository;
import com.example.usuario.service.ClienteService;

@Service
@Transactional
public class ClienteServiceImpl implements ClienteService{

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    public ClienteServiceImpl(ClienteRepository clienteRepository, UsuarioRepository usuarioRepository){
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> findAll(){
        return clienteRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cliente> findById(Long usuarioId){
        return clienteRepository.findById(usuarioId);
    }

    @Override
    public Cliente create(Cliente c){
        if (c.getUsuario() == null ||
                c.getUsuario().getIdUsuario() == null) {

            throw new IllegalArgumentException(
                    "Debe enviar el usuario con su idUsuario"
            );
        }

        Usuario usuario = usuarioRepository
                .findById(c.getUsuario().getIdUsuario())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Usuario no encontrado con id: "
                                        + c.getUsuario().getIdUsuario()
                        )
                );

        if (usuario.getTipoUsuario() != TipoUsuario.CLIENTE) {
            throw new IllegalArgumentException(
                    "El usuario indicado no es de tipo CLIENTE"
            );
        }
        validarTelefono(c.getTelefono());

        c.setUsuario(usuario);

        return clienteRepository.save(c);
    }

    @Override
    public Cliente update(Long usuarioId, Cliente c){
        Cliente existe = clienteRepository.findById(usuarioId)
        .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        validarTelefono(c.getTelefono());

        existe.setTelefono(c.getTelefono().trim());
        existe.setDireccion(c.getDireccion());

        return clienteRepository.save(existe);
    }

    @Override
    public void deleteById(Long usuarioId){
        if (!clienteRepository.existsById(usuarioId)) {
            throw new IllegalArgumentException("Cliente no encontrado con el ID: "+ usuarioId);
        }
        clienteRepository.deleteById(usuarioId);
    }

    public void validarTelefono(String telefono){
        if (telefono == null || telefono.isBlank()) {
            throw new IllegalArgumentException(
                "el teléfono es obligatorio"
            );
        }
        String telefonoLimpio = telefono.trim();

        if (!telefonoLimpio.matches("9\\d{8}$")) {
            throw new IllegalArgumentException(
                "El teléfono debe contener 9 dígitos y comenzar con 9"
            );
        }
    }
}
