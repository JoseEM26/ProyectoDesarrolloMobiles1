package com.example.computronica

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.computronica.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarMain)

        binding.bottomNav.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_inicio -> {
                    changeFrame(DashBoardActivity())
                    true
                }
                R.id.nav_chat -> {
                    changeFrame(ChatActivity())
                    true
                }
                R.id.nav_usuarios -> {
                    changeFrame(UsuariosActivity())
                    true
                }
                else -> false
            }
        }


        if(savedInstanceState==null){
            binding.bottomNav.selectedItemId=R.id.nav_inicio
        }

    }

    fun changeFrame(fragment: Fragment){
          supportFragmentManager.beginTransaction()
              .replace(R.id.frameLayout,fragment)
              .commit()
    }
}
