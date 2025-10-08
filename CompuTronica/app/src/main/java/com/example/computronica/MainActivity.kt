package com.example.computronica

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.computronica.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar el botón para redirigir
        binding.btnRedirigirUsuarios.setOnClickListener {
            // Crear el Intent para redirigir a Usuarios_Activity
            val intent = Intent(this, Usuarios_Activity::class.java)
            startActivity(intent)  // Iniciar la actividad
        }
    }
}
