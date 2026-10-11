package com.example.usuario.service;

import java.util.List;
import java.util.Optional;

import com.example.usuario.entity.Empleado;

public interface EmpleadoService {

    List<Empleado> findAll();
    Optional<Empleado> findById(Long usuarioId);
    Empleado create(Empleado e);
    Empleado update(Long usuarioId, Empleado e);
    void deleteById(Long usuarioId);
}
