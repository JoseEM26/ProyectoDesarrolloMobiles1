package com.example.computronica.Model

import android.net.Uri

data class Usuario(
    var id: Int,
    var codigoInstitucional: String,
    var sede: String,
    var nombre: String,
    var apellido: String,
    var correoInstitucional: String,
    var contrasena: String,
    var tipo: TipoUsuario,
    var estado: Boolean = true,
    var imgURI:Uri?=null
)

enum class TipoUsuario {
    estudiante,
    profesor,
    administrativo
}
