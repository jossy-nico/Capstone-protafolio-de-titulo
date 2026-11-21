package com.example.producto.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.producto.entity.Producto;
import com.example.producto.services.CloudinaryService;
import com.example.producto.services.ProductoService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
public class ProductoController {

    private final CloudinaryService cloudinaryService;
    private final ProductoService productoService;

    ProductoController(CloudinaryService cloudinaryService, ProductoService productoService) {
        this.cloudinaryService = cloudinaryService;
        this.productoService = productoService;
    }

    @GetMapping
    public ResponseEntity<List<Producto>> listar() {
        List<Producto> productos = productoService.findAll();
        return ResponseEntity.ok(productos);
    }

    @GetMapping("/categoria/{idCategoria}")
    public ResponseEntity<List<Producto>> listarPorCategoria(
            @PathVariable("idCategoria") Integer idCategoria) {

        List<Producto> productos = productoService.findByCategoria(idCategoria);

        return ResponseEntity.ok(productos);
    }

    @GetMapping("/{idProductos}")
    public ResponseEntity<Producto> buscar(@PathVariable("idProductos") Integer idProductos) {
        try {
            Producto producto = productoService.findById(idProductos)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + idProductos));
            return ResponseEntity.ok(producto);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Producto> crear(@RequestBody Producto producto) {
        Producto nuevoProducto = productoService.create(producto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoProducto);
    }

    @PutMapping("/{idProductos}")
    public ResponseEntity<Producto> actualizar(
            @PathVariable("idProductos") Integer idProductos,
            @RequestBody Producto producto) {
        try {
            Producto actualizado = productoService.update(idProductos, producto);
            return ResponseEntity.ok(actualizado);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{idProductos}")
    public ResponseEntity<?> eliminar(@PathVariable("idProductos") Integer idProductos) {
        try {
            productoService.deleteById(idProductos);
            return ResponseEntity.ok("Producto eliminado correctamente");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }

    // Subir imagen a Cloudinary y guardar imageUrl + imagePublicId
    @PostMapping("/{idProductos}/imagen")
    public ResponseEntity<?> subirImagen(
            @PathVariable("idProductos") Integer idProductos,
            @RequestParam("file") MultipartFile file) {
        try {
            Producto producto = productoService.findById(idProductos)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + idProductos));

            // Si ya tenía imagen, borrar la anterior en Cloudinary
            if (producto.getImagePublicId() != null && !producto.getImagePublicId().isBlank()) {
                cloudinaryService.deleteByPublicId(producto.getImagePublicId());
            }

            Map result = cloudinaryService.uploadImage(file, "productos");

            String imageUrl = result.get("secure_url").toString();
            String publicId = result.get("public_id").toString();

            producto.setImageUrl(imageUrl);
            producto.setImagePublicId(publicId);

            Producto actualizado = productoService.update(idProductos, producto);
            return ResponseEntity.ok(actualizado);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al subir imagen: " + e.getMessage());
        }
    }
    // Inventario

    @PostMapping("/{id}/stock/descontar")
    public ResponseEntity<String> descontar(
            @PathVariable("id") Integer id,
            @RequestParam("gramos") Integer gramos) {
        productoService.descuentoStock(id, gramos);
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/{id}/stock/sumar")
    public ResponseEntity<String> sumar(
            @PathVariable("id") Integer id,
            @RequestParam("gramos") Integer gramos) {
        productoService.sumarStock(id, gramos);
        return ResponseEntity.ok("OK");
    }

}
