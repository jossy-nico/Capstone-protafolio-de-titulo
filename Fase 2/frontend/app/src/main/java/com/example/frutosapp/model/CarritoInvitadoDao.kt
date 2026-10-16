package com.example.frutosapp.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy

@Dao
interface CarritoInvitadoDao {

    @Query("SELECT * FROM carrito_invitado")
    suspend fun obtenerTodo(): List<CarritoInvitadoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(item: CarritoInvitadoEntity)

    @Query("DELETE FROM carrito_invitado")
    suspend fun vaciar()

    @Query("DELETE FROM carrito_invitado WHERE productoId = :productoId")
    suspend fun eliminar(productoId: Int)

    @Query("""
        UPDATE carrito_invitado
        SET cantidad = :cantidad
        WHERE productoId = :productoId
    """)
    suspend fun actualizarCantidad(
        productoId: Int,
        cantidad: Int
    )

    @Query("SELECT * FROM carrito_invitado WHERE productoId = :productoId LIMIT 1")
    suspend fun obtenerPorProducto(productoId: Int): CarritoInvitadoEntity?
}