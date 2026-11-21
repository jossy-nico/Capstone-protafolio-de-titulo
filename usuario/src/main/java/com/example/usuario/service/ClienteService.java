package com.example.usuario.service;

import java.util.List;
import java.util.Optional;

import com.example.usuario.entity.Cliente;

public interface ClienteService {
    List<Cliente> findAll();
    Optional<Cliente> findById(Long usuarioId);
    Cliente create(Cliente c);
    Cliente update(Long usuarioId, Cliente c);
    void deleteById(Long usuarioId);
}
