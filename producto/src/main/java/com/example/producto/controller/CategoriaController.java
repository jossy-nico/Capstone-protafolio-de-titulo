package com.example.producto.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.producto.entity.Categoria;
import com.example.producto.services.CategoriaService;

@RestController
@RequestMapping("/api/categorias")
@CrossOrigin(origins = "*")
public class CategoriaController {
    @Autowired
    private CategoriaService categoriaService;

    @GetMapping
    public ResponseEntity<List<Categoria>> listar(){
        List<Categoria> categorias = categoriaService.findAll();
        return ResponseEntity.ok(categorias);
    }

    @GetMapping("/{idCategoria}")
    public ResponseEntity<Categoria> buscar(@PathVariable Integer idCategoria){
        try {
            Categoria categoria = categoriaService.findById(idCategoria).orElseThrow(() -> new IllegalArgumentException("Categoria no encontrada con ID: "+ idCategoria));
            return ResponseEntity.ok(categoria);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Categoria> crear(@RequestBody Categoria categoria){
        Categoria nuevaCategoria = categoriaService.create(categoria);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaCategoria);
    }

    @PutMapping("/{idCategoria}")
    public ResponseEntity<Categoria> actualizar(@PathVariable Integer idCategoria, @RequestBody Categoria categoria){
        try {
            Categoria actualizada = categoriaService.update(idCategoria, categoria);
            return ResponseEntity.ok(actualizada);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{idCategoria}")
    public ResponseEntity<?> eliminar(@PathVariable Integer idCategoria){
        try {
            categoriaService.deleteById(idCategoria);
            return ResponseEntity.ok("Categoria eliminada correctamente");
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
