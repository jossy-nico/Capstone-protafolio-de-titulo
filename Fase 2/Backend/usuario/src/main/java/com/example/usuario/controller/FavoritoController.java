package com.example.usuario.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.usuario.entity.Favorito;
import com.example.usuario.service.FavoritoService;

import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/api/favoritos")
@RequiredArgsConstructor 
@CrossOrigin (origins = "*")
public class FavoritoController {

    private final FavoritoService favoritoService;

    // AGREGAR PRODUCTO A FAVORITOS
    @PostMapping ("/{idUsuario}/{idProducto}")
    public ResponseEntity<Favorito> agregarFavorito(
            @PathVariable("idUsuario") Long idUsuario,
            @PathVariable("idProducto") Integer idProducto
    ) {

        Favorito favorito =
                favoritoService.agregarFavorito(
                        idUsuario,
                        idProducto
                );

        return ResponseEntity.ok(favorito);
    }

    // LISTAR FAVORITOS DEL USUARIO
    @GetMapping ("/usuario/{idUsuario}")
    public ResponseEntity<List<Favorito>> listarFavoritos(
            @PathVariable("idUsuario") Long idUsuario
    ) {

        return ResponseEntity.ok(
                favoritoService.listarFavoritos(idUsuario)
        );
    }

    // SABER SI UN PRODUCTO ESTÁ EN FAVORITOS
    @GetMapping("/usuario/{idUsuario}/producto/{idProducto}")
    public ResponseEntity<Map<String, Boolean>> esFavorito(
            @PathVariable("idUsuario") Long idUsuario,
            @PathVariable("idProducto") Integer idProducto
    ) {

        boolean favorito =
                favoritoService.esFavorito(
                        idUsuario,
                        idProducto
                );

        return ResponseEntity.ok(
                Map.of("favorito", favorito)
        );
    }

    // ELIMINAR PRODUCTO DE FAVORITOS
    @DeleteMapping("/{idUsuario}/{idProducto}")
    public ResponseEntity<Void> eliminarFavorito(
            @PathVariable("idUsuario") Long idUsuario,
            @PathVariable("idProducto") Integer idProducto
    ) {

        favoritoService.eliminarFavorito(
                idUsuario,
                idProducto
        );

        return ResponseEntity.noContent().build();
    }
}
