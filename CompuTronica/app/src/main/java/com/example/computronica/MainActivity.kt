package com.example.computronica

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.computronica.Model.TipoUsuario
import com.example.computronica.Model.Usuario
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
        // Verificar sesión
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

        // Configurar navegación según el tipo de usuario
        setupBottomNavigation(usuario)

        // Fragmento por defecto
        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_inicio
            changeFrame(DashBoardActivity())
            updateToolbar("Dashboard", "Bienvenido a Computrónica")
        }
    }

    private fun setupBottomNavigation(usuario: Usuario) {
        // Limpiar menú actual
        binding.bottomNav.menu.clear()

        // Inflar menú base (común)
        when (usuario.tipo) {
            TipoUsuario.administrativo -> {
                binding.bottomNav.inflateMenu(R.menu.bottom_nav_admin)
            }
            TipoUsuario.estudiante, TipoUsuario.profesor -> {
                binding.bottomNav.inflateMenu(R.menu.bottom_nav_student_teacher)
            }
        }

        // Configurar listener
        binding.bottomNav.setOnItemSelectedListener { item ->
            val currentTime = System.currentTimeMillis()
            if (isLoading) {
                Log.d("MainActivity", "Navegación bloqueada por carga")
                return@setOnItemSelectedListener false
            }
            if (currentTime - lastClickTime <= debounceDelay) {
                return@setOnItemSelectedListener false
            }
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
                R.id.nav_chat -> {
                    if (currentFragment !is ChatFragment) {
                        changeFrame(ChatFragment())
                        updateToolbar("Chat", "Mensajes con docentes")
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
                        .commitAllowingStateLoss() // Más seguro
                    currentFragment = fragment
                    if (fragment !is DashBoardActivity) {
                        setLoading(false)
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error en transacción de fragmento: ${e.message}")
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