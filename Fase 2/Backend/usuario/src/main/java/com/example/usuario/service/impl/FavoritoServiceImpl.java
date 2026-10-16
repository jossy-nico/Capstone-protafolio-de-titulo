package com.example.usuario.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.usuario.entity.Favorito;
import com.example.usuario.repository.FavoritoRepository;
import com.example.usuario.service.FavoritoService;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class FavoritoServiceImpl implements FavoritoService {

    private final FavoritoRepository favoritoRepository;

    @Override
    @Transactional 
    public Favorito agregarFavorito(
            Long idUsuario,
            Integer idProducto
    ) {

        return favoritoRepository
                .findByIdUsuarioAndIdProducto(idUsuario, idProducto)
                .orElseGet(() -> {

                    Favorito favorito = new Favorito();

                    favorito.setIdUsuario(idUsuario);
                    favorito.setIdProducto(idProducto);

                    return favoritoRepository.save(favorito);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Favorito> listarFavoritos(
            Long idUsuario
    ) {

        return favoritoRepository.findByIdUsuario(idUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean esFavorito(
            Long idUsuario,
            Integer idProducto
    ) {

        return favoritoRepository
                .existsByIdUsuarioAndIdProducto(
                        idUsuario,
                        idProducto
                );
    }

    @Override
    @Transactional
    public void eliminarFavorito(
            Long idUsuario,
            Integer idProducto
    ) {

        favoritoRepository
                .findByIdUsuarioAndIdProducto(
                        idUsuario,
                        idProducto
                )
                .ifPresent(
                        favoritoRepository::delete
                );
    }
}
