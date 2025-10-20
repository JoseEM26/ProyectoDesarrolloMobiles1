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

        // Validación de sesión antes de inflar la vista
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

        // Bottom Navigation
        binding.bottomNav.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_inicio -> changeFrame(DashBoardActivity())
                R.id.nav_asignatura -> changeFrame(AsignaturaActivity())
                R.id.nav_calificaoiones -> changeFrame(CalificacionActivity())
                R.id.nav_more -> changeFrame(MoreMenuNavActivity())
                else -> false
            }
            true
        }

        if(savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_inicio
        }
    }

    fun changeFrame(fragment: Fragment){
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .commit()
    }
}
