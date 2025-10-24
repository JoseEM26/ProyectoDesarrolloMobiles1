package com.example.computronica.Model

import com.google.firebase.Timestamp
import java.io.Serializable

data class Tema(
    val id: String = "",
    val asignaturaId: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val estado: Boolean = true,
    val fechaCreacion: Timestamp? = null
) : Serializable