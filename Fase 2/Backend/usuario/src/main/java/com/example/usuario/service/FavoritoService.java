package com.example.usuario.service;


import java.util.List;

import com.example.usuario.entity.Favorito;

public interface FavoritoService {

    Favorito agregarFavorito(
            Long idUsuario,
            Integer idProducto
    );

    List<Favorito> listarFavoritos(
            Long idUsuario
    );

    boolean esFavorito(
            Long idUsuario,
            Integer idProducto
    );

    void eliminarFavorito(
            Long idUsuario,
            Integer idProducto
    );
}
