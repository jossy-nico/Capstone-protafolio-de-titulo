package com.example.producto.services.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.producto.entity.Categoria;
import com.example.producto.repository.CategoriaRepository;
import com.example.producto.services.CategoriaService;

@Service
@Transactional
public class CategoriaServiceImpl implements CategoriaService {
    private final CategoriaRepository categoriaRepository;

    public CategoriaServiceImpl(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Categoria> findAll() {
        return categoriaRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Categoria> findById(Integer idCategoria) {
        return categoriaRepository.findById(idCategoria);
    }

    @Override
    public Categoria create(Categoria c) {
        return categoriaRepository.save(c);
    }

    @Override
    public Categoria update(Integer idCategoria, Categoria c) {
        Categoria existente = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada con ID: " + idCategoria));
        existente.setNomCategoria(c.getNomCategoria());
        return categoriaRepository.save(existente);
    }

    @Override
    public void deleteById(Integer idCategoria) {
        if (!categoriaRepository.existsById(idCategoria)) {
            throw new IllegalArgumentException("Categoría no encontrada con ID: " + idCategoria);
        }
        categoriaRepository.deleteById(idCategoria);
    }
}