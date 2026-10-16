package com.example.frutosapp.model

import com.google.gson.annotations.SerializedName

data class Producto(
    val idProductos: Int? = null,
    val nomProducto: String,
    val descripcion: String,
    val precioKilo: Double,
    val precio250g: Double,
    val stockGramos: Int,
    val categoria: Categoria,
    val tablaNutricional: String? = null,

    @SerializedName("imageUrl")
    val imageUrl: String? = null,

    @SerializedName("imagePublicId")
    val imagePublicId: String? = null,
    val ingredientes: String? = null,
    val alergenos: String? = null

)
