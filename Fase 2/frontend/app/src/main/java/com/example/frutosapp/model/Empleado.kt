package com.example.frutosapp.model

import java.time.LocalDate

data class Empleado(
  val usuarioId: Long? = null,
  val usuario: Usuario? = null,
  val puesto: String,
  val fechaContrato: String,
  val  sueldo: Double
) {

}