package com.example.computronica.Model

data class UserModel(
    var userID: String = "",
    var userName: String = "",
    var userEmail: String = ""
) {
    // Constructor vacío requerido por Firebase
    constructor() : this("", "", "")
}