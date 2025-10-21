package com.example.computronica.Model


    data class MessageModel(
        var messageId: String = "",
        var senderId: String = "",
        var message: String = "",
        var timestamp: Long = System.currentTimeMillis()
    ) {
        // Constructor vacío requerido por Firebase
        constructor() : this("", "", "", 0L)
    }