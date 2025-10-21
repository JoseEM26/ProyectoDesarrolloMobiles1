package com.example.computronica

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.computronica.databinding.ActivityMoreMenuNavBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MoreMenuNavActivity : Fragment() {
    private var _b: ActivityMoreMenuNavBinding? = null
    private val b get() = _b!!
    private var ui = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = ActivityMoreMenuNavBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtons()
    }

    private fun setupButtons() {
        // Botón Chat
        b.btnChat.setOnClickListener {
            openUsersChatFragment()
        }

        // Botón Usuarios (si lo necesitas)
        b.btnUsuarios.setOnClickListener {
            // Aquí puedes abrir el UsuariosActivity de tu equipo si lo necesitas
            // O dejarlo sin acción si ya tienen esa funcionalidad en otro lado
        }

        // Botón Presentación
        b.btnPresentacion.setOnClickListener {
            openFragment(PresentacionActivity())
        }

        // Botón Logout
        b.btnLogOut.setOnClickListener {
            // Aquí implementarás el logout de Firebase
            // Por ahora lo dejamos preparado
        }
    }

    private fun openUsersChatFragment() {
        val fragment = UsersChatFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun openFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ui.cancel()
        _b = null
    }
}