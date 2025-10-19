package com.example.computronica.Model

import java.util.Date

data class Calificaciones (
    var id: String = "",
    val estudianteId: Int,
    val asignaturaId: Int,
    val evaluacion: String, // "Parcial" o "Final"
    val nota: Double,
    val fechaRegistro: Date = Date()
)