package com.example.producto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.producto.entity.Categoria;

import java.util.List;


@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Integer>{
    //usando JPQL
    @Query("SELECT c FROM Categoria c WHERE c.nomCategoria = :nomCategoria")
    List<Categoria> buscarCategoria(@Param("nomCategoria") String nomCategoria);

}
