package com.example.producto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.producto.entity.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer>{
    //usando JPQL
    @Query("SELECT p FROM Producto p WHERE p.nomProducto = :nomProducto")
    List<Producto> buscarProductos(@Param("nomProducto") String nomProducto);

    List<Producto> findByCategoria_IdCategoria(Integer idCategoria);

    @Modifying
    @Query("""
        UPDATE Producto p
        SET p.stockGramos = p.stockGramos - :gramos
        WHERE p.idProductos = :productoId
    AND p.stockGramos >= :gramos
    """)
    int descontarStockEnGramos(@Param("productoId") Integer productoId,
                               @Param("gramos") Integer gramos);

    @Modifying
    @Query("""
        UPDATE Producto p
          SET p.stockGramos = p.stockGramos + :gramos
        WHERE p.idProductos = :idProducto
    """)
    int sumarStock(@Param("idProducto") Integer idProducto,
                   @Param("gramos") Integer gramos);
                   


}
