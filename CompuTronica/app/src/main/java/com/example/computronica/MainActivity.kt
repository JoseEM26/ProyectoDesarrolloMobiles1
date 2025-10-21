package com.example.computronica

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.computronica.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        val usuario = SessionManager.currentUser
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (usuario == null || firebaseUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarMain)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        // Configurar toolbar base
        binding.toolbarMain.title = "Dashboard"
        binding.toolbarMain.subtitle = "Bienvenido a Computrónica"

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    changeFrame(DashBoardActivity())
                    updateToolbar("Dashboard", "Bienvenido a Computrónica")
                }
                R.id.nav_asignatura -> {
                    changeFrame(AsignaturaActivity())
                    updateToolbar("Asignaturas", "Gestión de cursos y docentes")
                }
                R.id.nav_calificaoiones -> {
                    changeFrame(CalificacionActivity())
                    updateToolbar("Calificaciones", "Consulta y registro de notas")
                }
                R.id.nav_more -> {
                    changeFrame(MoreMenuNavActivity())
                    updateToolbar("Más opciones", "Configuraciones y soporte")
                }
                else -> false
            }
            true
        }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_inicio
        }
    }

    private fun updateToolbar(title: String, subtitle: String) {
        binding.toolbarMain.title = title
        binding.toolbarMain.subtitle = subtitle
    }

    fun changeFrame(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .commit()
    }
}
