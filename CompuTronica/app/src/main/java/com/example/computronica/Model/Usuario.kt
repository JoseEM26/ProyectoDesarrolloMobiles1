package com.example.computronica.Model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Usuario(
    var id: String = "",
    var codigoInstitucional: String = "",
    var sede: String = "",
    var nombre: String = "",
    var apellido: String = "",
    var correoInstitucional: String = "",
    var contrasena: String = "",
    var tipo: TipoUsuario = TipoUsuario.estudiante,
    var estado: Boolean = true,
    @ServerTimestamp var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null
)

enum class TipoUsuario {
    estudiante,
    profesor,
    administrativo
}
