package com.example.usuario.service;

import java.util.List;
import java.util.Optional;

import com.example.usuario.entity.Usuario;

public interface UsuarioService {
    List<Usuario> findAll();

    Optional<Usuario> findById(Long idUsuario);

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> login(String correo, String password);

    Usuario create(Usuario u);

    Usuario update(Long idUsuario, Usuario u);

    void deleteById(Long idUsuario);

}
