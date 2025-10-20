package com.example.computronica

import com.example.computronica.Model.Usuario

object SessionManager {
    var currentUser: Usuario? = null
    var userId: String? = null

    fun clearSession() {
        currentUser = null
        userId = null
    }
}
