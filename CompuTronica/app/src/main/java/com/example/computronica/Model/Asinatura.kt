package com.example.computronica.Model

import java.io.Serializable

data class Asignatura(
    val id: String = "",
    val nombre: String = "",
    val codigoAsignatura: String = "",
    val descripcion: String = "",
    val creditos: Int = 0,
    val profesores: List<String> = emptyList(),
    val estudiantes: List<String> = emptyList()
) : Serializable