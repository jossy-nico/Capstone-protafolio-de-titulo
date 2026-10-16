package com.example.producto.services.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.producto.entity.Producto;
import com.example.producto.repository.ProductoRepository;
import com.example.producto.services.ProductoService;

@Service
@Transactional
public class ProductoServiceImpl implements ProductoService{

    private final ProductoRepository productoRepository;

    public ProductoServiceImpl(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> findAll() {
        return productoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Producto> findById(Integer idProductos) {
        return productoRepository.findById(idProductos);
    }
    @Override
    @Transactional(readOnly = true)
    public List<Producto> findByCategoria(Integer idCategoria) {
        return productoRepository.findByCategoria_IdCategoria(idCategoria);
    }

    @Override
    public Producto create(Producto p) {

        if (p.getPrecioKilo() == null || p.getPrecioKilo() < 0) {
            throw new IllegalArgumentException("El precio por kilo es obligatorio y no puede ser negativo.");
        }

        if (p.getStockGramos() == null || p.getStockGramos() < 0) {
            throw new IllegalArgumentException("El stock en gramos es obligatorio y no puede ser negativo.");
        }

        // Asegura que el ID venga nulo para crear
        p.setIdProductos(null);

        // Calcula automáticamente el precio por 250g
        p.setPrecio250g((double) Math.round(p.getPrecioKilo() / 4.0));

        return productoRepository.save(p);
    }

    @Override
    public Producto update(Integer idProductos, Producto p) {
        Producto existente = productoRepository.findById(idProductos)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + idProductos));
        if (p.getPrecioKilo() != null && p.getPrecioKilo() < 0) {
            throw new IllegalArgumentException("El precio por kilo no puede ser negativo.");
        }

        if (p.getNomProducto() != null) existente.setNomProducto(p.getNomProducto());
        if (p.getPrecioKilo() != null) {
            existente.setPrecioKilo(p.getPrecioKilo());
            existente.setPrecio250g((double) Math.round(p.getPrecioKilo() / 4.0));
        }
        if (p.getDescripcion() != null) existente.setDescripcion(p.getDescripcion());

        if (p.getStockGramos() != null) existente.setStockGramos(p.getStockGramos());
        if (p.getCategoria() != null) existente.setCategoria(p.getCategoria());

        if (p.getImageUrl() != null) existente.setImageUrl(p.getImageUrl());
        if (p.getImagePublicId() != null) existente.setImagePublicId(p.getImagePublicId());
        if (p.getTablaNutricional() != null) {
            existente.setTablaNutricional(p.getTablaNutricional());
        }
        if (p.getIngredientes() != null) {
            existente.setIngredientes(p.getIngredientes());
        }
        if (p.getAlergenos() != null) {
            existente.setAlergenos(p.getAlergenos());
        }
        return productoRepository.save(existente);
    }
    

    @Override
    public void deleteById(Integer idProductos) {
        if (!productoRepository.existsById(idProductos)) {
            throw new IllegalArgumentException("Producto no encontrado con ID: " + idProductos);
        }
        productoRepository.deleteById(idProductos);
    }

    @Override
    public void descuentoStock(Integer idProducto, Integer gramos) {

        if (gramos == null || gramos <= 0) {
            throw new IllegalArgumentException("Los gramos deben ser mayores a 0");
        }

        // Si quieres, valida que exista el producto (opcional, pero útil)
        if (!productoRepository.existsById(idProducto)) {
            throw new IllegalArgumentException("Producto no encontrado con ID: " + idProducto);
        }

        int updated = productoRepository.descontarStockEnGramos(idProducto, gramos);
        if (updated == 0) {
            throw new IllegalArgumentException("Stock insuficiente para el producto ID: " + idProducto);
        }
    }
    @Override
    public void sumarStock(Integer idProducto, Integer gramos) {
        if (gramos == null || gramos <= 0) throw new IllegalArgumentException("Gramos inválidos");
        if (!productoRepository.existsById(idProducto)) {
            throw new IllegalArgumentException("Producto no encontrado con ID: " + idProducto);
        }
        productoRepository.sumarStock(idProducto, gramos);
    }
}
