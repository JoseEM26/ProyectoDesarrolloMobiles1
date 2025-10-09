package com.example.computronica.Model

import android.net.Uri

data class Usuario(
    val id: Int,
    val codigoInstitucional: String,
    val sede: String,
    val nombre: String,
    val apellido: String,
    val correoInstitucional: String,
    val contrasena: String,
    val tipo: TipoUsuario,
    val estado: Boolean = true,
    val imgURI:Uri?=null
)

enum class TipoUsuario {
    estudiante,
    profesor,
    administrativo
}
