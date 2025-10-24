package com.example.computronica

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.computronica.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var lastClickTime = 0L
    private val debounceDelay = 500L
    private var currentFragment: Fragment? = null
    private var isLoading = false

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
            if (isLoading) {
                Log.d("MainActivity", "Navigation blocked due to loading")
                false
            } else if (currentTime - lastClickTime > debounceDelay) {
                lastClickTime = currentTime
                when (item.itemId) {
                    R.id.nav_inicio -> {
                        if (currentFragment !is DashBoardActivity) {
                            changeFrame(DashBoardActivity())
                            updateToolbar("Dashboard", "Bienvenido a Computrónica")
                        }
                        true
                    }
                    R.id.nav_asignatura -> {
                        if (currentFragment !is AsignaturaActivity) {
                            changeFrame(AsignaturaActivity())
                            updateToolbar("Asignaturas", "Gestión de cursos y docentes")
                        }
                        true
                    }
                    R.id.nav_calificaoiones -> {
                        if (currentFragment !is MisNotasFragment) {
                            changeFrame(MisNotasFragment())
                            updateToolbar("Calificaciones", "Mis calificaciones")
                        }
                        true
                    }
                    R.id.nav_more -> {
                        if (currentFragment !is MoreMenuNavActivity) {
                            changeFrame(MoreMenuNavActivity())
                            updateToolbar("Más opciones", "Configuraciones y soporte")
                        }
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
        binding.toolbarMain.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white))
        binding.toolbarMain.setSubtitleTextColor(ContextCompat.getColor(this, R.color.gris_claro))
    }

    fun changeFrame(fragment: Fragment) {
        scope.launch {
            try {
                if (!isFinishing && !supportFragmentManager.isStateSaved) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, fragment)
                        .commit()
                    currentFragment = fragment
                    // Hide loading when changing to a non-DashBoardFragment
                    if (fragment !is DashBoardActivity) {
                        setLoading(false)
                    }
                } else {
                    Log.w("MainActivity", "Cannot commit fragment transaction: Activity is finishing or state saved")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error in fragment transaction: ${e.message}")
            }
        }
    }

    fun setLoading(isLoading: Boolean) {
        this.isLoading = isLoading
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}