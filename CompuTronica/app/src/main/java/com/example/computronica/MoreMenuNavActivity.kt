package com.example.computronica

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import com.example.computronica.Model.TipoUsuario
import com.example.computronica.Model.Usuario
import com.example.computronica.databinding.ActivityMoreMenuNavBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MoreMenuNavActivity : Fragment() {
    private var _binding: ActivityMoreMenuNavBinding? = null
    private val binding get() = _binding!!
    private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var lastClickTime = 0L
    private val debounceDelay = 500L // 500ms debounce

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityMoreMenuNavBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val usuario: Usuario? = SessionManager.currentUser
        if (usuario != null) {
            binding.txtNombreUsuario.text = "${usuario.nombre} ${usuario.apellido}"
            binding.txtNombreUsuario.setTextColor(ContextCompat.getColor(requireContext(), R.color.azul_oscuro))
            // Ocultar btnUsuarios para estudiantes
            binding.btnUsuarios.isInvisible = usuario.tipo == TipoUsuario.estudiante
        } else {
            binding.txtNombreUsuario.text = getString(R.string.msg_usuarioNoLogueado)
            binding.txtNombreUsuario.setTextColor(ContextCompat.getColor(requireContext(), R.color.rojo_error))
            binding.btnUsuarios.isInvisible = true // Ocultar para usuarios no logueados
        }

        setupButtons()
    }

    private fun setupButtons() {
        val mainActivity = requireActivity() as? MainActivity
            ?: run {
                Log.e("MoreMenuNavActivity", "Parent activity is not MainActivity")
                return
            }

        // Botón Chat
        binding.btnChat.setOnClickListener {
            if (debounceClick()) {
                mainActivity.changeFrame(UsersChatFragment())
                mainActivity.supportActionBar?.title = "Chat Institucional 💬"
            }
        }

        // Botón Presentación
        binding.btnPresentacion.setOnClickListener {
            if (debounceClick()) {
                mainActivity.changeFrame(PresentacionActivity())
                mainActivity.supportActionBar?.title = "Presentación"
            }
        }

        // Botón Usuarios
        binding.btnUsuarios.setOnClickListener {
            if (debounceClick()) {
                mainActivity.changeFrame(UsuariosActivity())
                mainActivity.supportActionBar?.title = "Gestión de Usuarios"
            }
        }

        // Botón Perfil
        binding.btnPerfil.setOnClickListener {
            if (debounceClick()) {
                mainActivity.changeFrame(PerfilActivity())
                mainActivity.supportActionBar?.title = "Mi Perfil"
            }
        }

        // Botón Logout
        binding.btnLogOut.setOnClickListener {
            if (debounceClick()) {
                FirebaseAuth.getInstance().signOut()
                SessionManager.clearSession()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }
        }
    }

    private fun debounceClick(): Boolean {
        val currentTime = System.currentTimeMillis()
        return if (currentTime - lastClickTime > debounceDelay) {
            lastClickTime = currentTime
            true
        } else {
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        uiScope.cancel()
        _binding = null
    }
}