package com.example.frutosapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "carrito_invitado")
data class CarritoInvitadoEntity(
    @PrimaryKey
    val productoId: Int,
    val cantidad: Int
)