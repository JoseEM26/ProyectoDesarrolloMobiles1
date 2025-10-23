package com.example.computronica.Model

data class Asignatura(
    val id: String = "",
    val codigoAsignatura: String = "",
    val nombre: String = "",
    val descripcion: String? = null,
    val creditos: Int = 3,
    val profesores: List<String> = emptyList(),
    val estudiantes: List<String> = emptyList()
)