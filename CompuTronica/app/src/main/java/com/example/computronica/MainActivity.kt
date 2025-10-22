package com.example.computronica

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.computronica.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var lastClickTime = 0L
    private val debounceDelay = 500L // 500ms debounce

    override fun onCreate(savedInstanceState: Bundle?) {
        // Check user session
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

        // Configure toolbar
        updateToolbar("Dashboard", "Bienvenido a Computrónica")

        // Set up bottom navigation with debouncing
        binding.bottomNav.setOnItemSelectedListener { item ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > debounceDelay) {
                lastClickTime = currentTime
                when (item.itemId) {
                    R.id.nav_inicio -> {
                        changeFrame(DashBoardActivity())
                        updateToolbar("Dashboard", "Bienvenido a Computrónica")
                        true
                    }
                    R.id.nav_asignatura -> {
                        changeFrame(AsignaturaActivity())
                        updateToolbar("Asignaturas", "Gestión de cursos y docentes")
                        true
                    }
                    R.id.nav_calificaoiones -> {
                        changeFrame(CalificacionActivity())
                        updateToolbar("Calificaciones", "Consulta y registro de notas")
                        true
                    }
                    R.id.nav_more -> {
                        changeFrame(MoreMenuNavActivity())
                        updateToolbar("Más opciones", "Configuraciones y soporte")
                        true
                    }
                    else -> false
                }
            } else {
                false
            }
        }

        // Set default fragment
        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_inicio
        }
    }

    private fun updateToolbar(title: String, subtitle: String) {
        binding.toolbarMain.title = title
        binding.toolbarMain.subtitle = subtitle
    }

    fun changeFrame(fragment: Fragment) {
        scope.launch {
            try {
                if (!isFinishing && !supportFragmentManager.isStateSaved) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, fragment)
                        .commitNow()
                }
            } catch (e: IllegalStateException) {
                // Log or handle state loss gracefully
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}