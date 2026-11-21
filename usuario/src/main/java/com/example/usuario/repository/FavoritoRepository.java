package com.example.usuario.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.usuario.entity.Favorito;

import java.util.List;
import java.util.Optional;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    List<Favorito> findByIdUsuario(Long idUsuario);

    Optional<Favorito> findByIdUsuarioAndIdProducto(
            Long idUsuario,
            Integer idProducto
    );

    boolean existsByIdUsuarioAndIdProducto(
            Long idUsuario,
            Integer idProducto
    );

    void deleteByIdUsuarioAndIdProducto(
            Long idUsuario,
            Integer idProducto
    );
}
