package com.example.producto.services;

import java.util.List;
import java.util.Optional;

import com.example.producto.entity.Categoria;

public interface CategoriaService {
    List<Categoria> findAll();
    Optional<Categoria> findById(Integer idCategoria);
    Categoria create(Categoria c);
    Categoria update(Integer idCategoria, Categoria c);
    void deleteById(Integer idCategoria);
}
