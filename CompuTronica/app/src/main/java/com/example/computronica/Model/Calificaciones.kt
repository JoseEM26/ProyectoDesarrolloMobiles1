package com.example.computronica.Model

import com.google.firebase.Timestamp

data class Calificaciones(
    var id: String = "",
    val estudianteId: String = "",
    val asignaturaId: String = "",
    val evaluacion: String = "",
    val nota: Double = 0.0,
    val fechaRegistro: String = ""
)

